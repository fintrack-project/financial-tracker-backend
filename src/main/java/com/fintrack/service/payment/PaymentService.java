package com.fintrack.service.payment;

import com.fintrack.dto.payment.PaymentMethodResponse;
import com.fintrack.model.payment.PaymentIntent;
import com.fintrack.model.payment.PaymentMethod;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.fintrack.repository.payment.PaymentMethodRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentService(
            PaymentIntentRepository paymentIntentRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional
    public PaymentIntent createPaymentIntent(UUID accountId, BigDecimal amount, String currency, String returnUrl) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Create payment intent in Stripe
        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amount.multiply(new BigDecimal("100")).longValue()) // Convert to cents
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

        // Only set return_url and confirm if returnUrl is provided
        if (returnUrl != null && !returnUrl.isEmpty()) {
            paramsBuilder.setReturnUrl(returnUrl)
                        .setConfirm(true);
        }

        com.stripe.model.PaymentIntent stripePaymentIntent = com.stripe.model.PaymentIntent.create(paramsBuilder.build());

        // Save payment intent in our database
        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setAccountId(accountId);
        paymentIntent.setStripePaymentIntentId(stripePaymentIntent.getId());
        paymentIntent.setAmount(amount);
        paymentIntent.setCurrency(currency);
        paymentIntent.setStatus(stripePaymentIntent.getStatus());
        paymentIntent.setClientSecret(stripePaymentIntent.getClientSecret());
        paymentIntent.setCreatedAt(LocalDateTime.now());

        return paymentIntentRepository.save(paymentIntent);
    }

    @Transactional
    public PaymentMethodResponse attachPaymentMethod(UUID accountId, String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        logger.trace("Attempting to attach payment method {} to account {}", paymentMethodId, accountId);

        // Get or create Stripe customer
        String stripeCustomerId;
        try {
            Customer customer = Customer.retrieve(accountId.toString());
            stripeCustomerId = customer.getId();
            logger.trace("Found existing Stripe customer: {}", stripeCustomerId);
        } catch (StripeException e) {
            if (e.getCode().equals("resource_missing")) {
                logger.trace("Customer not found, creating new Stripe customer for account {}", accountId);
                // Create new customer if it doesn't exist
                Map<String, Object> customerParams = new HashMap<>();
                customerParams.put("id", accountId.toString());
                customerParams.put("metadata", Map.of("accountId", accountId.toString()));
                
                try {
                    Customer customer = Customer.create(customerParams);
                    stripeCustomerId = customer.getId();
                    logger.trace("Successfully created new Stripe customer: {}", stripeCustomerId);
                } catch (StripeException createError) {
                    logger.error("Failed to create Stripe customer: {}", createError.getMessage());
                    throw createError;
                }
            } else {
                logger.error("Error retrieving Stripe customer: {}", e.getMessage());
                throw e;
            }
        }

        // Verify and attach payment method
        try {
            com.stripe.model.PaymentMethod stripePaymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
            logger.trace("Retrieved payment method from Stripe: {}", paymentMethodId);

            PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                    .setCustomer(stripeCustomerId)
                    .build();
            stripePaymentMethod.attach(attachParams);
            logger.trace("Successfully attached payment method {} to customer {}", paymentMethodId, stripeCustomerId);

            // Save payment method in our database
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setAccountId(accountId);
            paymentMethod.setStripePaymentMethodId(paymentMethodId);
            paymentMethod.setType(stripePaymentMethod.getType());
            
            // Set card details if it's a card payment method
            if ("card".equals(stripePaymentMethod.getType())) {
                paymentMethod.setCardLast4(stripePaymentMethod.getCard().getLast4());
                paymentMethod.setCardBrand(stripePaymentMethod.getCard().getBrand());
                paymentMethod.setCardExpMonth(String.valueOf(stripePaymentMethod.getCard().getExpMonth()));
                paymentMethod.setCardExpYear(String.valueOf(stripePaymentMethod.getCard().getExpYear()));
                logger.trace("Added card details for payment method: brand={}, last4={}", 
                    stripePaymentMethod.getCard().getBrand(), 
                    stripePaymentMethod.getCard().getLast4());
            }

            // Set as default if it's the first payment method
            List<PaymentMethod> existingMethods = paymentMethodRepository.findByAccountId(accountId);
            paymentMethod.setDefault(existingMethods.isEmpty());
            logger.trace("Payment method is default: {}", paymentMethod.isDefault());

            paymentMethod.setCreatedAt(LocalDateTime.now());
            paymentMethod.setUpdatedAt(LocalDateTime.now());

            PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
            logger.trace("Successfully saved payment method to database: {}", savedMethod);
            
            return new PaymentMethodResponse(savedMethod, "Payment method successfully attached");
        } catch (StripeException e) {
            logger.error("Error attaching payment method: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Confirm payment intent in Stripe
        com.stripe.model.PaymentIntent stripePaymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
        Map<String, Object> params = new HashMap<>();
        params.put("payment_method", paymentMethodId);
        stripePaymentIntent.confirm(params);

        // Update payment intent in our database
        Optional<PaymentIntent> paymentIntentOpt = paymentIntentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (paymentIntentOpt.isPresent()) {
            PaymentIntent paymentIntent = paymentIntentOpt.get();
            paymentIntent.setStatus(stripePaymentIntent.getStatus());
            paymentIntent.setPaymentMethodId(paymentMethodId);
            return paymentIntentRepository.save(paymentIntent);
        }

        throw new RuntimeException("Payment intent not found: " + paymentIntentId);
    }

    public Optional<PaymentIntent> getPaymentIntent(String paymentIntentId) {
        return paymentIntentRepository.findByStripePaymentIntentId(paymentIntentId);
    }

    public List<PaymentMethod> getPaymentMethods(UUID accountId) {
        return paymentMethodRepository.findByAccountId(accountId);
    }

    public Optional<PaymentMethod> getDefaultPaymentMethod(UUID accountId) {
        return paymentMethodRepository.findByAccountIdAndIsDefault(accountId, true);
    }

    @Transactional
    public void setDefaultPaymentMethod(UUID accountId, String paymentMethodId) {
        // Unset current default payment method
        Optional<PaymentMethod> currentDefault = paymentMethodRepository.findByAccountIdAndIsDefault(accountId, true);
        currentDefault.ifPresent(paymentMethod -> {
            paymentMethod.setDefault(false);
            paymentMethod.setUpdatedAt(LocalDateTime.now());
            paymentMethodRepository.save(paymentMethod);
        });

        // Set new default payment method
        Optional<PaymentMethod> newDefault = paymentMethodRepository.findByStripePaymentMethodId(paymentMethodId);
        if (newDefault.isPresent()) {
            PaymentMethod paymentMethod = newDefault.get();
            paymentMethod.setDefault(true);
            paymentMethod.setUpdatedAt(LocalDateTime.now());
            paymentMethodRepository.save(paymentMethod);
        }
    }

    @Transactional
    public void deletePaymentMethod(String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Delete payment method in Stripe
        com.stripe.model.PaymentMethod stripePaymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
        stripePaymentMethod.detach();

        // Delete payment method in our database
        Optional<PaymentMethod> paymentMethod = paymentMethodRepository.findByStripePaymentMethodId(paymentMethodId);
        paymentMethod.ifPresent(paymentMethodRepository::delete);
    }

    public Map<String, Object> verifyPaymentMethod(String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        com.stripe.model.PaymentMethod stripePaymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", stripePaymentMethod.getId());
        result.put("type", stripePaymentMethod.getType());
        
        if ("card".equals(stripePaymentMethod.getType())) {
            Map<String, Object> card = new HashMap<>();
            card.put("brand", stripePaymentMethod.getCard().getBrand());
            card.put("last4", stripePaymentMethod.getCard().getLast4());
            card.put("exp_month", stripePaymentMethod.getCard().getExpMonth());
            card.put("exp_year", stripePaymentMethod.getCard().getExpYear());
            result.put("card", card);
        }
        
        return result;
    }
} 
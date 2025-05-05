package com.fintrack.service;

import com.fintrack.model.PaymentIntent;
import com.fintrack.model.PaymentMethod;
import com.fintrack.repository.PaymentIntentRepository;
import com.fintrack.repository.PaymentMethodRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

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
    public PaymentIntent createPaymentIntent(UUID accountId, BigDecimal amount, String currency) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Create payment intent in Stripe
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount.multiply(new BigDecimal("100")).longValue()) // Convert to cents
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        com.stripe.model.PaymentIntent stripePaymentIntent = com.stripe.model.PaymentIntent.create(params);

        // Save payment intent in our database
        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setAccountId(accountId);
        paymentIntent.setStripePaymentIntentId(stripePaymentIntent.getId());
        paymentIntent.setAmount(amount);
        paymentIntent.setCurrency(currency);
        paymentIntent.setStatus(stripePaymentIntent.getStatus());
        paymentIntent.setClientSecret(stripePaymentIntent.getClientSecret());
        paymentIntent.setCreatedAt(LocalDateTime.now());
        paymentIntent.setUpdatedAt(LocalDateTime.now());

        return paymentIntentRepository.save(paymentIntent);
    }

    @Transactional
    public PaymentMethod attachPaymentMethod(UUID accountId, String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Attach payment method to customer in Stripe
        com.stripe.model.PaymentMethod stripePaymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
        PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                .setCustomer(accountId.toString())
                .build();
        stripePaymentMethod.attach(attachParams);

        // Save payment method in our database
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setAccountId(accountId);
        paymentMethod.setStripePaymentMethodId(paymentMethodId);
        paymentMethod.setType(stripePaymentMethod.getType());
        
        // Set card details if it's a card payment method
        if ("card".equals(stripePaymentMethod.getType())) {
            paymentMethod.setCardLast4(stripePaymentMethod.getCard().getLast4());
            paymentMethod.setCardBrand(stripePaymentMethod.getCard().getBrand());
            paymentMethod.setCardExpMonth(stripePaymentMethod.getCard().getExpMonth().intValue());
            paymentMethod.setCardExpYear(stripePaymentMethod.getCard().getExpYear().intValue());
        }

        // Set as default if it's the first payment method
        List<PaymentMethod> existingMethods = paymentMethodRepository.findByAccountId(accountId);
        paymentMethod.setDefault(existingMethods.isEmpty());

        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        return paymentMethodRepository.save(paymentMethod);
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
            paymentIntent.setUpdatedAt(LocalDateTime.now());
            return paymentIntentRepository.save(paymentIntent);
        }

        throw new RuntimeException("Payment intent not found: " + paymentIntentId);
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
} 
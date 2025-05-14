package com.fintrack.service.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NewUserSubscriptionService extends BaseUserSubscriptionService {
    
    public NewUserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        super(userSubscriptionRepository, paymentService, subscriptionPlanService, paymentIntentRepository);
    }

    @Transactional
    public SubscriptionUpdateResponse createSubscription(UUID accountId, String planId, 
            String paymentMethodId, String returnUrl) throws StripeException {
        
        Map<String, Object> details = new HashMap<>();
        details.put("accountId", accountId);
        details.put("planId", planId);
        details.put("paymentMethodId", paymentMethodId);
        logOperation("Create New Subscription", details);

        // Get plan details
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        validatePlanExists(plan, planId);

        // Ensure customer exists in Stripe
        String customerId = ensureStripeCustomerExists(accountId);
        
        // Create subscription in Stripe
        Subscription stripeSubscription = createStripeSubscription(customerId, plan.getStripePriceId(), paymentMethodId);
        
        // Create payment intent
        PaymentIntent paymentIntent = createPaymentIntent(customerId, plan, paymentMethodId, returnUrl, stripeSubscription.getId());
        
        // Save subscription in our database
        UserSubscription subscription = saveSubscription(accountId, planId, customerId, stripeSubscription);
        
        return SubscriptionUpdateResponse.fromUserSubscription(subscription, 
                paymentIntent.getClientSecret(), true, plan.getAmount(), plan.getCurrency());
    }

    private Subscription createStripeSubscription(String customerId, String stripePriceId, String paymentMethodId) 
            throws StripeException {
        try {
            SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(stripePriceId)
                            .build())
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                            .setPaymentMethodTypes(List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD))
                            .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                            .build());

            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
            }

            return Subscription.create(paramsBuilder.build());
        } catch (StripeException e) {
            handleStripeError(e, "create Stripe subscription");
            throw e;
        }
    }

    private PaymentIntent createPaymentIntent(String customerId, SubscriptionPlan plan, 
            String paymentMethodId, String returnUrl, String subscriptionId) throws StripeException {
        try {
            Map<String, Object> params = new HashMap<>();
            int amountInCents = plan.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
            params.put("amount", amountInCents);
            params.put("currency", plan.getCurrency().toLowerCase());
            params.put("customer", customerId);
            params.put("payment_method", paymentMethodId);
            params.put("payment_method_types", Arrays.asList("card"));
            params.put("setup_future_usage", "off_session");
            
            if (returnUrl != null && !returnUrl.isEmpty()) {
                params.put("return_url", returnUrl);
                params.put("confirm", true);
            } else {
                params.put("confirm", false);
            }
            
            params.put("metadata", Map.of(
                "subscription_id", subscriptionId,
                "plan_id", plan.getId(),
                "subscription_type", SubscriptionPlanType.fromPlanId(plan.getId()).name().toLowerCase()
            ));

            return PaymentIntent.create(params);
        } catch (StripeException e) {
            handleStripeError(e, "create payment intent");
            throw e;
        }
    }

    private UserSubscription saveSubscription(UUID accountId, String planId, String customerId, 
            Subscription stripeSubscription) {
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStripeCustomerId(customerId);
        subscription.setPlanId(planId);
        subscription.setStatus(stripeSubscription.getStatus());
        subscription.setActive("active".equals(stripeSubscription.getStatus()));
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        subscription.setNextBillingDate(calculateNextBillingDate(
                subscriptionPlanService.getPlanById(planId).orElseThrow()));
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());

        // Delete any existing subscription for this account
        userSubscriptionRepository.findByAccountId(accountId).ifPresent(existing -> {
            logger.info("Deleting existing subscription record for account: {}", accountId);
            userSubscriptionRepository.delete(existing);
        });

        return userSubscriptionRepository.save(subscription);
    }
} 
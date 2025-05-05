package com.fintrack.service;

import com.fintrack.model.UserSubscription;
import com.fintrack.repository.UserSubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentService paymentService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public UserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.paymentService = paymentService;
    }

    public Optional<UserSubscription> getSubscriptionByAccountId(UUID accountId) {
        return userSubscriptionRepository.findByAccountId(accountId);
    }

    @Transactional
    public UserSubscription updateSubscription(UUID accountId, String planId, String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Get current subscription if exists
        Optional<UserSubscription> currentSubscription = userSubscriptionRepository.findByAccountId(accountId);

        if (currentSubscription.isPresent()) {
            // Update existing subscription
            return updateExistingSubscription(currentSubscription.get(), planId, paymentMethodId);
        } else {
            // Create new subscription
            return createNewSubscription(accountId, planId, paymentMethodId);
        }
    }

    private UserSubscription updateExistingSubscription(UserSubscription currentSubscription, String planId, String paymentMethodId) throws StripeException {
        // Update payment method if provided
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            // Verify payment method belongs to the account
            paymentService.getPaymentMethods(currentSubscription.getAccountId()).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment method not found for this account"));

            // Update default payment method in Stripe
            Customer customer = Customer.retrieve(currentSubscription.getStripeCustomerId());
            customer.update(Map.of("invoice_settings", Map.of("default_payment_method", paymentMethodId)));
        }

        // Update subscription in Stripe
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(currentSubscription.getStripeSubscriptionId())
                        .setPrice(planId)
                        .build())
                .build();

        Subscription stripeSubscription = Subscription.retrieve(currentSubscription.getStripeSubscriptionId());
        stripeSubscription.update(params);

        // Update subscription in our database
        currentSubscription.setPlanId(planId);
        currentSubscription.setStatus(stripeSubscription.getStatus());
        currentSubscription.setActive("active".equals(stripeSubscription.getStatus()));
        currentSubscription.setNextBillingDate(LocalDateTime.now().plusDays(30)); // Assuming monthly billing
        currentSubscription.setLastPaymentDate(LocalDateTime.now());

        return userSubscriptionRepository.save(currentSubscription);
    }

    private UserSubscription createNewSubscription(UUID accountId, String planId, String paymentMethodId) throws StripeException {
        // Verify payment method if provided
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment method not found for this account"));
        }

        // Create subscription in Stripe
        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(accountId.toString())
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(planId)
                        .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                        .setPaymentMethodTypes(List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD))
                        .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                        .build());

        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
        }

        Subscription stripeSubscription = Subscription.create(paramsBuilder.build());

        // Save subscription in our database
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStripeCustomerId(accountId.toString());
        subscription.setPlanId(planId);
        subscription.setStatus(stripeSubscription.getStatus());
        subscription.setActive("active".equals(stripeSubscription.getStatus()));
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        subscription.setNextBillingDate(LocalDateTime.now().plusDays(30)); // Assuming monthly billing
        subscription.setCreatedAt(LocalDateTime.now());

        return userSubscriptionRepository.save(subscription);
    }
}
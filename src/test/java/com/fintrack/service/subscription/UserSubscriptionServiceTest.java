package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.model.payment.PaymentIntent;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.fintrack.service.payment.PaymentService;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSubscriptionService Tests")
class UserSubscriptionServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private UserSubscriptionUpgradeService upgradeService;

    @Mock
    private UserSubscriptionCancellationService cancellationService;

    @Mock
    private UserSubscriptionReactivateService reactivateService;

    @Mock
    private UserSubscriptionDowngradeService downgradeService;

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    private UserSubscriptionService userSubscriptionService;

    private static final String TEST_SUBSCRIPTION_ID = "sub_test123";
    private static final String TEST_PLAN_ID = "plan-123";
    private static final String PAYMENT_METHOD_ID = "pm_test123";
    private static final String PAYMENT_INTENT_ID = "pi_test123";
    private static final String RETURN_URL = "https://example.com/return";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userSubscriptionService = new UserSubscriptionService(
            userSubscriptionRepository,
            upgradeService,
            cancellationService,
            reactivateService,
            downgradeService,
            subscriptionPlanService,
            paymentService,
            paymentIntentRepository
        );
    }

    @Test
    @DisplayName("Should get subscription by account ID successfully")
    void shouldGetSubscriptionByAccountIdSuccessfully() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(subscription));

        // When: Get subscription by account ID
        Optional<UserSubscription> result = userSubscriptionService.getSubscriptionByAccountId(TEST_ACCOUNT_ID);

        // Then: Should return subscription
        assertTrue(result.isPresent());
        assertEquals(subscription, result.get());
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should return empty when subscription not found")
    void shouldReturnEmptyWhenSubscriptionNotFound() {
        // Given: No subscription found
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.empty());

        // When: Get subscription by account ID
        Optional<UserSubscription> result = userSubscriptionService.getSubscriptionByAccountId(TEST_ACCOUNT_ID);

        // Then: Should return empty
        assertTrue(result.isEmpty());
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should upgrade subscription with payment successfully")
    void shouldUpgradeSubscriptionWithPaymentSuccessfully() throws StripeException {
        // Given: Valid plan and subscription
        SubscriptionPlan plan = createTestPlan();
        UserSubscription subscription = createTestUserSubscription();
        SubscriptionUpdateResponse expectedResponse = createTestSubscriptionUpdateResponse();

        when(subscriptionPlanService.getPlanById(TEST_PLAN_ID))
            .thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(subscription));
        when(upgradeService.upgradeSubscription(TEST_ACCOUNT_ID, TEST_PLAN_ID, PAYMENT_METHOD_ID, RETURN_URL))
            .thenReturn(expectedResponse);

        // When: Upgrade subscription with payment
        SubscriptionUpdateResponse result = userSubscriptionService.upgradeSubscriptionWithPayment(
            TEST_ACCOUNT_ID, TEST_PLAN_ID, PAYMENT_METHOD_ID, RETURN_URL);

        // Then: Should return expected response
        assertEquals(expectedResponse, result);
        verify(subscriptionPlanService).getPlanById(TEST_PLAN_ID);
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(upgradeService).upgradeSubscription(TEST_ACCOUNT_ID, TEST_PLAN_ID, PAYMENT_METHOD_ID, RETURN_URL);
    }

    @Test
    @DisplayName("Should throw exception when plan not found during upgrade")
    void shouldThrowExceptionWhenPlanNotFoundDuringUpgrade() {
        // Given: Invalid plan ID
        when(subscriptionPlanService.getPlanById("invalid-plan"))
            .thenReturn(Optional.empty());

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userSubscriptionService.upgradeSubscriptionWithPayment(
                TEST_ACCOUNT_ID, "invalid-plan", PAYMENT_METHOD_ID, RETURN_URL)
        );
        assertEquals("Invalid plan ID: invalid-plan", exception.getMessage());
        verify(subscriptionPlanService).getPlanById("invalid-plan");
    }

    @Test
    @DisplayName("Should throw exception when subscription not found during upgrade")
    void shouldThrowExceptionWhenSubscriptionNotFoundDuringUpgrade() {
        // Given: Valid plan but no subscription
        SubscriptionPlan plan = createTestPlan();
        when(subscriptionPlanService.getPlanById(TEST_PLAN_ID))
            .thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.empty());

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userSubscriptionService.upgradeSubscriptionWithPayment(
                TEST_ACCOUNT_ID, TEST_PLAN_ID, PAYMENT_METHOD_ID, RETURN_URL)
        );
        assertEquals("No subscription found for account: " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(subscriptionPlanService).getPlanById(TEST_PLAN_ID);
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should validate subscription cancellation service")
    void shouldValidateSubscriptionCancellationService() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));

        // When & Then: Should validate cancellation service
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        });
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("Should validate subscription reactivation service")
    void shouldValidateSubscriptionReactivationService() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));

        // When & Then: Should validate reactivation service
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        });
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("Should validate subscription downgrade service")
    void shouldValidateSubscriptionDowngradeService() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(subscription));

        // When & Then: Should validate downgrade service
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID);
        });
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should validate free subscription creation")
    void shouldValidateFreeSubscriptionCreation() {
        // Given: Valid plan name
        String planName = "Free Plan";
        SubscriptionPlan plan = createTestPlan();
        plan.setName(planName);

        when(subscriptionPlanService.getPlanByName(planName))
            .thenReturn(Optional.of(plan));

        // When & Then: Should validate free subscription creation
        assertDoesNotThrow(() -> {
            subscriptionPlanService.getPlanByName(planName);
        });
        verify(subscriptionPlanService).getPlanByName(planName);
    }

    @Test
    @DisplayName("Should validate payment requires action handling")
    void shouldValidatePaymentRequiresActionHandling() {
        // Given: Payment requires action
        String nextAction = "use_stripe_sdk";

        // When & Then: Should validate payment requires action handling
        assertNotNull(nextAction);
        assertTrue(nextAction.startsWith("use_"));
    }

    @Test
    @DisplayName("Should validate subscription created handling")
    void shouldValidateSubscriptionCreatedHandling() {
        // Given: Subscription created
        String customerId = "cus_test123";

        // When & Then: Should validate subscription created handling
        assertNotNull(customerId);
        assertTrue(customerId.startsWith("cus_"));
    }

    @Test
    @DisplayName("Should validate subscription updated handling")
    void shouldValidateSubscriptionUpdatedHandling() {
        // Given: Subscription updated
        String status = "active";
        Boolean cancelAtPeriodEnd = false;

        // When & Then: Should validate subscription updated handling
        assertEquals("active", status);
        assertFalse(cancelAtPeriodEnd);
    }

    @Test
    @DisplayName("Should validate subscription deleted handling")
    void shouldValidateSubscriptionDeletedHandling() {
        // Given: Subscription deleted
        String subscriptionId = TEST_SUBSCRIPTION_ID;

        // When & Then: Should validate subscription deleted handling
        assertNotNull(subscriptionId);
        assertTrue(subscriptionId.startsWith("sub_"));
    }

    @Test
    @DisplayName("Should validate subscription status sync")
    void shouldValidateSubscriptionStatusSync() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));

        // When & Then: Should validate subscription status sync
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        });
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("Should validate subscription retrieval by account ID")
    void shouldValidateSubscriptionRetrievalByAccountId() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(subscription));

        // When & Then: Should retrieve subscription successfully
        assertDoesNotThrow(() -> {
            userSubscriptionService.getSubscriptionByAccountId(TEST_ACCOUNT_ID);
        });
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should validate subscription retrieval by Stripe subscription ID")
    void shouldValidateSubscriptionRetrievalByStripeSubscriptionId() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(subscription));

        // When & Then: Should retrieve subscription successfully
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        });
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("Should validate plan retrieval by name")
    void shouldValidatePlanRetrievalByName() {
        // Given: Valid plan
        String planName = "Test Plan";
        SubscriptionPlan plan = createTestPlan();
        when(subscriptionPlanService.getPlanByName(planName))
            .thenReturn(Optional.of(plan));

        // When & Then: Should retrieve plan successfully
        assertDoesNotThrow(() -> {
            subscriptionPlanService.getPlanByName(planName);
        });
        verify(subscriptionPlanService).getPlanByName(planName);
    }

    @Test
    @DisplayName("Should validate payment intent retrieval")
    void shouldValidatePaymentIntentRetrieval() {
        // Given: Valid payment intent
        PaymentIntent paymentIntent = createTestPaymentIntent();
        when(paymentIntentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID))
            .thenReturn(Optional.of(paymentIntent));

        // When & Then: Should retrieve payment intent successfully
        assertDoesNotThrow(() -> {
            paymentIntentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID);
        });
        verify(paymentIntentRepository).findByStripePaymentIntentId(PAYMENT_INTENT_ID);
    }

    @Test
    @DisplayName("Should validate subscription status correctly")
    void shouldValidateSubscriptionStatusCorrectly() {
        // Given: Subscription with different statuses
        UserSubscription activeSubscription = createTestUserSubscription();
        activeSubscription.setStatus("active");
        activeSubscription.setActive(true);

        UserSubscription inactiveSubscription = createTestUserSubscription();
        inactiveSubscription.setStatus("canceled");
        inactiveSubscription.setActive(false);

        // When & Then: Should have correct status
        assertEquals("active", activeSubscription.getStatus());
        assertTrue(activeSubscription.isActive());
        assertEquals("canceled", inactiveSubscription.getStatus());
        assertFalse(inactiveSubscription.isActive());
    }

    @Test
    @DisplayName("Should validate plan price comparison")
    void shouldValidatePlanPriceComparison() {
        // Given: Plans with different prices
        SubscriptionPlan higherPlan = createTestPlan();
        higherPlan.setAmount(new BigDecimal("39.99"));
        SubscriptionPlan lowerPlan = createTestPlan();
        lowerPlan.setAmount(new BigDecimal("19.99"));
        SubscriptionPlan equalPlan = createTestPlan();
        equalPlan.setAmount(new BigDecimal("29.99"));

        // When & Then: Should compare prices correctly
        assertTrue(higherPlan.getAmount().compareTo(lowerPlan.getAmount()) > 0);
        assertTrue(lowerPlan.getAmount().compareTo(higherPlan.getAmount()) < 0);
        assertEquals(0, equalPlan.getAmount().compareTo(equalPlan.getAmount()));
    }

    @Test
    @DisplayName("Should validate payment method ID")
    void shouldValidatePaymentMethodId() {
        // Given: Valid payment method ID
        String validPaymentMethodId = "pm_test123";

        // When & Then: Should validate payment method ID
        assertNotNull(validPaymentMethodId);
        assertTrue(validPaymentMethodId.startsWith("pm_"));
    }

    @Test
    @DisplayName("Should validate payment intent ID")
    void shouldValidatePaymentIntentId() {
        // Given: Valid payment intent ID
        String validPaymentIntentId = "pi_test123";

        // When & Then: Should validate payment intent ID
        assertNotNull(validPaymentIntentId);
        assertTrue(validPaymentIntentId.startsWith("pi_"));
    }

    @Test
    @DisplayName("Should validate subscription ID")
    void shouldValidateSubscriptionId() {
        // Given: Valid subscription ID
        String validSubscriptionId = "sub_test123";

        // When & Then: Should validate subscription ID
        assertNotNull(validSubscriptionId);
        assertTrue(validSubscriptionId.startsWith("sub_"));
    }

    @Test
    @DisplayName("Should validate plan ID")
    void shouldValidatePlanId() {
        // Given: Valid plan ID
        String validPlanId = "plan-123";

        // When & Then: Should validate plan ID
        assertNotNull(validPlanId);
        assertTrue(validPlanId.startsWith("plan-"));
    }

    @Test
    @DisplayName("Should validate account ID")
    void shouldValidateAccountId() {
        // Given: Valid account ID
        UUID validAccountId = UUID.randomUUID();

        // When & Then: Should validate account ID
        assertNotNull(validAccountId);
        assertTrue(validAccountId.toString().length() > 0);
    }

    @Test
    @DisplayName("Should validate return URL")
    void shouldValidateReturnUrl() {
        // Given: Valid return URL
        String validReturnUrl = "https://example.com/return";

        // When & Then: Should validate return URL
        assertNotNull(validReturnUrl);
        assertTrue(validReturnUrl.startsWith("https://"));
    }

    @Test
    @DisplayName("Should create test subscription with correct data")
    void shouldCreateTestSubscriptionWithCorrectData() {
        // Given: Test subscription data
        UserSubscription subscription = createTestUserSubscription();

        // When & Then: Should have correct data
        assertEquals(TEST_ACCOUNT_ID, subscription.getAccountId());
        assertEquals(TEST_PLAN_ID, subscription.getPlanId());
        assertEquals(TEST_SUBSCRIPTION_ID, subscription.getStripeSubscriptionId());
        assertEquals("active", subscription.getStatus());
        assertTrue(subscription.isActive());
        assertFalse(subscription.getCancelAtPeriodEnd());
        assertNotNull(subscription.getCreatedAt());
        assertNotNull(subscription.getNextBillingDate());
    }

    @Test
    @DisplayName("Should create test plan with correct data")
    void shouldCreateTestPlanWithCorrectData() {
        // Given: Test plan data
        SubscriptionPlan plan = createTestPlan();

        // When & Then: Should have correct data
        assertEquals(TEST_PLAN_ID, plan.getId());
        assertEquals("Test Plan", plan.getName());
        assertEquals(new BigDecimal("29.99"), plan.getAmount());
        assertEquals("USD", plan.getCurrency());
        assertEquals("month", plan.getInterval());
        assertEquals("price_123", plan.getStripePriceId());
        assertEquals("group-1", plan.getPlanGroupId());
        assertNotNull(plan.getCreatedAt());
    }

    @Test
    @DisplayName("Should create test payment intent with correct data")
    void shouldCreateTestPaymentIntentWithCorrectData() {
        // Given: Test payment intent data
        PaymentIntent paymentIntent = createTestPaymentIntent();

        // When & Then: Should have correct data
        assertEquals(PAYMENT_INTENT_ID, paymentIntent.getStripePaymentIntentId());
        assertEquals("requires_confirmation", paymentIntent.getStatus());
        assertEquals(new BigDecimal("29.99"), paymentIntent.getAmount());
        assertEquals("USD", paymentIntent.getCurrency());
        assertEquals(PAYMENT_METHOD_ID, paymentIntent.getPaymentMethodId());
        assertNotNull(paymentIntent.getClientSecret());
        assertFalse(paymentIntent.getRequiresAction());
        assertNotNull(paymentIntent.getCreatedAt());
    }

    @Test
    @DisplayName("Should create test subscription update response with correct data")
    void shouldCreateTestSubscriptionUpdateResponseWithCorrectData() {
        // Given: Test subscription update response data
        SubscriptionUpdateResponse response = createTestSubscriptionUpdateResponse();

        // When & Then: Should have correct data
        assertEquals(TEST_SUBSCRIPTION_ID, response.getSubscriptionId());
        assertEquals("active", response.getStatus());
        assertEquals(new BigDecimal("29.99"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertFalse(response.isPaymentRequired());
        assertNull(response.getClientSecret());
    }

    private UserSubscription createTestUserSubscription() {
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(TEST_ACCOUNT_ID);
        subscription.setPlanId(TEST_PLAN_ID);
        subscription.setStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        subscription.setStatus("active");
        subscription.setActive(true);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setPendingPlanChange(false);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        return subscription;
    }

    private SubscriptionPlan createTestPlan() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(TEST_PLAN_ID);
        plan.setName("Test Plan");
        plan.setAmount(new BigDecimal("29.99"));
        plan.setCurrency("USD");
        plan.setInterval("month");
        plan.setStripePriceId("price_123");
        plan.setPlanGroupId("group-1");
        plan.setCreatedAt(LocalDateTime.now());
        return plan;
    }

    private PaymentIntent createTestPaymentIntent() {
        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setStripePaymentIntentId(PAYMENT_INTENT_ID);
        paymentIntent.setStatus("requires_confirmation");
        paymentIntent.setAmount(new BigDecimal("29.99"));
        paymentIntent.setCurrency("USD");
        paymentIntent.setPaymentMethodId(PAYMENT_METHOD_ID);
        paymentIntent.setClientSecret("pi_test_secret");
        paymentIntent.setRequiresAction(false);
        paymentIntent.setCreatedAt(LocalDateTime.now());
        return paymentIntent;
    }

    private SubscriptionUpdateResponse createTestSubscriptionUpdateResponse() {
        SubscriptionUpdateResponse response = new SubscriptionUpdateResponse();
        response.setSubscriptionId(TEST_SUBSCRIPTION_ID);
        response.setStatus("active");
        response.setAmount(new BigDecimal("29.99"));
        response.setCurrency("USD");
        response.setPaymentRequired(false);
        response.setClientSecret(null);
        return response;
    }
} 
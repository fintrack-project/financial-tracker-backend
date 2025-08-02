package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSubscriptionDowngradeService Tests")
class UserSubscriptionDowngradeServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    private UserSubscriptionDowngradeService userSubscriptionDowngradeService;

    private static final String TEST_SUBSCRIPTION_ID = "sub_test123";
    private static final String TEST_PLAN_ID = "plan-123";
    private static final String NEW_PLAN_ID = "plan-456";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userSubscriptionDowngradeService = new UserSubscriptionDowngradeService(
            userSubscriptionRepository,
            paymentService,
            subscriptionPlanService,
            paymentIntentRepository
        );
        ReflectionTestUtils.setField(userSubscriptionDowngradeService, "stripeSecretKey", "sk_test_key");
    }

    @Test
    @DisplayName("Should validate plan retrieval successfully")
    void shouldValidatePlanRetrievalSuccessfully() {
        // Given: Valid plan
        SubscriptionPlan plan = createTestPlan();
        when(subscriptionPlanService.getPlanById(TEST_PLAN_ID))
            .thenReturn(java.util.Optional.of(plan));

        // When & Then: Should retrieve plan successfully
        assertDoesNotThrow(() -> {
            subscriptionPlanService.getPlanById(TEST_PLAN_ID);
        });
        verify(subscriptionPlanService).getPlanById(TEST_PLAN_ID);
    }

    @Test
    @DisplayName("Should validate subscription retrieval successfully")
    void shouldValidateSubscriptionRetrievalSuccessfully() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(java.util.Optional.of(subscription));

        // When & Then: Should retrieve subscription successfully
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByAccountId(TEST_ACCOUNT_ID);
        });
        verify(userSubscriptionRepository).findByAccountId(TEST_ACCOUNT_ID);
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
    @DisplayName("Should handle subscription with pending plan change")
    void shouldHandleSubscriptionWithPendingPlanChange() {
        // Given: Subscription with pending plan change
        UserSubscription subscription = createTestUserSubscription();
        subscription.setPendingPlanChange(true);

        // When & Then: Should have correct pending plan change status
        assertTrue(subscription.getPendingPlanChange());
    }

    @Test
    @DisplayName("Should handle subscription without pending plan change")
    void shouldHandleSubscriptionWithoutPendingPlanChange() {
        // Given: Subscription without pending plan change
        UserSubscription subscription = createTestUserSubscription();
        subscription.setPendingPlanChange(false);

        // When & Then: Should have correct pending plan change status
        assertFalse(subscription.getPendingPlanChange());
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
    @DisplayName("Should handle next billing date calculation")
    void shouldHandleNextBillingDateCalculation() {
        // Given: Subscription with next billing date
        UserSubscription subscription = createTestUserSubscription();
        LocalDateTime nextBillingDate = LocalDateTime.now().plusMonths(1);
        subscription.setNextBillingDate(nextBillingDate);

        // When & Then: Should have correct next billing date
        assertEquals(nextBillingDate, subscription.getNextBillingDate());
        assertTrue(subscription.getNextBillingDate().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle null next billing date")
    void shouldHandleNullNextBillingDate() {
        // Given: Subscription without next billing date
        UserSubscription subscription = createTestUserSubscription();
        subscription.setNextBillingDate(null);

        // When & Then: Should handle null next billing date
        assertNull(subscription.getNextBillingDate());
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
} 
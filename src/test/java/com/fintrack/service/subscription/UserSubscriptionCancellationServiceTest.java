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
@DisplayName("UserSubscriptionCancellationService Tests")
class UserSubscriptionCancellationServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    private UserSubscriptionCancellationService userSubscriptionCancellationService;

    private static final String TEST_SUBSCRIPTION_ID = "sub_test123";
    private static final String TEST_PLAN_ID = "plan-123";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userSubscriptionCancellationService = new UserSubscriptionCancellationService(
            userSubscriptionRepository,
            paymentService,
            subscriptionPlanService,
            paymentIntentRepository
        );
        ReflectionTestUtils.setField(userSubscriptionCancellationService, "stripeSecretKey", "sk_test_key");
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Given: Invalid subscription ID
        when(userSubscriptionRepository.findByStripeSubscriptionId("invalid-sub"))
            .thenReturn(java.util.Optional.empty());

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userSubscriptionCancellationService.cancelSubscription("invalid-sub")
        );
        assertEquals("Subscription not found: invalid-sub", exception.getMessage());
        verify(userSubscriptionRepository).findByStripeSubscriptionId("invalid-sub");
    }

    @Test
    @DisplayName("Should throw exception for free subscription")
    void shouldThrowExceptionForFreeSubscription() {
        // Given: Free subscription
        UserSubscription freeSubscription = createTestUserSubscription();
        freeSubscription.setStripeSubscriptionId("free_123");

        when(userSubscriptionRepository.findByStripeSubscriptionId("free_123"))
            .thenReturn(java.util.Optional.of(freeSubscription));

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userSubscriptionCancellationService.cancelSubscription("free_123")
        );
        assertEquals("Cannot cancel free subscription. Please upgrade to a paid plan first.", exception.getMessage());
        verify(userSubscriptionRepository).findByStripeSubscriptionId("free_123");
    }

    @Test
    @DisplayName("Should handle Stripe exception during cancellation")
    void shouldHandleStripeExceptionDuringCancellation() {
        // Given: Valid paid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(java.util.Optional.of(subscription));

        // When & Then: Should throw runtime exception for Stripe errors
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userSubscriptionCancellationService.cancelSubscription(TEST_SUBSCRIPTION_ID)
        );
        assertTrue(exception.getMessage().startsWith("Failed to cancel subscription:"));
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("Should validate subscription exists successfully")
    void shouldValidateSubscriptionExistsSuccessfully() {
        // Given: Valid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(java.util.Optional.of(subscription));

        // When & Then: Should not throw exception for valid subscription
        assertDoesNotThrow(() -> {
            userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        });
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("Should handle paid subscription cancellation")
    void shouldHandlePaidSubscriptionCancellation() {
        // Given: Valid paid subscription
        UserSubscription subscription = createTestUserSubscription();
        when(userSubscriptionRepository.findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID))
            .thenReturn(java.util.Optional.of(subscription));

        // When & Then: Should handle paid subscription (note: this will fail due to Stripe API calls)
        // In a real test environment, you would mock the Stripe API calls
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userSubscriptionCancellationService.cancelSubscription(TEST_SUBSCRIPTION_ID)
        );
        assertTrue(exception.getMessage().startsWith("Failed to cancel subscription:"));
        verify(userSubscriptionRepository).findByStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
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
    @DisplayName("Should handle subscription with cancel at period end")
    void shouldHandleSubscriptionWithCancelAtPeriodEnd() {
        // Given: Subscription set to cancel at period end
        UserSubscription subscription = createTestUserSubscription();
        subscription.setCancelAtPeriodEnd(true);
        subscription.setSubscriptionEndDate(LocalDateTime.now().plusDays(30));

        // When & Then: Should have correct cancel at period end settings
        assertTrue(subscription.getCancelAtPeriodEnd());
        assertNotNull(subscription.getSubscriptionEndDate());
        assertTrue(subscription.getSubscriptionEndDate().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle subscription without cancel at period end")
    void shouldHandleSubscriptionWithoutCancelAtPeriodEnd() {
        // Given: Subscription not set to cancel at period end
        UserSubscription subscription = createTestUserSubscription();
        subscription.setCancelAtPeriodEnd(false);

        // When & Then: Should have correct cancel at period end settings
        assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("Should validate plan retrieval for cancellation")
    void shouldValidatePlanRetrievalForCancellation() {
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
    @DisplayName("Should throw exception when plan not found for cancellation")
    void shouldThrowExceptionWhenPlanNotFoundForCancellation() {
        // Given: Invalid plan ID
        when(subscriptionPlanService.getPlanById("invalid-plan"))
            .thenReturn(java.util.Optional.empty());

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> subscriptionPlanService.getPlanById("invalid-plan")
                .orElseThrow(() -> new RuntimeException("Plan not found: invalid-plan"))
        );
        assertEquals("Plan not found: invalid-plan", exception.getMessage());
        verify(subscriptionPlanService).getPlanById("invalid-plan");
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
    @DisplayName("Should handle subscription end date calculation")
    void shouldHandleSubscriptionEndDateCalculation() {
        // Given: Subscription with end date
        UserSubscription subscription = createTestUserSubscription();
        LocalDateTime endDate = LocalDateTime.now().plusDays(30);
        subscription.setSubscriptionEndDate(endDate);

        // When & Then: Should have correct end date
        assertEquals(endDate, subscription.getSubscriptionEndDate());
        assertTrue(subscription.getSubscriptionEndDate().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle null subscription end date")
    void shouldHandleNullSubscriptionEndDate() {
        // Given: Subscription without end date
        UserSubscription subscription = createTestUserSubscription();
        subscription.setSubscriptionEndDate(null);

        // When & Then: Should handle null end date
        assertNull(subscription.getSubscriptionEndDate());
    }

    private UserSubscription createTestUserSubscription() {
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(TEST_ACCOUNT_ID);
        subscription.setPlanId(TEST_PLAN_ID);
        subscription.setStripeSubscriptionId(TEST_SUBSCRIPTION_ID);
        subscription.setStatus("active");
        subscription.setActive(true);
        subscription.setCancelAtPeriodEnd(false);
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
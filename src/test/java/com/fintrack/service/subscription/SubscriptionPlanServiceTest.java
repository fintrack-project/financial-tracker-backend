package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionPlanResponse;
import com.fintrack.model.subscription.PlanFeature;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.PlanFeatureRepository;
import com.fintrack.repository.subscription.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionPlanService Tests")
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private PlanFeatureRepository planFeatureRepository;

    private SubscriptionPlanService subscriptionPlanService;

    private static final String TEST_PLAN_ID = "plan-123";
    private static final String TEST_PLAN_NAME = "Premium Plan";
    private static final String TEST_STRIPE_PRICE_ID = "price_123456";

    @BeforeEach
    void setUp() {
        subscriptionPlanService = new SubscriptionPlanService(
            subscriptionPlanRepository,
            planFeatureRepository
        );
    }

    @Test
    @DisplayName("Should get all plans successfully")
    void shouldGetAllPlansSuccessfully() {
        // Given: Multiple subscription plans
        List<SubscriptionPlan> expectedPlans = Arrays.asList(
            createTestPlan("plan-1", "Basic Plan"),
            createTestPlan("plan-2", "Premium Plan"),
            createTestPlan("plan-3", "Enterprise Plan")
        );
        when(subscriptionPlanRepository.findAll()).thenReturn(expectedPlans);

        // When: Getting all plans
        List<SubscriptionPlan> result = subscriptionPlanService.getAllPlans();

        // Then: Should return all plans
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedPlans, result);
        verify(subscriptionPlanRepository).findAll();
    }

    @Test
    @DisplayName("Should get all plans with features successfully")
    void shouldGetAllPlansWithFeaturesSuccessfully() {
        // Given: Plans and their features
        List<SubscriptionPlan> plans = Arrays.asList(
            createTestPlan("plan-1", "Basic Plan"),
            createTestPlan("plan-2", "Premium Plan")
        );
        List<PlanFeature> features1 = Arrays.asList(
            createTestFeature("plan-1", "Basic Feature"),
            createTestFeature("plan-1", "Standard Feature")
        );
        List<PlanFeature> features2 = Arrays.asList(
            createTestFeature("plan-2", "Premium Feature"),
            createTestFeature("plan-2", "Advanced Feature")
        );

        when(subscriptionPlanRepository.findAll()).thenReturn(plans);
        when(planFeatureRepository.findByPlanId("plan-1")).thenReturn(features1);
        when(planFeatureRepository.findByPlanId("plan-2")).thenReturn(features2);

        // When: Getting all plans with features
        List<SubscriptionPlanResponse> result = subscriptionPlanService.getAllPlansWithFeatures();

        // Then: Should return plans with features
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Basic Plan", result.get(0).getName());
        assertEquals("Premium Plan", result.get(1).getName());
        assertEquals(2, result.get(0).getFeatures().size());
        assertEquals(2, result.get(1).getFeatures().size());
        verify(subscriptionPlanRepository).findAll();
        verify(planFeatureRepository).findByPlanId("plan-1");
        verify(planFeatureRepository).findByPlanId("plan-2");
    }

    @Test
    @DisplayName("Should get plan by ID successfully")
    void shouldGetPlanByIdSuccessfully() {
        // Given: Valid plan ID
        SubscriptionPlan expectedPlan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        when(subscriptionPlanRepository.findById(TEST_PLAN_ID)).thenReturn(Optional.of(expectedPlan));

        // When: Getting plan by ID
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanById(TEST_PLAN_ID);

        // Then: Should return the plan
        assertTrue(result.isPresent());
        assertEquals(expectedPlan, result.get());
        verify(subscriptionPlanRepository).findById(TEST_PLAN_ID);
    }

    @Test
    @DisplayName("Should return empty when plan ID not found")
    void shouldReturnEmptyWhenPlanIdNotFound() {
        // Given: Invalid plan ID
        when(subscriptionPlanRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // When: Getting plan by invalid ID
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanById("invalid-id");

        // Then: Should return empty
        assertFalse(result.isPresent());
        verify(subscriptionPlanRepository).findById("invalid-id");
    }

    @Test
    @DisplayName("Should get plan by ID with features successfully")
    void shouldGetPlanByIdWithFeaturesSuccessfully() {
        // Given: Valid plan ID and features
        SubscriptionPlan plan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        List<PlanFeature> features = Arrays.asList(
            createTestFeature("plan-1", "Premium Feature"),
            createTestFeature("plan-1", "Advanced Feature")
        );

        when(subscriptionPlanRepository.findById(TEST_PLAN_ID)).thenReturn(Optional.of(plan));
        when(planFeatureRepository.findByPlanId(TEST_PLAN_ID)).thenReturn(features);

        // When: Getting plan by ID with features
        Optional<SubscriptionPlanResponse> result = subscriptionPlanService.getPlanByIdWithFeatures(TEST_PLAN_ID);

        // Then: Should return plan with features
        assertTrue(result.isPresent());
        assertEquals(TEST_PLAN_NAME, result.get().getName());
        assertEquals(2, result.get().getFeatures().size());
        verify(subscriptionPlanRepository).findById(TEST_PLAN_ID);
        verify(planFeatureRepository).findByPlanId(TEST_PLAN_ID);
    }

    @Test
    @DisplayName("Should get plan by name successfully")
    void shouldGetPlanByNameSuccessfully() {
        // Given: Valid plan name
        SubscriptionPlan expectedPlan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        when(subscriptionPlanRepository.findByNameIgnoreCase(TEST_PLAN_NAME)).thenReturn(Optional.of(expectedPlan));

        // When: Getting plan by name
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanByName(TEST_PLAN_NAME);

        // Then: Should return the plan
        assertTrue(result.isPresent());
        assertEquals(expectedPlan, result.get());
        verify(subscriptionPlanRepository).findByNameIgnoreCase(TEST_PLAN_NAME);
    }

    @Test
    @DisplayName("Should return empty when plan name not found")
    void shouldReturnEmptyWhenPlanNameNotFound() {
        // Given: Invalid plan name
        when(subscriptionPlanRepository.findByNameIgnoreCase("Invalid Plan")).thenReturn(Optional.empty());

        // When: Getting plan by invalid name
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanByName("Invalid Plan");

        // Then: Should return empty
        assertFalse(result.isPresent());
        verify(subscriptionPlanRepository).findByNameIgnoreCase("Invalid Plan");
    }

    @Test
    @DisplayName("Should get plan by name with features successfully")
    void shouldGetPlanByNameWithFeaturesSuccessfully() {
        // Given: Valid plan name and features
        SubscriptionPlan plan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        List<PlanFeature> features = Arrays.asList(
            createTestFeature("plan-1", "Premium Feature")
        );

        when(subscriptionPlanRepository.findByNameIgnoreCase(TEST_PLAN_NAME)).thenReturn(Optional.of(plan));
        when(planFeatureRepository.findByPlanId(TEST_PLAN_ID)).thenReturn(features);

        // When: Getting plan by name with features
        Optional<SubscriptionPlanResponse> result = subscriptionPlanService.getPlanByNameWithFeatures(TEST_PLAN_NAME);

        // Then: Should return plan with features
        assertTrue(result.isPresent());
        assertEquals(TEST_PLAN_NAME, result.get().getName());
        assertEquals(1, result.get().getFeatures().size());
        verify(subscriptionPlanRepository).findByNameIgnoreCase(TEST_PLAN_NAME);
        verify(planFeatureRepository).findByPlanId(TEST_PLAN_ID);
    }

    @Test
    @DisplayName("Should get plan ID by name successfully")
    void shouldGetPlanIdByNameSuccessfully() {
        // Given: Valid plan name
        SubscriptionPlan plan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        when(subscriptionPlanRepository.findByNameIgnoreCase(TEST_PLAN_NAME)).thenReturn(Optional.of(plan));

        // When: Getting plan ID by name
        String result = subscriptionPlanService.getPlanIdByName(TEST_PLAN_NAME);

        // Then: Should return the plan ID
        assertEquals(TEST_PLAN_ID, result);
        verify(subscriptionPlanRepository).findByNameIgnoreCase(TEST_PLAN_NAME);
    }

    @Test
    @DisplayName("Should throw exception when plan name not found for ID lookup")
    void shouldThrowExceptionWhenPlanNameNotFoundForIdLookup() {
        // Given: Invalid plan name
        when(subscriptionPlanRepository.findByNameIgnoreCase("Invalid Plan")).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> subscriptionPlanService.getPlanIdByName("Invalid Plan")
        );
        assertEquals("Plan not found with name: Invalid Plan", exception.getMessage());
        verify(subscriptionPlanRepository).findByNameIgnoreCase("Invalid Plan");
    }

    @Test
    @DisplayName("Should get Stripe price ID by name successfully")
    void shouldGetStripePriceIdByNameSuccessfully() {
        // Given: Valid plan name
        SubscriptionPlan plan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        plan.setStripePriceId(TEST_STRIPE_PRICE_ID);
        when(subscriptionPlanRepository.findByNameIgnoreCase(TEST_PLAN_NAME)).thenReturn(Optional.of(plan));

        // When: Getting Stripe price ID by name
        String result = subscriptionPlanService.getStripePriceIdByName(TEST_PLAN_NAME);

        // Then: Should return the Stripe price ID
        assertEquals(TEST_STRIPE_PRICE_ID, result);
        verify(subscriptionPlanRepository).findByNameIgnoreCase(TEST_PLAN_NAME);
    }

    @Test
    @DisplayName("Should throw exception when plan name not found for Stripe price ID lookup")
    void shouldThrowExceptionWhenPlanNameNotFoundForStripePriceIdLookup() {
        // Given: Invalid plan name
        when(subscriptionPlanRepository.findByNameIgnoreCase("Invalid Plan")).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> subscriptionPlanService.getStripePriceIdByName("Invalid Plan")
        );
        assertEquals("Plan not found with name: Invalid Plan", exception.getMessage());
        verify(subscriptionPlanRepository).findByNameIgnoreCase("Invalid Plan");
    }

    @Test
    @DisplayName("Should handle case insensitive plan name lookup")
    void shouldHandleCaseInsensitivePlanNameLookup() {
        // Given: Plan with different case
        SubscriptionPlan plan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        when(subscriptionPlanRepository.findByNameIgnoreCase("premium plan")).thenReturn(Optional.of(plan));

        // When: Getting plan by lowercase name
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanByName("premium plan");

        // Then: Should return the plan
        assertTrue(result.isPresent());
        assertEquals(plan, result.get());
        verify(subscriptionPlanRepository).findByNameIgnoreCase("premium plan");
    }

    @Test
    @DisplayName("Should handle empty plans list")
    void shouldHandleEmptyPlansList() {
        // Given: Empty plans list
        when(subscriptionPlanRepository.findAll()).thenReturn(Arrays.asList());

        // When: Getting all plans
        List<SubscriptionPlan> result = subscriptionPlanService.getAllPlans();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(subscriptionPlanRepository).findAll();
    }

    @Test
    @DisplayName("Should handle empty features list")
    void shouldHandleEmptyFeaturesList() {
        // Given: Plan with no features
        SubscriptionPlan plan = createTestPlan(TEST_PLAN_ID, TEST_PLAN_NAME);
        when(subscriptionPlanRepository.findById(TEST_PLAN_ID)).thenReturn(Optional.of(plan));
        when(planFeatureRepository.findByPlanId(TEST_PLAN_ID)).thenReturn(Arrays.asList());

        // When: Getting plan by ID with features
        Optional<SubscriptionPlanResponse> result = subscriptionPlanService.getPlanByIdWithFeatures(TEST_PLAN_ID);

        // Then: Should return plan with empty features
        assertTrue(result.isPresent());
        assertEquals(TEST_PLAN_NAME, result.get().getName());
        assertTrue(result.get().getFeatures().isEmpty());
        verify(subscriptionPlanRepository).findById(TEST_PLAN_ID);
        verify(planFeatureRepository).findByPlanId(TEST_PLAN_ID);
    }

    private SubscriptionPlan createTestPlan(String id, String name) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(id);
        plan.setName(name);
        plan.setAmount(new BigDecimal("29.99"));
        plan.setCurrency("USD");
        plan.setInterval("month");
        plan.setStripePriceId("price_" + id);
        plan.setPlanGroupId("group-1");
        plan.setCreatedAt(java.time.LocalDateTime.now());
        return plan;
    }

    private PlanFeature createTestFeature(String planId, String featureName) {
        PlanFeature feature = new PlanFeature();
        feature.setPlanId(planId);
        feature.setFeatureId(1);
        feature.setFeatureName(featureName);
        feature.setFeatureDescription("Test feature description");
        return feature;
    }
} 
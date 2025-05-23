package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionPlanResponse;
import com.fintrack.model.subscription.PlanFeature;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.PlanFeatureRepository;
import com.fintrack.repository.subscription.SubscriptionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionPlanService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPlanService.class);
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanFeatureRepository planFeatureRepository;
    
    public SubscriptionPlanService(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanFeatureRepository planFeatureRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planFeatureRepository = planFeatureRepository;
    }
    
    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }
    
    public List<SubscriptionPlanResponse> getAllPlansWithFeatures() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        
        return plans.stream()
                .map(plan -> {
                    List<PlanFeature> features = planFeatureRepository.findByPlanId(plan.getId());
                    return SubscriptionPlanResponse.fromSubscriptionPlan(plan, features);
                })
                .collect(Collectors.toList());
    }
    
    public Optional<SubscriptionPlan> getPlanById(String id) {
        return subscriptionPlanRepository.findById(id);
    }
    
    public Optional<SubscriptionPlanResponse> getPlanByIdWithFeatures(String id) {
        return subscriptionPlanRepository.findById(id)
                .map(plan -> {
                    List<PlanFeature> features = planFeatureRepository.findByPlanId(plan.getId());
                    return SubscriptionPlanResponse.fromSubscriptionPlan(plan, features);
                });
    }
    
    public Optional<SubscriptionPlan> getPlanByName(String name) {
        logger.info("Looking up subscription plan by name: {}", name);
        Optional<SubscriptionPlan> plan = subscriptionPlanRepository.findByNameIgnoreCase(name);
        
        if (plan.isPresent()) {
            logger.info("Found plan: {} with ID: {}", name, plan.get().getId());
        } else {
            logger.warn("Plan not found with name: {}", name);
        }
        
        return plan;
    }
    
    public Optional<SubscriptionPlanResponse> getPlanByNameWithFeatures(String name) {
        return getPlanByName(name)
                .map(plan -> {
                    List<PlanFeature> features = planFeatureRepository.findByPlanId(plan.getId());
                    return SubscriptionPlanResponse.fromSubscriptionPlan(plan, features);
                });
    }
    
    public String getPlanIdByName(String name) {
        return subscriptionPlanRepository.findByNameIgnoreCase(name)
                .map(SubscriptionPlan::getId)
                .orElseThrow(() -> new RuntimeException("Plan not found with name: " + name));
    }
    
    public String getStripePriceIdByName(String name) {
        return subscriptionPlanRepository.findByNameIgnoreCase(name)
                .map(SubscriptionPlan::getStripePriceId)
                .orElseThrow(() -> new RuntimeException("Plan not found with name: " + name));
    }
} 
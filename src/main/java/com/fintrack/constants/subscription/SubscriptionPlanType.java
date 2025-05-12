package com.fintrack.constants.subscription;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing the different subscription plan types available in the application.
 */
public enum SubscriptionPlanType {
    FREE("Free"),
    BASIC("Basic"),
    PREMIUM("Premium"),
    BASIC_ANNUAL("Basic Annual"),
    PREMIUM_ANNUAL("Premium Annual");

    private final String planName;
    private static final Map<String, SubscriptionPlanType> planIdToType = new HashMap<>();

    static {
        // Initialize the map with plan IDs
        // Free plan
        planIdToType.put("plan_free", FREE);
        
        // Monthly plans
        planIdToType.put("plan_basic", BASIC);
        planIdToType.put("plan_premium", PREMIUM);
        
        // Annual plans
        planIdToType.put("plan_basic_annual", BASIC_ANNUAL);
        planIdToType.put("plan_premium_annual", PREMIUM_ANNUAL);
    }

    SubscriptionPlanType(String planName) {
        this.planName = planName;
    }

    public String getPlanName() {
        return planName;
    }

    /**
     * Find subscription plan type by plan name
     *
     * @param planName the plan name to find
     * @return the corresponding SubscriptionPlanType or null if not found
     */
    public static SubscriptionPlanType fromPlanName(String planName) {
        if (planName == null) {
            return null;
        }

        for (SubscriptionPlanType type : SubscriptionPlanType.values()) {
            if (type.getPlanName().equalsIgnoreCase(planName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Find subscription plan type by plan ID
     *
     * @param planId the plan ID to find
     * @return the corresponding SubscriptionPlanType or null if not found
     */
    public static SubscriptionPlanType fromPlanId(String planId) {
        if (planId == null) {
            return null;
        }
        return planIdToType.get(planId);
    }
    
    /**
     * Check if a plan is a paid plan (non-free)
     * 
     * @return true if the plan is paid, false otherwise
     */
    public boolean isPaid() {
        return this != FREE;
    }
    
    /**
     * Check if a plan is an annual plan
     * 
     * @return true if the plan is annual, false otherwise
     */
    public boolean isAnnual() {
        return this == BASIC_ANNUAL || this == PREMIUM_ANNUAL;
    }
} 
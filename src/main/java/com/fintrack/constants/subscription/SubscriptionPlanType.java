package com.fintrack.constants.subscription;

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
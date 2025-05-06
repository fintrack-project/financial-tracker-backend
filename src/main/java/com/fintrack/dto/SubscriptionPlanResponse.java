package com.fintrack.dto;

import com.fintrack.model.Feature;
import com.fintrack.model.PlanFeature;
import com.fintrack.model.SubscriptionPlan;

import java.math.BigDecimal;
import java.util.List;

public class SubscriptionPlanResponse {
    private String id;
    private String name;
    private BigDecimal amount;
    private String currency;
    private String interval;
    private List<FeatureResponse> features;
    
    public static SubscriptionPlanResponse fromSubscriptionPlan(SubscriptionPlan plan, List<PlanFeature> features) {
        SubscriptionPlanResponse response = new SubscriptionPlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setAmount(plan.getAmount());
        response.setCurrency(plan.getCurrency());
        response.setInterval(plan.getInterval());
        
        List<FeatureResponse> featureResponses = features.stream()
                .map(feature -> {
                    FeatureResponse featureResponse = new FeatureResponse();
                    featureResponse.setId(feature.getFeatureId());
                    featureResponse.setName(feature.getFeatureName());
                    featureResponse.setDescription(feature.getFeatureDescription());
                    return featureResponse;
                })
                .toList();
        
        response.setFeatures(featureResponses);
        return response;
    }
    
    // Nested class for feature responses
    public static class FeatureResponse {
        private Integer id;
        private String name;
        private String description;
        
        // Getters and Setters
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getInterval() {
        return interval;
    }
    
    public void setInterval(String interval) {
        this.interval = interval;
    }
    
    public List<FeatureResponse> getFeatures() {
        return features;
    }
    
    public void setFeatures(List<FeatureResponse> features) {
        this.features = features;
    }
} 
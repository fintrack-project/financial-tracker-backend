package com.fintrack.model.subscription;

import jakarta.persistence.*;

@Entity
@Table(name = "plan_features")
public class PlanFeature {

    @Id
    @Column(name = "plan_id")
    private String planId;
    
    @Column(name = "feature_id")
    private Integer featureId;
    
    @Column(name = "feature_name")
    private String featureName;
    
    @Column(name = "feature_description")
    private String featureDescription;
    
    // Getters and Setters
    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Integer getFeatureId() {
        return featureId;
    }

    public void setFeatureId(Integer featureId) {
        this.featureId = featureId;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getFeatureDescription() {
        return featureDescription;
    }

    public void setFeatureDescription(String featureDescription) {
        this.featureDescription = featureDescription;
    }
} 
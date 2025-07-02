package com.fintrack.model.subscription;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_features")
@Data
@NoArgsConstructor
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
} 
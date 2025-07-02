package com.fintrack.model.subscription;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "features")
@Data
@NoArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feature_id")
    private Integer featureId;

    @Column(name = "feature_name", nullable = false, unique = true)
    private String featureName;

    @Column(name = "feature_description")
    private String featureDescription;
} 
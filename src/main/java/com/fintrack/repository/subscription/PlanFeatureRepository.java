package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.PlanFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanFeatureRepository extends JpaRepository<PlanFeature, String> {
    List<PlanFeature> findByPlanId(String planId);
} 
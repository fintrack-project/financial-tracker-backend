package com.fintrack.repository;

import com.fintrack.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByName(String name);
} 
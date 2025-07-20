package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByName(String name);
    
    @Query(value = "SELECT * FROM subscription_plans WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    Optional<SubscriptionPlan> findByNameIgnoreCase(@Param("name") String name);
} 
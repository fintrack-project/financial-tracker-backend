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
    
    @Query("SELECT s FROM SubscriptionPlan s WHERE LOWER(s.name) = LOWER(:name)")
    Optional<SubscriptionPlan> findByNameIgnoreCase(@Param("name") String name);
} 
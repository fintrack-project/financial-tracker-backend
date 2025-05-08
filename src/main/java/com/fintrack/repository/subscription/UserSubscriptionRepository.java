package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByAccountId(UUID accountId);
    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
} 
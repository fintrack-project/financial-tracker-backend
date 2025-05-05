package com.fintrack.repository;

import com.fintrack.model.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    Optional<PaymentIntent> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<PaymentIntent> findByAccountId(UUID accountId);
    Optional<PaymentIntent> findByAccountIdAndStatus(UUID accountId, String status);
} 
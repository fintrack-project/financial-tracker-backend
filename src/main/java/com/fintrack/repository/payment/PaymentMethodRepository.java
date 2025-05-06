package com.fintrack.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fintrack.model.payment.PaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    Optional<PaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);
    List<PaymentMethod> findByAccountId(UUID accountId);
    Optional<PaymentMethod> findByAccountIdAndIsDefault(UUID accountId, boolean isDefault);
} 
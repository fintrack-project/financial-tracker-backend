package com.fintrack.controller;

import com.fintrack.model.PaymentIntent;
import com.fintrack.model.PaymentMethod;
import com.fintrack.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            UUID accountId = UUID.fromString((String) request.get("accountId"));
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String currency = (String) request.get("currency");

            PaymentIntent paymentIntent = paymentService.createPaymentIntent(accountId, amount, currency);
            return ResponseEntity.ok(paymentIntent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/attach-method")
    public ResponseEntity<?> attachPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentMethodId = request.get("paymentMethodId");

            PaymentMethod paymentMethod = paymentService.attachPaymentMethod(accountId, paymentMethodId);
            return ResponseEntity.ok(paymentMethod);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentIntentId = request.get("paymentIntentId");
            String paymentMethodId = request.get("paymentMethodId");

            // Verify that the payment intent belongs to the account
            Optional<PaymentIntent> paymentIntent = paymentService.getPaymentIntent(paymentIntentId);
            if (paymentIntent.isEmpty() || !paymentIntent.get().getAccountId().equals(accountId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment intent not found for this account"));
            }

            // Verify that the payment method belongs to the account
            Optional<PaymentMethod> paymentMethod = paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst();

            if (paymentMethod.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment method not found for this account"));
            }

            PaymentIntent confirmedIntent = paymentService.confirmPaymentIntent(paymentIntentId, paymentMethodId);
            return ResponseEntity.ok(confirmedIntent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/methods")
    public ResponseEntity<?> getPaymentMethods(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            List<PaymentMethod> paymentMethods = paymentService.getPaymentMethods(accountId);
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/default-method")
    public ResponseEntity<PaymentMethod> getDefaultPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            return paymentService.getDefaultPaymentMethod(accountId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/set-default")
    public ResponseEntity<?> setDefaultPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentMethodId = request.get("paymentMethodId");

            paymentService.setDefaultPaymentMethod(accountId, paymentMethodId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/delete-method")
    public ResponseEntity<?> deletePaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentMethodId = request.get("paymentMethodId");

            // Verify that the payment method belongs to the account
            Optional<PaymentMethod> paymentMethod = paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst();

            if (paymentMethod.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment method not found for this account"));
            }

            paymentService.deletePaymentMethod(paymentMethodId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 
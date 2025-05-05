package com.fintrack.controller;

import com.fintrack.model.PaymentIntent;
import com.fintrack.model.PaymentMethod;
import com.fintrack.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
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
            logger.trace("Received attach-method request: {}", request);
            
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentMethodId = request.get("paymentMethodId");

            if (accountId == null || paymentMethodId == null) {
                logger.trace("Invalid request - missing accountId or paymentMethodId");
                return ResponseEntity.badRequest().body(Map.of("message", "accountId and paymentMethodId are required"));
            }

            PaymentMethod paymentMethod = paymentService.attachPaymentMethod(accountId, paymentMethodId);
            logger.trace("Successfully attached payment method: {}", paymentMethod);
            
            return ResponseEntity.ok(paymentMethod);
        } catch (Exception e) {
            logger.trace("Error attaching payment method: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
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

    @PostMapping("/verify-method")
    public ResponseEntity<?> verifyPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            logger.trace("Received verify-method request: {}", request);
            
            String paymentMethodId = request.get("paymentMethodId");
            if (paymentMethodId == null) {
                logger.trace("Invalid request - missing paymentMethodId");
                return ResponseEntity.badRequest().body(Map.of("message", "paymentMethodId is required"));
            }

            Map<String, Object> paymentMethod = paymentService.verifyPaymentMethod(paymentMethodId);
            logger.trace("Successfully verified payment method: {}", paymentMethod);
            
            return ResponseEntity.ok(paymentMethod);
        } catch (Exception e) {
            logger.trace("Error verifying payment method: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
} 
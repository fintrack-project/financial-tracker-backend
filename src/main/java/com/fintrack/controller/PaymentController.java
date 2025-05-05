package com.fintrack.controller;

import com.fintrack.model.PaymentIntent;
import com.fintrack.model.PaymentMethod;
import com.fintrack.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
            String paymentIntentId = request.get("paymentIntentId");
            String paymentMethodId = request.get("paymentMethodId");

            PaymentIntent paymentIntent = paymentService.confirmPaymentIntent(paymentIntentId, paymentMethodId);
            return ResponseEntity.ok(paymentIntent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/methods/{accountId}")
    public ResponseEntity<?> getPaymentMethods(@PathVariable String accountId) {
        try {
            List<PaymentMethod> paymentMethods = paymentService.getPaymentMethods(UUID.fromString(accountId));
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/default-method/{accountId}")
    public ResponseEntity<?> getDefaultPaymentMethod(@PathVariable String accountId) {
        try {
            return paymentService.getDefaultPaymentMethod(UUID.fromString(accountId))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    @DeleteMapping("/methods/{paymentMethodId}")
    public ResponseEntity<?> deletePaymentMethod(@PathVariable String paymentMethodId) {
        try {
            paymentService.deletePaymentMethod(paymentMethodId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 
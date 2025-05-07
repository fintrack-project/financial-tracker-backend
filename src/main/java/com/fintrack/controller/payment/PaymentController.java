package com.fintrack.controller.payment;

import com.fintrack.dto.ErrorResponse;
import com.fintrack.dto.payment.PaymentMethodRequest;
import com.fintrack.dto.payment.PaymentMethodResponse;
import com.fintrack.model.payment.PaymentIntent;
import com.fintrack.model.payment.PaymentMethod;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/user/payments", produces = MediaType.APPLICATION_JSON_VALUE)
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentIntent>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            UUID accountId = UUID.fromString((String) request.get("accountId"));
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String currency = (String) request.get("currency");

            PaymentIntent paymentIntent = paymentService.createPaymentIntent(accountId, amount, currency);
            return ResponseWrapper.ok(paymentIntent);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/attach-method")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> attachPaymentMethod(@RequestBody PaymentMethodRequest request) {
        logger.trace("Received attach-method request: {}", request);
        
        try {
            PaymentMethodResponse response = paymentService.attachPaymentMethod(
                request.getAccountId(), 
                request.getPaymentMethodId()
            );
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error while attaching payment method: {}", e.getMessage());
            // Handle Stripe-specific errors
            ErrorResponse errorResponse = new ErrorResponse(
                "payment_error",
                e.getMessage(),
                e.getStripeError() != null ? e.getStripeError().getCode() : null
            );
            return ResponseWrapper.badRequest(errorResponse.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while attaching payment method: {}", e.getMessage(), e);
            // Handle other errors
            ErrorResponse errorResponse = new ErrorResponse(
                "internal_error",
                "An unexpected error occurred while processing your payment",
                null
            );
            return ResponseWrapper.internalServerError(errorResponse.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentIntent>> confirmPayment(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentIntentId = request.get("paymentIntentId");
            String paymentMethodId = request.get("paymentMethodId");

            // Verify that the payment intent belongs to the account
            Optional<PaymentIntent> paymentIntent = paymentService.getPaymentIntent(paymentIntentId);
            if (paymentIntent.isEmpty() || !paymentIntent.get().getAccountId().equals(accountId)) {
                return ResponseWrapper.badRequest("Payment intent not found for this account");
            }

            // Verify that the payment method belongs to the account
            Optional<PaymentMethod> paymentMethod = paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst();

            if (paymentMethod.isEmpty()) {
                return ResponseWrapper.badRequest("Payment method not found for this account");
            }

            PaymentIntent confirmedIntent = paymentService.confirmPaymentIntent(paymentIntentId, paymentMethodId);
            return ResponseWrapper.ok(confirmedIntent);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/methods")
    public ResponseEntity<ApiResponse<List<PaymentMethod>>> getPaymentMethods(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            List<PaymentMethod> paymentMethods = paymentService.getPaymentMethods(accountId);
            return ResponseWrapper.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/default-method")
    public ResponseEntity<ApiResponse<PaymentMethod>> getDefaultPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            Optional<PaymentMethod> defaultMethod = paymentService.getDefaultPaymentMethod(accountId);
            if (defaultMethod.isPresent()) {
                return ResponseWrapper.ok(defaultMethod.get());
            } else {
                return ResponseWrapper.notFound();
            }
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentMethodId = request.get("paymentMethodId");

            paymentService.setDefaultPaymentMethod(accountId, paymentMethodId);
            return ResponseWrapper.ok(null, "Default payment method set successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/delete-method")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(@RequestBody Map<String, String> request) {
        try {
            UUID accountId = UUID.fromString(request.get("accountId"));
            String paymentMethodId = request.get("paymentMethodId");

            // Verify that the payment method belongs to the account
            Optional<PaymentMethod> paymentMethod = paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst();

            if (paymentMethod.isEmpty()) {
                return ResponseWrapper.badRequest("Payment method not found for this account");
            }

            paymentService.deletePaymentMethod(paymentMethodId);
            return ResponseWrapper.ok(null, "Payment method deleted successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/verify-method")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPaymentMethod(@RequestBody Map<String, String> request) {
        try {
            logger.trace("Received verify-method request: {}", request);
            
            String paymentMethodId = request.get("paymentMethodId");
            if (paymentMethodId == null) {
                logger.trace("Invalid request - missing paymentMethodId");
                return ResponseWrapper.badRequest("paymentMethodId is required");
            }

            Map<String, Object> paymentMethod = paymentService.verifyPaymentMethod(paymentMethodId);
            logger.trace("Successfully verified payment method: {}", paymentMethod);
            return ResponseWrapper.ok(paymentMethod);
        } catch (Exception e) {
            logger.trace("Error verifying payment method: {}", e.getMessage(), e);
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
} 
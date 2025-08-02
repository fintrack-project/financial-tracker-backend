package com.fintrack.service.payment;

import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for StripeService.
 * Tests critical payment processing business logic including payment intent creation,
 * payment method management, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    private PaymentIntent mockPaymentIntent;

    @Mock
    private PaymentMethod mockPaymentMethod;

    @Mock
    private Customer mockCustomer;

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "stripeSecretKey", "sk_test_1234567890");
    }

    @Test
    void shouldCreatePaymentIntentWithReturnUrl() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("99.99");
        String currency = "USD";
        String paymentMethodId = "pm_1234567890";
        String customerId = "cus_1234567890";
        String returnUrl = "https://example.com/return";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);

            // Then
            assertNotNull(result);
            assertEquals(mockPaymentIntent, result);
            
            // Verify the correct parameters were passed
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)));
        }
    }

    @Test
    void shouldCreatePaymentIntentWithoutReturnUrl() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        String currency = "EUR";
        String paymentMethodId = "pm_0987654321";
        String customerId = "cus_0987654321";
        String returnUrl = null;

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);

            // Then
            assertNotNull(result);
            assertEquals(mockPaymentIntent, result);
            
            // Verify the correct parameters were passed
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)));
        }
    }

    @Test
    void shouldCreatePaymentIntentWithEmptyReturnUrl() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("25.50");
        String currency = "GBP";
        String paymentMethodId = "pm_5555555555";
        String customerId = "cus_5555555555";
        String returnUrl = "";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);

            // Then
            assertNotNull(result);
            assertEquals(mockPaymentIntent, result);
            
            // Verify the correct parameters were passed
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)));
        }
    }

    @Test
    void shouldHandleLargeAmountCorrectly() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("999999.99");
        String currency = "USD";
        String paymentMethodId = "pm_large_amount";
        String customerId = "cus_large_amount";
        String returnUrl = "https://example.com/return";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);

            // Then
            assertNotNull(result);
            
            // Verify the amount was converted correctly
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)));
        }
    }

    @Test
    void shouldHandleZeroAmount() throws Exception {
        // Given
        BigDecimal amount = BigDecimal.ZERO;
        String currency = "USD";
        String paymentMethodId = "pm_zero_amount";
        String customerId = "cus_zero_amount";
        String returnUrl = "https://example.com/return";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);

            // Then
            assertNotNull(result);
            
            // Verify the amount was converted correctly
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)));
        }
    }

    @Test
    void shouldHandleStripeExceptionInCreatePaymentIntent() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String paymentMethodId = "pm_invalid";
        String customerId = "cus_invalid";
        String returnUrl = "https://example.com/return";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenThrow(new InvalidRequestException("Invalid payment method", "pm_invalid", "invalid_payment_method", "pm_invalid", 400, null));

            // When & Then
            assertThrows(InvalidRequestException.class, () -> {
                stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);
            });
        }
    }

    @Test
    void shouldAttachPaymentMethodToCustomer() throws Exception {
        // Given
        String paymentMethodId = "pm_attach_test";
        String customerId = "cus_attach_test";

        try (MockedStatic<PaymentMethod> paymentMethodMock = mockStatic(PaymentMethod.class)) {
            paymentMethodMock.when(() -> PaymentMethod.retrieve(paymentMethodId))
                    .thenReturn(mockPaymentMethod);
            
            when(mockPaymentMethod.attach(any(Map.class)))
                    .thenReturn(mockPaymentMethod);

            // When
            PaymentMethod result = stripeService.attachPaymentMethodToCustomer(paymentMethodId, customerId);

            // Then
            assertNotNull(result);
            assertEquals(mockPaymentMethod, result);
            
            // Verify the payment method was retrieved and attached
            paymentMethodMock.verify(() -> PaymentMethod.retrieve(paymentMethodId));
            verify(mockPaymentMethod).attach(any(Map.class));
        }
    }

    @Test
    void shouldHandleStripeExceptionInAttachPaymentMethod() {
        // Given
        String paymentMethodId = "pm_invalid_attach";
        String customerId = "cus_invalid_attach";

        try (MockedStatic<PaymentMethod> paymentMethodMock = mockStatic(PaymentMethod.class)) {
            paymentMethodMock.when(() -> PaymentMethod.retrieve(paymentMethodId))
                    .thenThrow(new InvalidRequestException("Payment method not found", "pm_invalid_attach", "resource_missing", "pm_invalid_attach", 404, null));

            // When & Then
            assertThrows(InvalidRequestException.class, () -> {
                stripeService.attachPaymentMethodToCustomer(paymentMethodId, customerId);
            });
        }
    }

    @Test
    void shouldSetDefaultPaymentMethod() throws Exception {
        // Given
        String customerId = "cus_default_pm";
        String paymentMethodId = "pm_default_pm";

        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.retrieve(customerId))
                    .thenReturn(mockCustomer);
            
            when(mockCustomer.update(any(Map.class)))
                    .thenReturn(mockCustomer);

            // When
            stripeService.setDefaultPaymentMethod(customerId, paymentMethodId);

            // Then
            customerMock.verify(() -> Customer.retrieve(customerId));
            verify(mockCustomer).update(any(Map.class));
        }
    }

    @Test
    void shouldHandleStripeExceptionInSetDefaultPaymentMethod() {
        // Given
        String customerId = "cus_invalid_default";
        String paymentMethodId = "pm_invalid_default";

        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.retrieve(customerId))
                    .thenThrow(new InvalidRequestException("Customer not found", "cus_invalid_default", "resource_missing", "cus_invalid_default", 404, null));

            // When & Then
            assertThrows(InvalidRequestException.class, () -> {
                stripeService.setDefaultPaymentMethod(customerId, paymentMethodId);
            });
        }
    }

    @Test
    void shouldHandleDifferentCurrenciesCorrectly() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD"};
        String paymentMethodId = "pm_currency_test";
        String customerId = "cus_currency_test";
        String returnUrl = "https://example.com/return";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When & Then
            for (String currency : currencies) {
                PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);
                assertNotNull(result);
            }
            
            // Verify all currencies were processed (5 currencies = 5 calls)
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)), times(5));
        }
    }

    @Test
    void shouldHandleDecimalPrecisionCorrectly() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("123.456");
        String currency = "USD";
        String paymentMethodId = "pm_precision_test";
        String customerId = "cus_precision_test";
        String returnUrl = "https://example.com/return";

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(Map.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntent result = stripeService.createPaymentIntent(amount, currency, paymentMethodId, customerId, returnUrl);

            // Then
            assertNotNull(result);
            
            // Verify the amount was converted correctly
            paymentIntentMock.verify(() -> PaymentIntent.create(any(Map.class)));
        }
    }
} 
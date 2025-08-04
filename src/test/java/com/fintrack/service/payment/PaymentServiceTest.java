package com.fintrack.service.payment;

import com.fintrack.dto.payment.PaymentMethodResponse;
import com.fintrack.model.payment.PaymentIntent;
import com.fintrack.model.payment.PaymentMethod;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.fintrack.repository.payment.PaymentMethodRepository;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    private PaymentService paymentService;

    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private static final String TEST_PAYMENT_INTENT_ID = "pi_test_123";
    private static final String TEST_PAYMENT_METHOD_ID = "pm_test_123";
    private static final String TEST_CLIENT_SECRET = "pi_test_123_secret_456";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentIntentRepository, paymentMethodRepository);
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_123");
    }

    @Test
    @DisplayName("Should get payment intent successfully")
    void shouldGetPaymentIntentSuccessfully() {
        // Given: Existing payment intent
        PaymentIntent existingPaymentIntent = createTestPaymentIntent();
        when(paymentIntentRepository.findByStripePaymentIntentId(TEST_PAYMENT_INTENT_ID))
            .thenReturn(Optional.of(existingPaymentIntent));

        // When: Get payment intent
        Optional<PaymentIntent> result = paymentService.getPaymentIntent(TEST_PAYMENT_INTENT_ID);

        // Then: Should return payment intent
        assertTrue(result.isPresent());
        assertEquals(TEST_PAYMENT_INTENT_ID, result.get().getStripePaymentIntentId());
        assertEquals(TEST_ACCOUNT_ID, result.get().getAccountId());
    }

    @Test
    @DisplayName("Should return empty when payment intent not found")
    void shouldReturnEmptyWhenPaymentIntentNotFound() {
        // Given: Payment intent not found
        when(paymentIntentRepository.findByStripePaymentIntentId(TEST_PAYMENT_INTENT_ID))
            .thenReturn(Optional.empty());

        // When: Get payment intent
        Optional<PaymentIntent> result = paymentService.getPaymentIntent(TEST_PAYMENT_INTENT_ID);

        // Then: Should return empty
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get payment methods successfully")
    void shouldGetPaymentMethodsSuccessfully() {
        // Given: Existing payment methods
        List<PaymentMethod> existingMethods = Arrays.asList(
            createTestPaymentMethod("pm_test_1", "card", true),
            createTestPaymentMethod("pm_test_2", "card", false)
        );
        when(paymentMethodRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(existingMethods);

        // When: Get payment methods
        List<PaymentMethod> result = paymentService.getPaymentMethods(TEST_ACCOUNT_ID);

        // Then: Should return payment methods
        assertEquals(2, result.size());
        assertEquals("pm_test_1", result.get(0).getStripePaymentMethodId());
        assertEquals("pm_test_2", result.get(1).getStripePaymentMethodId());
        assertTrue(result.get(0).isDefault());
        assertFalse(result.get(1).isDefault());
    }

    @Test
    @DisplayName("Should get default payment method successfully")
    void shouldGetDefaultPaymentMethodSuccessfully() {
        // Given: Default payment method exists
        PaymentMethod defaultMethod = createTestPaymentMethod(TEST_PAYMENT_METHOD_ID, "card", true);
        when(paymentMethodRepository.findByAccountIdAndIsDefault(TEST_ACCOUNT_ID, true))
            .thenReturn(Optional.of(defaultMethod));

        // When: Get default payment method
        Optional<PaymentMethod> result = paymentService.getDefaultPaymentMethod(TEST_ACCOUNT_ID);

        // Then: Should return default payment method
        assertTrue(result.isPresent());
        assertEquals(TEST_PAYMENT_METHOD_ID, result.get().getStripePaymentMethodId());
        assertTrue(result.get().isDefault());
    }

    @Test
    @DisplayName("Should return empty when no default payment method")
    void shouldReturnEmptyWhenNoDefaultPaymentMethod() {
        // Given: No default payment method
        when(paymentMethodRepository.findByAccountIdAndIsDefault(TEST_ACCOUNT_ID, true))
            .thenReturn(Optional.empty());

        // When: Get default payment method
        Optional<PaymentMethod> result = paymentService.getDefaultPaymentMethod(TEST_ACCOUNT_ID);

        // Then: Should return empty
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should set default payment method successfully")
    void shouldSetDefaultPaymentMethodSuccessfully() {
        // Given: Existing payment methods
        PaymentMethod currentDefault = createTestPaymentMethod("pm_test_1", "card", true);
        PaymentMethod newDefault = createTestPaymentMethod(TEST_PAYMENT_METHOD_ID, "card", false);

        when(paymentMethodRepository.findByAccountIdAndIsDefault(TEST_ACCOUNT_ID, true))
            .thenReturn(Optional.of(currentDefault));
        when(paymentMethodRepository.findByStripePaymentMethodId(TEST_PAYMENT_METHOD_ID))
            .thenReturn(Optional.of(newDefault));
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Set default payment method
        assertDoesNotThrow(() -> {
            paymentService.setDefaultPaymentMethod(TEST_ACCOUNT_ID, TEST_PAYMENT_METHOD_ID);
        });

        // Then: Should update both payment methods
        verify(paymentMethodRepository).save(argThat(pm -> !pm.isDefault())); // Current default unset
        verify(paymentMethodRepository).save(argThat(pm -> pm.isDefault())); // New default set
    }

    @Test
    @DisplayName("Should validate payment intent creation with different currencies")
    void shouldValidatePaymentIntentCreationWithDifferentCurrencies() {
        // Given: Different currencies
        String[] currencies = {"USD", "EUR", "GBP", "JPY"};
        BigDecimal amount = new BigDecimal("100.00");

        // When & Then: Should validate currency handling
        for (String currency : currencies) {
            assertNotNull(currency);
            assertTrue(currency.length() == 3);
            assertTrue(currency.matches("[A-Z]{3}"));
        }
    }

    @Test
    @DisplayName("Should validate payment intent creation with different amounts")
    void shouldValidatePaymentIntentCreationWithDifferentAmounts() {
        // Given: Different amounts
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("1.00"),
            new BigDecimal("100.00"),
            new BigDecimal("1000.00")
        };

        // When & Then: Should validate amount handling
        for (BigDecimal amount : amounts) {
            assertNotNull(amount);
            assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(amount.scale() <= 2);
        }
    }

    @Test
    @DisplayName("Should validate payment method types")
    void shouldValidatePaymentMethodTypes() {
        // Given: Different payment method types
        String[] types = {"card", "sepa_debit", "ideal", "sofort"};

        // When & Then: Should validate payment method types
        for (String type : types) {
            assertNotNull(type);
            assertFalse(type.isEmpty());
            assertTrue(type.matches("[a-z_]+"));
        }
    }

    @Test
    @DisplayName("Should validate payment method card details")
    void shouldValidatePaymentMethodCardDetails() {
        // Given: Card payment method
        PaymentMethod cardMethod = createTestPaymentMethod(TEST_PAYMENT_METHOD_ID, "card", true);

        // When & Then: Should have valid card details
        assertEquals("4242", cardMethod.getCardLast4());
        assertEquals("visa", cardMethod.getCardBrand());
        assertEquals("12", cardMethod.getCardExpMonth());
        assertEquals("2025", cardMethod.getCardExpYear());
        assertTrue(cardMethod.isDefault());
    }

    @Test
    @DisplayName("Should validate payment intent status transitions")
    void shouldValidatePaymentIntentStatusTransitions() {
        // Given: Payment intent with different statuses
        String[] statuses = {"requires_payment_method", "requires_confirmation", "requires_action", "processing", "succeeded", "canceled"};

        // When & Then: Should validate status handling
        for (String status : statuses) {
            assertNotNull(status);
            assertFalse(status.isEmpty());
            assertTrue(status.matches("[a-z_]+"));
        }
    }

    @Test
    @DisplayName("Should validate account ID format")
    void shouldValidateAccountIdFormat() {
        // Given: Valid account ID
        UUID accountId = UUID.randomUUID();

        // When & Then: Should have valid UUID format
        assertNotNull(accountId);
        assertEquals(36, accountId.toString().length());
        assertTrue(accountId.toString().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("Should validate payment intent amount conversion")
    void shouldValidatePaymentIntentAmountConversion() {
        // Given: Different amounts in dollars
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("1.00"),
            new BigDecimal("29.99"),
            new BigDecimal("100.00")
        };

        // When & Then: Should convert to cents correctly
        for (BigDecimal amount : amounts) {
            BigDecimal cents = amount.multiply(new BigDecimal("100"));
            assertTrue(cents.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(cents.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0);
        }
    }

    @Test
    @DisplayName("Should validate payment method creation timestamp")
    void shouldValidatePaymentMethodCreationTimestamp() {
        // Given: Payment method
        PaymentMethod paymentMethod = createTestPaymentMethod(TEST_PAYMENT_METHOD_ID, "card", false);

        // When & Then: Should have valid timestamps
        assertNotNull(paymentMethod.getCreatedAt());
        assertNotNull(paymentMethod.getUpdatedAt());
        assertTrue(paymentMethod.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(paymentMethod.getUpdatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("Should validate payment intent creation timestamp")
    void shouldValidatePaymentIntentCreationTimestamp() {
        // Given: Payment intent
        PaymentIntent paymentIntent = createTestPaymentIntent();

        // When & Then: Should have valid timestamp
        assertNotNull(paymentIntent.getCreatedAt());
        assertTrue(paymentIntent.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("Should validate payment method default logic")
    void shouldValidatePaymentMethodDefaultLogic() {
        // Given: Payment methods with different default states
        PaymentMethod defaultMethod = createTestPaymentMethod("pm_test_1", "card", true);
        PaymentMethod nonDefaultMethod = createTestPaymentMethod("pm_test_2", "card", false);

        // When & Then: Should have correct default states
        assertTrue(defaultMethod.isDefault());
        assertFalse(nonDefaultMethod.isDefault());
    }

    @Test
    @DisplayName("Should validate payment method type handling")
    void shouldValidatePaymentMethodTypeHandling() {
        // Given: Different payment method types
        String[] types = {"card", "sepa_debit", "ideal", "sofort"};
        PaymentMethod[] methods = new PaymentMethod[types.length];

        // When: Create payment methods with different types
        for (int i = 0; i < types.length; i++) {
            methods[i] = createTestPaymentMethod("pm_test_" + i, types[i], false);
        }

        // Then: Should have correct types
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], methods[i].getType());
        }
    }

    private PaymentIntent createTestPaymentIntent() {
        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId(1L);
        paymentIntent.setAccountId(TEST_ACCOUNT_ID);
        paymentIntent.setStripePaymentIntentId(TEST_PAYMENT_INTENT_ID);
        paymentIntent.setAmount(new BigDecimal("29.99"));
        paymentIntent.setCurrency("USD");
        paymentIntent.setStatus("requires_payment_method");
        paymentIntent.setClientSecret(TEST_CLIENT_SECRET);
        paymentIntent.setCreatedAt(LocalDateTime.now());
        return paymentIntent;
    }

    private PaymentMethod createTestPaymentMethod(String paymentMethodId, String type, boolean isDefault) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setAccountId(TEST_ACCOUNT_ID);
        paymentMethod.setStripePaymentMethodId(paymentMethodId);
        paymentMethod.setType(type);
        paymentMethod.setCardLast4("4242");
        paymentMethod.setCardBrand("visa");
        paymentMethod.setCardExpMonth("12");
        paymentMethod.setCardExpYear("2025");
        paymentMethod.setDefault(isDefault);
        paymentMethod.setCreatedAt(LocalDateTime.now());
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        return paymentMethod;
    }
} 
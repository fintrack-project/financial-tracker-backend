package com.fintrack.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class StripeService {
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String paymentMethodId, String customerId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Convert to cents
        params.put("currency", currency.toLowerCase());
        params.put("payment_method", paymentMethodId);
        params.put("customer", customerId);
        params.put("confirm", false);
        params.put("setup_future_usage", "off_session");
        
        return PaymentIntent.create(params);
    }
    
    public PaymentMethod attachPaymentMethodToCustomer(String paymentMethodId, String customerId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        
        return paymentMethod.attach(params);
    }
    
    public void setDefaultPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        Customer customer = Customer.retrieve(customerId);
        Map<String, Object> params = new HashMap<>();
        params.put("invoice_settings", Map.of("default_payment_method", paymentMethodId));
        
        customer.update(params);
    }
} 
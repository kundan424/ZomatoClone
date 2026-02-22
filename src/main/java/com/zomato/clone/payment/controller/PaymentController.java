package com.zomato.clone.payment.controller;

import com.stripe.exception.StripeException;
import com.zomato.clone.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payments/checkout/1
    @PostMapping("/checkout/{orderId}")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @PathVariable Long orderId) {
        try {
            String paymentLink = paymentService.createPaymentLink(orderId);
            return ResponseEntity.ok(Map.of("url", paymentLink));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

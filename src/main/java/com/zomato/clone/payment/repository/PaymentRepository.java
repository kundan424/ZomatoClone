package com.zomato.clone.payment.repository;

import com.zomato.clone.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface
PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment by the Stripe ID (Critical for Webhooks!)
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    // Find payment by Order ID
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

}

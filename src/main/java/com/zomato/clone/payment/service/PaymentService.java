package com.zomato.clone.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zomato.clone.enums.OrderStatus;
import com.zomato.clone.enums.PaymentStatus;
import com.zomato.clone.order.entity.Order;
import com.zomato.clone.order.repository.OrderRepository;
import com.zomato.clone.payment.entity.Payment;
import com.zomato.clone.payment.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    // Initialize Stripe with the Secret Key on startup
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public String createPaymentLink(Long orderId) throws StripeException {

        // validate the order
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 2. Build the Stripe Session (The "Checkout Page")
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:3000/payment/success?session_id={CHECKOUT_SESSION_ID}") // Frontend Success Page
                .setCancelUrl("http://localhost:3000/payment/cancel") // frontend url
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("inr")
                                                .setUnitAmount(order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()) // convert to paise
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Zomato Clone Order #" + order.getId())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                // METADATA: This is crucial for matching the payment later in Webhooks
                .putMetadata("order_id", order.getId().toString())
                .build();

        // 3. call the stripe api
        Session session = Session.create(params);

        // 4. Save the "Promise" of Payment (Status: PENDING)
        // we save the PaymentIntentId so we can verify it later

        Payment payment = Payment.builder()
                .order(order)
                .stripePaymentIntentId(session.getPaymentIntent()) // The ID needed for tracking
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepo.save(payment);

        // 5. return the url so Frontend can redirect the user
        return session.getUrl();
    }

    @Transactional
    public void handleSuccessfulPayment(String orderStringId) {
        long orderId = Long.parseLong(orderStringId);

        //1. find the payment record
        Payment payment = paymentRepo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment record not found for Order: " + orderId));

        // 2. find the order record
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // 3. update status
        payment.setStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.CONFIRMED); // kitchen start cooking

        //4. Save (Handled automatically by @Transactional, but explicit saves are fine)
        paymentRepo.save(payment);
        orderRepo.save(order);

        System.out.println("✅ Payment Confirmed for Order ID: " + orderId);
    }
}

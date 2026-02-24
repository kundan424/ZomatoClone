package com.zomato.clone.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.zomato.clone.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class StripeWebhookController implements StripeWebhookApi {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Override
    public ResponseEntity<String> handleStripeEvent(
            String payload,
            String sigHeader
    ) {
        Event event;

        try {
            // Verify the cryptographic signature sent by Stripe
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("🔥 Webhook received: " + event.getType());
        } catch (SignatureVerificationException e) {
            System.err.println("⚠️ Webhook signature verification failed.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            System.err.println("❌ Webhook error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }

        // Handle a successfully completed payment
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = extractSessionUnsafe(event);
            if (session != null) {
                String orderIdStr = session.getMetadata().get("order_id");
                if (orderIdStr != null) {
                    paymentService.handleSuccessfulPayment(
                            orderIdStr,
                            session.getId(),
                            session.getPaymentIntent()
                    );
                    System.out.println("✅ Payment successfully updated in DB.");
                } else {
                    System.err.println("❌ CRITICAL: Metadata 'order_id' was missing from the session!");
                }
            }
        }
        // Handle a checkout session that was abandoned and expired
        else if ("checkout.session.expired".equals(event.getType())) {
            Session session = extractSessionUnsafe(event);
            if (session != null) {
                String orderIdStr = session.getMetadata().get("order_id");
                if (orderIdStr != null) {
                    paymentService.handleExpiredPayment(orderIdStr, session.getId());
                    System.out.println("❌ Checkout expired. Order cancelled in DB.");
                }
            }
        }

        // Always return 200 OK quickly so Stripe marks the webhook as delivered
        return ResponseEntity.ok("Success");
    }

    /**
     * Helper method to safely extract the Session object even if the Stripe
     * Java SDK version does not perfectly match the Stripe Dashboard API version.
     */
    private Session extractSessionUnsafe(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;

        if (deserializer.getObject().isPresent()) {
            stripeObject = deserializer.getObject().get();
        } else {
            try {
                System.out.println("⚠️ API Version mismatch detected. Extracting unsafely.");
                // We must catch the checked exception thrown by deserializeUnsafe()
                stripeObject = deserializer.deserializeUnsafe();
            } catch (com.stripe.exception.EventDataObjectDeserializationException e) {
                System.err.println("❌ Failed to deserialize Stripe object: " + e.getMessage());
                return null;
            }
        }

        if (stripeObject instanceof Session session) {
            return session;
        } else {
            System.err.println("❌ Expected a Session object, but received a different StripeObject.");
            return null;
        }
    }
}

// command to start stripe cli
//docker run --rm -it stripe/stripe-cli \
//  --api-key YOUR_STRIPE_SECRET_KEY \
//  listen --forward-to host.docker.internal:8080/api/webhooks/stripe
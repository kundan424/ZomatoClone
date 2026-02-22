package com.zomato.clone.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.zomato.clone.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;

        try {
            // 1. Verify the Signature (Security Check)
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            // Invalid signature! Someone is trying to hack your payment endpoint.
            System.err.println("⚠️ Webhook signature verification failed.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }

        // 2. Handle the Event
        // We only care about "checkout.session.completed" for this phase
        if ("checkout.session.completed".equals(event.getType())) {

            // Deserialize the data into a Session object
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (dataObjectDeserializer.getObject().isPresent()) {
                Session session = (Session) dataObjectDeserializer.getObject().get();

                // 3. Extract the metadata we injected earlier
                String orderIdStr = session.getMetadata().get("order_id");

                if (orderIdStr != null) {
                    // 4. Update the database!
                    paymentService.handleSuccessfulPayment(orderIdStr);
                }
            }
        }

        // 5. Always return 200 OK quickly so Stripe knows we received it
        return ResponseEntity.ok("Success");
    }
}

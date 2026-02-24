package com.zomato.clone.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Webhook Management", description = "Secure endpoints for receiving external provider events")
@RequestMapping("/api/webhooks")
public interface StripeWebhookApi {


    @Operation(
            summary = "Handle Stripe Events",
            description = "Receives and securely verifies asynchronous payment events directly from Stripe."
    )
    @ApiResponses( value = {
            @ApiResponse(responseCode = "200", description = "Event received and processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or malformed payload")
    })
    @PostMapping("/stripe")
    ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    );
}

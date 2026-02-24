package com.zomato.clone.payment.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Tag(name = "Payment Management", description = "APIs for handling Stripe checkout sessions")
@RequestMapping("/api/payments")
public interface PaymentApi {

    @Operation(
            summary = "Create a Stripe Checkout Session",
            description = "Generates a secure Stripe payment link for a pending order."
    )
    @ApiResponses( value = {
            @ApiResponse(responseCode = "200", description = "Successfully generated the payment link"),
            @ApiResponse(responseCode = "403", description = "Unauthorized - Valid JWT required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Stripe API error", content = @Content)
    })
    @PostMapping("/checkout/{orderId}")
    ResponseEntity<Map<String , String >> createCheckoutSession (@PathVariable("orderId") Long orderId);
}

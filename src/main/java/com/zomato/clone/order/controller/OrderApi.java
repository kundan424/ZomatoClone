package com.zomato.clone.order.controller;

import com.zomato.clone.order.dto.OrderResponse;
import com.zomato.clone.order.dto.PlaceOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Tag(name = "Order Management", description = "APIs for placing and tracking food orders")
@RequestMapping("/api/orders")
public interface OrderApi {


    @Operation(
            summary = "Place a new Order",
            description = "Locks inventory in Redis, calculates the total bill, and creates a pending order."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order successfully created"),
            @ApiResponse(responseCode = "400", description = "Item out of stock or concurrency conflict")
    })
    @PostMapping
    ResponseEntity<OrderResponse> placeOrder(@RequestBody PlaceOrderRequest request, Principal principal);


    @Operation(
            summary = "Get User Orders",
            description = "Retrieves a cached list of all previous orders for the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved order history")
    @GetMapping
    ResponseEntity<List<OrderResponse>> getMyOrders(Principal principal);
}

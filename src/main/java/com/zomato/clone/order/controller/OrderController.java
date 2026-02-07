package com.zomato.clone.order.controller;

import com.zomato.clone.order.dto.OrderResponse;
import com.zomato.clone.order.dto.PlaceOrderRequest;
import com.zomato.clone.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    // 1. Place Order (Authenticated User)
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestBody PlaceOrderRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(service.placeOrder(request, principal.getName()));
    }

    // 2. Get My Order History
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Principal principal) {
        return ResponseEntity.ok(service.getUserOrders(principal.getName()));
    }
}
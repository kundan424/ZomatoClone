package com.zomato.clone.order.dto;

import com.zomato.clone.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String restaurantName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime placedAt;
    
}

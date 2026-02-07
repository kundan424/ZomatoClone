package com.zomato.clone.order.dto;

import com.zomato.clone.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    private Long restaurantId;
    private List<OrderItemRequest> items;


    /**
     * OrderItemRequest represents an individual menu item
     * included in an order.
     *
     * <p>
     * This nested static class is used to logically group
     * order item details inside the parent request.
     */
    @Data
    @Builder
    public static class OrderItemRequest {
        private Long menuItemId;
        private Integer quantity;

    }
}

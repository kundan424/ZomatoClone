package com.zomato.clone.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for sending MenuItem data to clients.
 * Safe for caching (Redis) and API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse implements Serializable {

    private Long id;
    private Long restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer availableQuantity;
    private Boolean isAvailable;

}


package com.zomato.clone.restaurant.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MenuItemRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer availableQuantity;
}
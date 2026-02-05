package com.zomato.clone.restaurant.dto;

import lombok.Data;

@Data
public class CreateRestaurantRequest {
    private String name;
    private String description;
    private String address;
}

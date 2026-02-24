package com.zomato.clone.restaurant.controller;

import com.zomato.clone.order.dto.MenuItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Menu Management", description = "APIs for retrieving restaurant catalog data")
@RequestMapping("/api/restaurants")
public interface MenuApi {


    @Operation(
            summary = "Get Restaurant Menu",
            description = "Retrieves all available menu items for a specific restaurant."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the menu list")
    @GetMapping("/{restaurantId}/menu")
    ResponseEntity<List<MenuItemResponse>> getMenu(@PathVariable("restaurantId") Long restaurantId);
}

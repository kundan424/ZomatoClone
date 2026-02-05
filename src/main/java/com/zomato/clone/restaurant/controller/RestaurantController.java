package com.zomato.clone.restaurant.controller;

import com.zomato.clone.restaurant.dto.CreateRestaurantRequest;
import com.zomato.clone.restaurant.dto.MenuItemRequest;
import com.zomato.clone.restaurant.entity.MenuItem;
import com.zomato.clone.restaurant.entity.Restaurant;
import com.zomato.clone.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * RestaurantController exposes REST APIs related to restaurants
 * and their menus.
 *
 * <p>
 * This controller:
 * <ul>
 *   <li>Allows restaurant owners to create restaurants</li>
 *   <li>Allows restaurant owners to manage menu items</li>
 *   <li>Allows users to browse restaurants and menus</li>
 * </ul>
 *
 * <p>
 * Authentication is handled using JWT.
 * The authenticated user's email is obtained via {@link Principal}.
 */

@RestController
@RequestMapping("api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService service;

    /**
     * Creates a new restaurant.
     *
     * <p>
     * Access rules:
     * <ul>
     *   <li>Endpoint is protected</li>
     *   <li>User must be authenticated</li>
     *   <li>User must have RESTAURANT role</li>
     * </ul>
     *
     * <p>
     * The authenticated user's email is extracted from the JWT token
     * using {@link Principal#getName()}.
     *
     * @param request   contains restaurant creation details
     * @param principal represents the authenticated user
     * @return newly created Restaurant
     */
    @PostMapping
    public ResponseEntity<Restaurant> createRestaurant(
            @RequestBody CreateRestaurantRequest request,
            Principal principal
    ) {
        //Principal.getName() return email from jwt token
        return ResponseEntity.ok(service.createRestaurant(request, principal.getName()));
    }

    /**
     * Adds a menu item to an existing restaurant.
     *
     * <p>
     * Access rules:
     * <ul>
     *   <li>User must be authenticated</li>
     *   <li>User must be the owner of the restaurant</li>
     * </ul>
     *
     * @param restaurantId ID of the restaurant
     * @param request      contains menu item details
     * @param principal    represents the authenticated user
     * @return newly created MenuItem
     */
    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<MenuItem> addMenuItem(
            @PathVariable Long restaurantId,
            @RequestBody MenuItemRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(service.addMenuItem(restaurantId, request, principal.getName()));
    }

    /**
     * Retrieves all restaurants.
     *
     * <p>
     * This endpoint is public and can be accessed by:
     * <ul>
     *   <li>Authenticated users</li>
     *   <li>Unauthenticated users</li>
     * </ul>
     *
     * @return list of all restaurants
     */
    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        return ResponseEntity.ok(service.getAllRestaurants());
    }

    /**
     * Retrieves the menu for a specific restaurant.
     *
     * <p>
     * This endpoint is public and does not require authentication.
     *
     * @param restaurantId ID of the restaurant
     * @return list of menu items for the restaurant
     */
    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getMenu(
            @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(service.getMenu(restaurantId));
    }
}

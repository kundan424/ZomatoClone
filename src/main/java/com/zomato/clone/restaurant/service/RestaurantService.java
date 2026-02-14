package com.zomato.clone.restaurant.service;

import com.zomato.clone.enums.UserRole;
import com.zomato.clone.restaurant.dto.CreateRestaurantRequest;
import com.zomato.clone.restaurant.dto.MenuItemRequest;
import com.zomato.clone.restaurant.entity.MenuItem;
import com.zomato.clone.restaurant.entity.Restaurant;
import com.zomato.clone.restaurant.repository.MenuItemRepository;
import com.zomato.clone.restaurant.repository.RestaurantRepository;
import com.zomato.clone.user.entity.User;
import com.zomato.clone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RestaurantService contains business logic related to restaurants
 * and their menus.
 *
 * <p>
 * Responsibilities of this service:
 * <ul>
 *   <li>Create restaurants for authorized restaurant owners</li>
 *   <li>Add menu items to restaurants</li>
 *   <li>Fetch restaurants and menus for users</li>
 * </ul>
 *
 * <p>
 * Security rules enforced here ensure that only legitimate owners
 * can manage their restaurants and menus.
 */

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepo;
    private final MenuItemRepository menuItemRepo;
    private final UserRepository userRepo;

    /**
     * Creates a new restaurant for a restaurant owner.
     *
     * <p>
     * Business rules:
     * <ul>
     *   <li>User must exist</li>
     *   <li>User must have RESTAURANT role</li>
     * </ul>
     *
     * <p>
     * The method is transactional to ensure consistency
     * when saving restaurant data.
     *
     * @param request   contains restaurant creation details
     * @param userEmail email of the authenticated user
     * @return newly created Restaurant entity
     * @throws UsernameNotFoundException if user does not exist
     * @throws RuntimeException if user is not a restaurant owner
     */
    @Transactional

    public Restaurant createRestaurant(CreateRestaurantRequest request, String userEmail) {

        // fetch the authenticated user
        User owner = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Validation: Only RESTAURANT role can create restaurants
        if (owner.getRole() != UserRole.RESTAURANT) {
            throw new RuntimeException("Only Restaurant Owners can create restaurants");
        }

        Restaurant restaurant = Restaurant.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .isOpen(true)
                .build();

        return restaurantRepo.save(restaurant);
    }

    /**
     * Adds a new menu item to an existing restaurant.
     *
     * <p>
     * Business rules:
     * <ul>
     *   <li>Restaurant must exist</li>
     *   <li>Only the restaurant owner can add menu items</li>
     * </ul>
     *
     * @param restaurantId ID of the restaurant
     * @param request      contains menu item details
     * @param userEmail    email of the authenticated user
     * @return newly created MenuItem entity
     * @throws RuntimeException if restaurant not found
     * @throws RuntimeException if user is not the restaurant owner
     */
    @Transactional
    @CacheEvict(value = "menu" , key = "#restaurantId")
    public MenuItem addMenuItem(Long restaurantId, MenuItemRequest request, String userEmail) {
        Restaurant restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        // Security Check: Does this user own this restaurant?
        if (!restaurant.getOwner().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not the owner of this restaurant");
        }

        MenuItem menuItem = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .availableQuantity(request.getAvailableQuantity())
                .isAvailable(true)
                .build();

        return menuItemRepo.save(menuItem);
    }

    /**
     * Retrieves all restaurants.
     *
     * <p>
     * This method is typically used for:
     * <ul>
     *   <li>User home feed</li>
     *   <li>Restaurant discovery</li>
     * </ul>
     *
     * @return list of all restaurants
     */
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepo.findAll();
    }

    /**
     * Retrieves all menu items for a specific restaurant.
     *
     * @param restaurantId ID of the restaurant
     * @return list of menu items belonging to the restaurant
     */
    // CACHE: This reads from Redis first!
    @Cacheable(value = "menu", key = "#restaurantId")
    public List<MenuItem> getMenu(Long restaurantId) {
        return menuItemRepo.findByRestaurantId(restaurantId);
    }
}
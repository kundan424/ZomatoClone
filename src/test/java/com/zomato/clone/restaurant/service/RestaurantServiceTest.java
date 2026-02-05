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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepo;

    @Mock
    private MenuItemRepository menuItemRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private RestaurantService restaurantService;

    private User restaurantOwner;
    private User regularUser;
    private Restaurant restaurant;
    private CreateRestaurantRequest createRestaurantRequest;
    private MenuItemRequest menuItemRequest;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        restaurantOwner = User.builder()
                .id(1L)
                .name("Restaurant Owner")
                .email("owner@example.com")
                .password("password")
                .role(UserRole.RESTAURANT)
                .build();

        regularUser = User.builder()
                .id(2L)
                .name("Regular User")
                .email("user@example.com")
                .password("password")
                .role(UserRole.USER)
                .build();

        restaurant = Restaurant.builder()
                .id(1L)
                .name("Test Restaurant")
                .description("A great restaurant")
                .address("123 Test St")
                .owner(restaurantOwner)
                .isOpen(true)
                .build();

        createRestaurantRequest = new CreateRestaurantRequest();
        createRestaurantRequest.setName("Test Restaurant");
        createRestaurantRequest.setDescription("A great restaurant");
        createRestaurantRequest.setAddress("123 Test St");

        menuItemRequest = new MenuItemRequest();
        menuItemRequest.setName("Pizza");
        menuItemRequest.setDescription("Delicious pizza");
        menuItemRequest.setPrice(new BigDecimal("12.99"));
        menuItemRequest.setAvailableQuantity(10);

        menuItem = MenuItem.builder()
                .id(1L)
                .name("Pizza")
                .description("Delicious pizza")
                .price(new BigDecimal("12.99"))
                .availableQuantity(10)
                .restaurant(restaurant)
                .isAvailable(true)
                .build();
    }

    @Test
    void createRestaurant_Success() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(restaurantOwner));
        when(restaurantRepo.save(any(Restaurant.class))).thenReturn(restaurant);

        // Act
        Restaurant result = restaurantService.createRestaurant(createRestaurantRequest, "owner@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("Test Restaurant", result.getName());
        assertEquals("A great restaurant", result.getDescription());
        verify(userRepo).findByEmail("owner@example.com");
        verify(restaurantRepo).save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            restaurantService.createRestaurant(createRestaurantRequest, "nonexistent@example.com");
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepo).findByEmail("nonexistent@example.com");
        verify(restaurantRepo, never()).save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_NonRestaurantRole_ThrowsException() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(regularUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            restaurantService.createRestaurant(createRestaurantRequest, "user@example.com");
        });

        assertEquals("Only Restaurant Owners can create restaurants", exception.getMessage());
        verify(userRepo).findByEmail("user@example.com");
        verify(restaurantRepo, never()).save(any(Restaurant.class));
    }

    @Test
    void addMenuItem_Success() {
        // Arrange
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepo.save(any(MenuItem.class))).thenReturn(menuItem);

        // Act
        MenuItem result = restaurantService.addMenuItem(1L, menuItemRequest, "owner@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("Pizza", result.getName());
        assertEquals(new BigDecimal("12.99"), result.getPrice());
        verify(restaurantRepo).findById(1L);
        verify(menuItemRepo).save(any(MenuItem.class));
    }

    @Test
    void addMenuItem_RestaurantNotFound_ThrowsException() {
        // Arrange
        when(restaurantRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            restaurantService.addMenuItem(1L, menuItemRequest, "owner@example.com");
        });

        assertEquals("Restaurant not found", exception.getMessage());
        verify(restaurantRepo).findById(1L);
        verify(menuItemRepo, never()).save(any(MenuItem.class));
    }

    @Test
    void addMenuItem_NotOwner_ThrowsException() {
        // Arrange
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            restaurantService.addMenuItem(1L, menuItemRequest, "different@example.com");
        });

        assertEquals("You are not the owner of this restaurant", exception.getMessage());
        verify(restaurantRepo).findById(1L);
        verify(menuItemRepo, never()).save(any(MenuItem.class));
    }

    @Test
    void getAllRestaurants_Success() {
        // Arrange
        List<Restaurant> restaurants = Arrays.asList(restaurant);
        when(restaurantRepo.findAll()).thenReturn(restaurants);

        // Act
        List<Restaurant> result = restaurantService.getAllRestaurants();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Restaurant", result.get(0).getName());
        verify(restaurantRepo).findAll();
    }

    @Test
    void getMenu_Success() {
        // Arrange
        List<MenuItem> menuItems = Arrays.asList(menuItem);
        when(menuItemRepo.findByRestaurantId(1L)).thenReturn(menuItems);

        // Act
        List<MenuItem> result = restaurantService.getMenu(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Pizza", result.get(0).getName());
        verify(menuItemRepo).findByRestaurantId(1L);
    }
}

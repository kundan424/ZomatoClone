package com.zomato.clone.order.service;

import com.zomato.clone.order.dto.PlaceOrderRequest;
import com.zomato.clone.order.dto.OrderResponse;
import com.zomato.clone.order.entity.Order;
import com.zomato.clone.order.repository.OrderRepository;
import com.zomato.clone.restaurant.entity.MenuItem;
import com.zomato.clone.restaurant.entity.Restaurant;
import com.zomato.clone.restaurant.repository.MenuItemRepository;
import com.zomato.clone.restaurant.repository.RestaurantRepository;
import com.zomato.clone.user.entity.User;
import com.zomato.clone.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initializes Mocks
class OrderServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private UserRepository userRepo;
    @Mock private RestaurantRepository restaurantRepo;
    @Mock private MenuItemRepository menuItemRepo;

    @InjectMocks
    private OrderService orderService; // Injects mocks into service

    @Test
    void shouldPlaceOrderSuccessfully() {
        // 1. ARRAGE (Prepare Mock Data)
        String userEmail = "test@example.com";
        User mockUser = User.builder().email(userEmail).id(1L).build();
        Restaurant mockRestaurant = Restaurant.builder().id(10L).name("Pizza Hut").build();
        MenuItem mockItem = MenuItem.builder()
                .id(100L)
                .price(BigDecimal.valueOf(10.00))
                .availableQuantity(5)
                .build();

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setRestaurantId(10L);
        PlaceOrderRequest.OrderItemRequest itemReq = new PlaceOrderRequest.OrderItemRequest();
        itemReq.setMenuItemId(100L);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        // Define Mock Behavior
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        when(restaurantRepo.findById(10L)).thenReturn(Optional.of(mockRestaurant));
        when(menuItemRepo.findById(100L)).thenReturn(Optional.of(mockItem));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(555L); // Simulate DB ID generation
            return saved;
        });

        // 2. ACT (Run the method)
        OrderResponse response = orderService.placeOrder(request, userEmail);

        // 3. ASSERT (Verify results)
        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(20.00), response.getTotalAmount()); // 2 * 10.00
        assertEquals("Pizza Hut", response.getRestaurantName());

        // Verify Inventory Deducted (5 - 2 = 3)
        assertEquals(3, mockItem.getAvailableQuantity());
        verify(menuItemRepo, times(1)).save(mockItem);
    }
}
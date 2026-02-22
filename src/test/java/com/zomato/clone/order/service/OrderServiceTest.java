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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled("Temporarily disabled ")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private UserRepository userRepo;
    @Mock private RestaurantRepository restaurantRepo;
    @Mock private MenuItemRepository menuItemRepo;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldPlaceOrderSuccessfully() {

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

        PlaceOrderRequest.OrderItemRequest itemReq =
                new PlaceOrderRequest.OrderItemRequest();
        itemReq.setMenuItemId(100L);
        itemReq.setQuantity(2);

        request.setItems(List.of(itemReq));

        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any()))
                .thenReturn(true);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Mock DB
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        when(restaurantRepo.findById(10L)).thenReturn(Optional.of(mockRestaurant));
        when(menuItemRepo.findById(100L)).thenReturn(Optional.of(mockItem));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(555L);
            return saved;
        });

        // ACT
        OrderResponse response = orderService.placeOrder(request, userEmail);

        // ASSERT
        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(20.00), response.getTotalAmount());
        assertEquals("Pizza Hut", response.getRestaurantName());
        assertEquals(3, mockItem.getAvailableQuantity());

        verify(menuItemRepo, times(1)).save(mockItem);
    }
}
//This is the most complex part of V1.
// It validates the request, calculates the bill, and updates inventory.

package com.zomato.clone.order.service;

import com.zomato.clone.enums.OrderStatus;
import com.zomato.clone.order.dto.OrderResponse;
import com.zomato.clone.order.dto.PlaceOrderRequest;
import com.zomato.clone.order.entity.Order;
import com.zomato.clone.order.entity.OrderItem;
import com.zomato.clone.order.repository.OrderRepository;
import com.zomato.clone.restaurant.entity.MenuItem;
import com.zomato.clone.restaurant.entity.Restaurant;
import com.zomato.clone.restaurant.repository.MenuItemRepository;
import com.zomato.clone.restaurant.repository.RestaurantRepository;
import com.zomato.clone.user.entity.User;
import com.zomato.clone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;
    private final MenuItemRepository menuItemRepo;


    private final StringRedisTemplate redisTemplate;

    @Cacheable(value = "userOrders", key = "#userEmail")
    public OrderResponse placeOrder(PlaceOrderRequest request, String userEmail) {

        // fetch user
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        // fetch restaurant
        Restaurant restaurant = restaurantRepo.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        // 3. Create Order Object (Initial State)
        Order order = Order.builder()
                .user(user)
                .restaurant(restaurant)
                .status(OrderStatus.CREATED)
                .orderItems(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // process each data
        for (PlaceOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Long menuItemId = itemRequest.getMenuItemId();
            String lockKey = "lock:item:" + menuItemId;

            // 1. TRY TO ACQUIRE LOCK
            // setIfAbsent is ATOMIC. Only one thread can succeed.

            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    "LOCKED",
                    Duration.ofSeconds(10) // Lock expires in 10s (Safety net if server crashes)
            );

            if (Boolean.FALSE.equals(acquired)) {
                throw new RuntimeException("Item is currently being processed by another user. Please try again.");
            }

            try {
                MenuItem menuItem = menuItemRepo.findById(menuItemId)
                        .orElseThrow(() -> new RuntimeException("Menu Item not found"));

                // check Availability
                if (menuItem.getAvailableQuantity() < itemRequest.getQuantity()) {
                    throw new RuntimeException("item " + menuItem.getName() + " is out of stock");
                }

                // Deduct Inventory (Simple Logic for V1 - V2 will use Redis Locking)
                menuItem.setAvailableQuantity(menuItem.getAvailableQuantity() - itemRequest.getQuantity());
                menuItemRepo.save(menuItem);

                // create order item
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .menuItem(menuItem)
                        .quantity(itemRequest.getQuantity())
                        .price(menuItem.getPrice())
                        .build();

                order.getOrderItems().add(orderItem);

                // calculate the total
                BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

            } finally {
                // 3. RELEASE LOCK (Always happen, even if error)
                redisTemplate.delete(lockKey);
            }
        }

        // save order
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepo.save(order);

        // return response
        return OrderResponse.builder()
                .orderId(savedOrder.getId())
                .restaurantName(restaurant.getName())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus())
                .placedAt(savedOrder.getCreatedAt())
                .build();

    }


    @Cacheable(value = "userOrders", key = "#userEmail")
    public List<OrderResponse> getUserOrders(String userEmail) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow();

        return orderRepo.findByUserId(user.getId()).stream()
                .map(order -> OrderResponse.builder()
                        .orderId(order.getId())
                        .restaurantName(order.getRestaurant().getName())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .placedAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

}

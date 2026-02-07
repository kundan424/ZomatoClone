package com.zomato.clone.order.repository;

import com.zomato.clone.order.dto.OrderResponse;
import com.zomato.clone.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository <Order, Long> {

    // Customer History: "My Orders"
    List<Order> findByUserId (Long userId);
}

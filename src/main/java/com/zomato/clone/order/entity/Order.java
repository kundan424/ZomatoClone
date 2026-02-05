package com.zomato.clone.order.entity;

import com.zomato.clone.enums.OrderStatus;
import com.zomato.clone.restaurant.entity.Restaurant;
import com.zomato.clone.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order represents a complete food order placed by a user.
 * It is the aggregate root of the Order module and
 * connects User, Restaurant, and OrderItems.
 */
@Entity
@Table(name = "orders") // table for storing the order
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // primary key

    /**
     * User who placed the order
     * Many order can placed by single user
     * LAZY fetch avoids loading user details unless required.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Restaurant from which order is placed
     * Used to query all orders for a specific restaurant
     */
    // Link to Restaurant Module (For querying "Orders for Restaurant X")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // CREATED, PAYMENT_PENDING, PAID..

    /**
     * Items included in this order.
     * One Order can have multiple OrderItems.
     * Cascade ALL ensures items are persisted/removed with the order.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // Timestamp when the order was created., automatically set by hibernate.

}

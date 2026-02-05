package com.zomato.clone.user.entity;

import com.zomato.clone.enums.UserRole;
import com.zomato.clone.order.entity.Order;
import com.zomato.clone.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // primary key

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phone; // Added for delivery contact

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // USER, RESTAURANT, ADMIN

    // relationships

    // One User can own multiple Restaurants (Restaurant Module)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Restaurant> ownedRestaurants;

    // One User can have multiple Orders (Order Module)
    @OneToMany(mappedBy = "user")
    private List<Order> orders;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

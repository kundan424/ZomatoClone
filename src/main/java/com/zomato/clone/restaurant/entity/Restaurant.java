package com.zomato.clone.restaurant.entity;

import com.zomato.clone.order.entity.Order;
import com.zomato.clone.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Restaurant represents a food outlet registered on the platform.
 * It is owned by a User and contains menu items and received orders.
 */

@Entity
@Table(name = "restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // primary key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // The user who owns this restaurant, Many restaurants can belong to one owner.

    @Column(nullable = false)
    private String name;

    private String description;
    private String address;

    @Column(nullable = false)
    private Boolean isOpen = true;

    // relationships
    // Internal to Restaurant Module (Cascade ALL: deleting restaurant deletes menu)
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuItem> menuItems;

    // Orders received by this restaurant.
    @OneToMany(mappedBy = "restaurant")
    private List<Order> orders;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}

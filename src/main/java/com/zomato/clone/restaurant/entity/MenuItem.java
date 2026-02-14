package com.zomato.clone.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MenuItem represents a single food item offered by a Restaurant.
 * It contains pricing, availability, and inventory-related data.
 */
@Entity
@Table(name = "menu_items") // table for storing restaurant menu items
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //primary key

    /**
     * Restaurant that owns this menu item.
     * Many menu items belong to one restaurant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @JsonIgnore
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    private String description; // description shown to users

    // Production-Grade: Use BigDecimal for money
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;


    /**
     * Production-Grade: Inventory Count (for concurrency locking later)
     * Available stock for this menu item.
     * used for inventory control and future concurrency handling
     */
    @Column(nullable = false)
    private Integer availableQuantity; //For inventory tracking


    //Flag indicating whether the item is currently orderable.
    @Column(nullable = false)
    private Boolean isAvailable = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}

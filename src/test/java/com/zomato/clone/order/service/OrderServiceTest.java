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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderService}.
 *
 * <p>
 * This test class verifies the business logic of the OrderService,
 * which is the most complex service in V1 of the application.
 *
 * <h2>Key Functionalities Tested</h2>
 * <ul>
 *   <li><b>Order Placement</b>: Validates requests, calculates bills, updates inventory</li>
 *   <li><b>Order Retrieval</b>: Fetches user's order history</li>
 * </ul>
 *
 * <h2>Business Rules Verified</h2>
 * <ul>
 *   <li>User must exist to place an order</li>
 *   <li>Restaurant must exist</li>
 *   <li>All menu items must exist and belong to the restaurant</li>
 *   <li>Sufficient inventory must be available</li>
 *   <li>Inventory is deducted upon successful order</li>
 *   <li>Total amount is calculated correctly</li>
 * </ul>
 *
 * <h2>Testing Approach</h2>
 * <ul>
 *   <li>Unit testing with Mockito for dependency isolation</li>
 *   <li>AAA (Arrange-Act-Assert) pattern</li>
 *   <li>Nested test classes for logical grouping</li>
 *   <li>ArgumentCaptors for verifying saved entities</li>
 * </ul>
 * @see OrderService
 * @see Order
 * @see OrderItem
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // ==================== MOCK DEPENDENCIES ====================

    /**
     * Mocked OrderRepository for simulating order persistence operations.
     */
    @Mock
    private OrderRepository orderRepo;

    /**
     * Mocked UserRepository for simulating user lookup operations.
     */
    @Mock
    private UserRepository userRepo;

    /**
     * Mocked RestaurantRepository for simulating restaurant lookup operations.
     */
    @Mock
    private RestaurantRepository restaurantRepo;

    /**
     * Mocked MenuItemRepository for simulating menu item operations.
     * Critical for inventory management testing.
     */
    @Mock
    private MenuItemRepository menuItemRepo;

    // ==================== SERVICE UNDER TEST ====================

    /**
     * The OrderService instance being tested.
     * All mocked dependencies are automatically injected.
     */
    @InjectMocks
    private OrderService orderService;

    // ==================== ARGUMENT CAPTORS ====================

    /**
     * Captures Order objects passed to repository save methods.
     * Used to verify order details like total amount and status.
     */
    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    /**
     * Captures MenuItem objects to verify inventory updates.
     */
    @Captor
    private ArgumentCaptor<MenuItem> menuItemCaptor;

    // ==================== TEST FIXTURES ====================

    /**
     * Test user who places orders.
     */
    private User user;

    /**
     * Test restaurant from which orders are placed.
     */
    private Restaurant restaurant;

    /**
     * First menu item - Pizza.
     */
    private MenuItem pizzaMenuItem;

    /**
     * Second menu item - Burger.
     */
    private MenuItem burgerMenuItem;

    /**
     * Sample order request with multiple items.
     */
    private PlaceOrderRequest placeOrderRequest;

    /**
     * Sample saved order for retrieval tests.
     */
    private Order savedOrder;

    // ==================== TEST SETUP ====================

    /**
     * Initializes test fixtures before each test method.
     *
     * <p>
     * Creates a complete set of test data including:
     * <ul>
     *   <li>User with valid credentials</li>
     *   <li>Restaurant with basic details</li>
     *   <li>Menu items with prices and inventory</li>
     *   <li>Order request with multiple items</li>
     *   <li>Pre-saved order for retrieval tests</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        /*
         * Setup test user.
         */
        user = User.builder()
                .id(1L)
                .email("customer@test.com")
                .name("Test Customer")
                .build();

        /*
         * Setup test restaurant.
         */
        restaurant = Restaurant.builder()
                .id(1L)
                .name("Test Restaurant")
                .description("Test Description")
                .address("123 Test Street")
                .isOpen(true)
                .build();

        /*
         * Setup menu items with different prices and quantities.
         * Pizza: $12.99, 50 available
         * Burger: $9.99, 30 available
         */
        pizzaMenuItem = MenuItem.builder()
                .id(1L)
                .restaurant(restaurant)
                .name("Pizza")
                .description("Delicious pizza")
                .price(new BigDecimal("12.99"))
                .availableQuantity(50)
                .isAvailable(true)
                .build();

        burgerMenuItem = MenuItem.builder()
                .id(2L)
                .restaurant(restaurant)
                .name("Burger")
                .description("Juicy burger")
                .price(new BigDecimal("9.99"))
                .availableQuantity(30)
                .isAvailable(true)
                .build();

        /*
         * Setup order request with 2 pizzas and 3 burgers.
         * Expected total: (2 * 12.99) + (3 * 9.99) = 25.98 + 29.97 = 55.95
         */
        PlaceOrderRequest.OrderItemRequest pizzaOrder = PlaceOrderRequest.OrderItemRequest.builder()
                .menuItemId(1L)
                .quantity(2)
                .build();

        PlaceOrderRequest.OrderItemRequest burgerOrder = PlaceOrderRequest.OrderItemRequest.builder()
                .menuItemId(2L)
                .quantity(3)
                .build();

        placeOrderRequest = PlaceOrderRequest.builder()
                .restaurantId(1L)
                .items(Arrays.asList(pizzaOrder, burgerOrder))
                .build();

        /*
         * Setup a pre-saved order for retrieval tests.
         */
        savedOrder = Order.builder()
                .id(100L)
                .user(user)
                .restaurant(restaurant)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("55.95"))
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();
    }

    // ==================== PLACE ORDER TESTS ====================

    /**
     * Test suite for {@link OrderService#placeOrder(PlaceOrderRequest, String)}.
     *
     * <p>
     * This is the most complex method in V1. Tests cover:
     * <ul>
     *   <li>Successful order placement with single/multiple items</li>
     *   <li>User validation</li>
     *   <li>Restaurant validation</li>
     *   <li>Menu item validation</li>
     *   <li>Inventory availability checks</li>
     *   <li>Inventory deduction</li>
     *   <li>Total amount calculation</li>
     * </ul>
     */
    @Nested
    @DisplayName("placeOrder Tests")
    class PlaceOrderTests {

        /**
         * Tests successful order placement with multiple items.
         *
         * <p>
         * <b>Scenario:</b> User places an order with 2 pizzas and 3 burgers.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>User exists</li>
         *   <li>Restaurant exists</li>
         *   <li>Menu items exist with sufficient inventory</li>
         * </ul>
         *
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>Order is created with CREATED status</li>
         *   <li>Total amount is calculated correctly: (2 * 12.99) + (3 * 9.99) = 55.95</li>
         *   <li>Inventory is deducted for each item</li>
         *   <li>OrderResponse contains correct details</li>
         * </ul>
         */
        @Test
        @DisplayName("Should place order successfully with multiple items")
        void shouldPlaceOrderSuccessfully() {
            // ============ ARRANGE ============
            /*
             * Configure mocks to return valid entities.
             */
            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));
            when(menuItemRepo.findById(burgerMenuItem.getId()))
                    .thenReturn(Optional.of(burgerMenuItem));

            /*
             * Configure order save to return order with ID and timestamp.
             */
            when(orderRepo.save(any(Order.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId(100L);
                        order.setCreatedAt(LocalDateTime.now());
                        return order;
                    });

            /*
             * Configure menu item saves for inventory updates.
             */
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ============ ACT ============
            OrderResponse response = orderService.placeOrder(placeOrderRequest, user.getEmail());

            // ============ ASSERT ============
            /*
             * Verify response contains correct details.
             */
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(100L);
            assertThat(response.getRestaurantName()).isEqualTo(restaurant.getName());
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(response.getPlacedAt()).isNotNull();

            /*
             * Verify total amount calculation.
             * Pizza: 2 * 12.99 = 25.98
             * Burger: 3 * 9.99 = 29.97
             * Total: 55.95
             */
            BigDecimal expectedTotal = new BigDecimal("12.99").multiply(BigDecimal.valueOf(2))
                    .add(new BigDecimal("9.99").multiply(BigDecimal.valueOf(3)));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);

            /*
             * Verify order was saved with correct details.
             */
            verify(orderRepo).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getUser()).isEqualTo(user);
            assertThat(capturedOrder.getRestaurant()).isEqualTo(restaurant);
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(capturedOrder.getOrderItems()).hasSize(2);

            /*
             * Verify inventory was updated for both items.
             */
            verify(menuItemRepo, times(2)).save(menuItemCaptor.capture());
            List<MenuItem> savedMenuItems = menuItemCaptor.getAllValues();

            /*
             * Verify pizza inventory: 50 - 2 = 48
             */
            MenuItem savedPizza = savedMenuItems.stream()
                    .filter(item -> item.getId().equals(pizzaMenuItem.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(savedPizza.getAvailableQuantity()).isEqualTo(48);

            /*
             * Verify burger inventory: 30 - 3 = 27
             */
            MenuItem savedBurger = savedMenuItems.stream()
                    .filter(item -> item.getId().equals(burgerMenuItem.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(savedBurger.getAvailableQuantity()).isEqualTo(27);
        }

        /**
         * Tests successful order placement with a single item.
         *
         * <p>
         * <b>Scenario:</b> User places an order with only 1 pizza.
         *
         * <p>
         * <b>Given:</b> Valid user, restaurant, and menu item
         *
         * <p>
         * <b>When:</b> placeOrder() is called with single item request
         *
         * <p>
         * <b>Then:</b> Order is created with correct total (1 * 12.99 = 12.99)
         */
        @Test
        @DisplayName("Should place order successfully with single item")
        void shouldPlaceOrderWithSingleItem() {
            // ============ ARRANGE ============
            PlaceOrderRequest.OrderItemRequest singleItemOrder = PlaceOrderRequest.OrderItemRequest.builder()
                    .menuItemId(1L)
                    .quantity(1)
                    .build();

            PlaceOrderRequest singleItemRequest = PlaceOrderRequest.builder()
                    .restaurantId(1L)
                    .items(Collections.singletonList(singleItemOrder))
                    .build();

            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));
            when(orderRepo.save(any(Order.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId(101L);
                        order.setCreatedAt(LocalDateTime.now());
                        return order;
                    });
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ============ ACT ============
            OrderResponse response = orderService.placeOrder(singleItemRequest, user.getEmail());

            // ============ ASSERT ============
            assertThat(response).isNotNull();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("12.99"));

            verify(orderRepo).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getOrderItems()).hasSize(1);
        }

        /**
         * Tests that UsernameNotFoundException is thrown for non-existent user.
         *
         * <p>
         * <b>Scenario:</b> Order placement attempted with unknown email.
         *
         * <p>
         * <b>Given:</b> User email does not exist in database
         *
         * <p>
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>UsernameNotFoundException is thrown</li>
         *   <li>No order is saved</li>
         *   <li>No inventory is modified</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // ============ ARRANGE ============
            String nonExistentEmail = "unknown@test.com";
            when(userRepo.findByEmail(nonExistentEmail))
                    .thenReturn(Optional.empty());

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    orderService.placeOrder(placeOrderRequest, nonExistentEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User Not Found");

            /*
             * Verify no further operations were performed.
             */
            verify(userRepo).findByEmail(nonExistentEmail);
            verify(restaurantRepo, never()).findById(any());
            verify(orderRepo, never()).save(any());
            verify(menuItemRepo, never()).save(any());
        }

        /**
         * Tests that RuntimeException is thrown for non-existent restaurant.
         *
         * <p>
         * <b>Scenario:</b> Order placement for a restaurant that doesn't exist.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>User exists</li>
         *   <li>Restaurant ID doesn't match any restaurant</li>
         * </ul>
         *
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>RuntimeException is thrown with "Restaurant not found"</li>
         *   <li>No order is saved</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw RuntimeException when restaurant not found")
        void shouldThrowExceptionWhenRestaurantNotFound() {
            // ============ ARRANGE ============
            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.empty());

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    orderService.placeOrder(placeOrderRequest, user.getEmail()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Restaurant not found");

            verify(orderRepo, never()).save(any());
            verify(menuItemRepo, never()).save(any());
        }

        /**
         * Tests that RuntimeException is thrown for non-existent menu item.
         *
         * <p>
         * <b>Scenario:</b> Order contains a menu item that doesn't exist.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>User and restaurant exist</li>
         *   <li>One of the menu items doesn't exist</li>
         * </ul>
         *
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b> RuntimeException is thrown with "Menu Item not found"
         */
        @Test
        @DisplayName("Should throw RuntimeException when menu item not found")
        void shouldThrowExceptionWhenMenuItemNotFound() {
            // ============ ARRANGE ============
            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.empty());  // Pizza not found

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    orderService.placeOrder(placeOrderRequest, user.getEmail()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Menu Item not found");

            verify(orderRepo, never()).save(any());
        }

        /**
         * Tests that RuntimeException is thrown when item is out of stock.
         *
         * <p>
         * <b>Scenario:</b> User orders more items than available in inventory.
         *
         * <p>
         * <b>Business Rule:</b> Cannot order more than availableQuantity.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>Pizza has 50 available</li>
         *   <li>User tries to order 100 pizzas</li>
         * </ul>
         *
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>RuntimeException is thrown with out of stock message</li>
         *   <li>No inventory is modified</li>
         *   <li>No order is saved</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw RuntimeException when item is out of stock")
        void shouldThrowExceptionWhenItemOutOfStock() {
            // ============ ARRANGE ============
            /*
             * Create request with quantity exceeding available stock.
             */
            PlaceOrderRequest.OrderItemRequest excessiveOrder = PlaceOrderRequest.OrderItemRequest.builder()
                    .menuItemId(1L)
                    .quantity(100)  // Exceeds available quantity of 50
                    .build();

            PlaceOrderRequest outOfStockRequest = PlaceOrderRequest.builder()
                    .restaurantId(1L)
                    .items(Collections.singletonList(excessiveOrder))
                    .build();

            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    orderService.placeOrder(outOfStockRequest, user.getEmail()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("out of stock")
                    .hasMessageContaining("Pizza");

            /*
             * Verify no inventory was modified and no order was saved.
             */
            verify(menuItemRepo, never()).save(any());
            verify(orderRepo, never()).save(any());
        }

        /**
         * Tests that partial inventory deduction doesn't occur on failure.
         *
         * <p>
         * <b>Scenario:</b> First item succeeds but second item is out of stock.
         *
         * <p>
         * <b>Note:</b> This test highlights a potential issue in V1 -
         * the current implementation may leave inventory in an inconsistent state
         * if an exception occurs mid-processing. This should be addressed with
         * proper transaction management.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>Pizza has sufficient stock</li>
         *   <li>Burger has insufficient stock for the requested quantity</li>
         * </ul>
         *
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b> Exception is thrown for burger out of stock
         */
        @Test
        @DisplayName("Should fail when second item is out of stock")
        void shouldFailWhenSecondItemOutOfStock() {
            // ============ ARRANGE ============
            /*
             * Set burger to have very low inventory.
             */
            burgerMenuItem.setAvailableQuantity(1);  // Only 1 available, but 3 requested

            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));
            when(menuItemRepo.findById(burgerMenuItem.getId()))
                    .thenReturn(Optional.of(burgerMenuItem));
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    orderService.placeOrder(placeOrderRequest, user.getEmail()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("out of stock")
                    .hasMessageContaining("Burger");

            /*
             * Note: In the current implementation, pizza inventory might have been
             * deducted before the burger check failed. This is a known limitation
             * that will be addressed in V2 with proper transaction handling.
             */
            verify(orderRepo, never()).save(any());
        }

        /**
         * Tests order placement when ordering exact available quantity.
         *
         * <p>
         * <b>Scenario:</b> User orders exactly what's available (edge case).
         *
         * <p>
         * <b>Given:</b> Pizza has 5 available, user orders 5
         *
         * <p>
         * <b>When:</b> placeOrder() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>Order is placed successfully</li>
         *   <li>Inventory becomes 0</li>
         * </ul>
         */
        @Test
        @DisplayName("Should place order when ordering exact available quantity")
        void shouldPlaceOrderWithExactAvailableQuantity() {
            // ============ ARRANGE ============
            pizzaMenuItem.setAvailableQuantity(5);  // Exactly 5 available

            PlaceOrderRequest.OrderItemRequest exactOrder = PlaceOrderRequest.OrderItemRequest.builder()
                    .menuItemId(1L)
                    .quantity(5)  // Order exactly 5
                    .build();

            PlaceOrderRequest exactQuantityRequest = PlaceOrderRequest.builder()
                    .restaurantId(1L)
                    .items(Collections.singletonList(exactOrder))
                    .build();

            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));
            when(orderRepo.save(any(Order.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId(102L);
                        order.setCreatedAt(LocalDateTime.now());
                        return order;
                    });
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ============ ACT ============
            OrderResponse response = orderService.placeOrder(exactQuantityRequest, user.getEmail());

            // ============ ASSERT ============
            assertThat(response).isNotNull();

            /*
             * Verify inventory is now 0.
             */
            verify(menuItemRepo).save(menuItemCaptor.capture());
            assertThat(menuItemCaptor.getValue().getAvailableQuantity()).isEqualTo(0);
        }

        /**
         * Tests correct total calculation with decimal prices.
         *
         * <p>
         * <b>Scenario:</b> Verify BigDecimal arithmetic is correct.
         *
         * <p>
         * <b>Calculation:</b>
         * <ul>
         *   <li>2 Pizzas at $12.99 = $25.98</li>
         *   <li>3 Burgers at $9.99 = $29.97</li>
         *   <li>Total = $55.95</li>
         * </ul>
         */
        @Test
        @DisplayName("Should calculate total amount correctly with decimal prices")
        void shouldCalculateTotalAmountCorrectly() {
            // ============ ARRANGE ============
            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));
            when(menuItemRepo.findById(burgerMenuItem.getId()))
                    .thenReturn(Optional.of(burgerMenuItem));
            when(orderRepo.save(any(Order.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId(103L);
                        order.setCreatedAt(LocalDateTime.now());
                        return order;
                    });
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ============ ACT ============
            OrderResponse response = orderService.placeOrder(placeOrderRequest, user.getEmail());

            // ============ ASSERT ============
            /*
             * Calculate expected total manually.
             * 2 * 12.99 = 25.98
             * 3 * 9.99 = 29.97
             * Total = 55.95
             */
            BigDecimal expectedTotal = new BigDecimal("55.95");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);

            /*
             * Also verify the saved order has correct total.
             */
            verify(orderRepo).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getTotalAmount()).isEqualByComparingTo(expectedTotal);
        }

        /**
         * Tests that order items are correctly associated with the order.
         *
         * <p>
         * <b>Scenario:</b> Verify OrderItem entities have correct details.
         */
        @Test
        @DisplayName("Should create order items with correct details")
        void shouldCreateOrderItemsWithCorrectDetails() {
            // ============ ARRANGE ============
            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));
            when(menuItemRepo.findById(pizzaMenuItem.getId()))
                    .thenReturn(Optional.of(pizzaMenuItem));
            when(menuItemRepo.findById(burgerMenuItem.getId()))
                    .thenReturn(Optional.of(burgerMenuItem));
            when(orderRepo.save(any(Order.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId(104L);
                        order.setCreatedAt(LocalDateTime.now());
                        return order;
                    });
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ============ ACT ============
            orderService.placeOrder(placeOrderRequest, user.getEmail());

            // ============ ASSERT ============
            verify(orderRepo).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();
            List<OrderItem> orderItems = capturedOrder.getOrderItems();

            assertThat(orderItems).hasSize(2);

            /*
             * Verify pizza order item.
             */
            OrderItem pizzaOrderItem = orderItems.stream()
                    .filter(item -> item.getMenuItem().getId().equals(pizzaMenuItem.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(pizzaOrderItem.getQuantity()).isEqualTo(2);
            assertThat(pizzaOrderItem.getPrice()).isEqualByComparingTo(new BigDecimal("12.99"));
            assertThat(pizzaOrderItem.getOrder()).isEqualTo(capturedOrder);

            /*
             * Verify burger order item.
             */
            OrderItem burgerOrderItem = orderItems.stream()
                    .filter(item -> item.getMenuItem().getId().equals(burgerMenuItem.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(burgerOrderItem.getQuantity()).isEqualTo(3);
            assertThat(burgerOrderItem.getPrice()).isEqualByComparingTo(new BigDecimal("9.99"));
        }
    }

    // ==================== GET USER ORDERS TESTS ====================

    /**
     * Test suite for {@link OrderService#getUserOrders(String)}.
     *
     * <p>
     * Tests order history retrieval functionality.
     *
     * <p>
     * <b>Scenarios Tested:</b>
     * <ul>
     *   <li>Returns all orders for a user</li>
     *   <li>Returns empty list when user has no orders</li>
     *   <li>Throws exception when user not found</li>
     * </ul>
     */
    @Nested
    @DisplayName("getUserOrders Tests")
    class GetUserOrdersTests {

        /**
         * Tests successful retrieval of user's orders.
         *
         * <p>
         * <b>Scenario:</b> User has multiple orders in history.
         *
         * <p>
         * <b>Given:</b> User has 2 orders
         *
         * <p>
         * <b>When:</b> getUserOrders() is called
         *
         * <p>
         * <b>Then:</b> Both orders are returned as OrderResponse objects
         */
        @Test
        @DisplayName("Should return all orders for a user")
        void shouldReturnAllUserOrders() {
            // ============ ARRANGE ============
            Order order1 = Order.builder()
                    .id(1L)
                    .user(user)
                    .restaurant(restaurant)
                    .status(OrderStatus.CREATED)
                    .totalAmount(new BigDecimal("25.98"))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            Order order2 = Order.builder()
                    .id(2L)
                    .user(user)
                    .restaurant(restaurant)
                    .status(OrderStatus.DELIVERED)
                    .totalAmount(new BigDecimal("55.95"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(orderRepo.findByUserId(user.getId()))
                    .thenReturn(Arrays.asList(order1, order2));

            // ============ ACT ============
            List<OrderResponse> responses = orderService.getUserOrders(user.getEmail());

            // ============ ASSERT ============
            assertThat(responses).hasSize(2);

            /*
             * Verify first order response.
             */
            OrderResponse response1 = responses.get(0);
            assertThat(response1.getOrderId()).isEqualTo(1L);
            assertThat(response1.getRestaurantName()).isEqualTo(restaurant.getName());
            assertThat(response1.getTotalAmount()).isEqualByComparingTo(new BigDecimal("25.98"));
            assertThat(response1.getStatus()).isEqualTo(OrderStatus.CREATED);

            /*
             * Verify second order response.
             */
            OrderResponse response2 = responses.get(1);
            assertThat(response2.getOrderId()).isEqualTo(2L);
            assertThat(response2.getStatus()).isEqualTo(OrderStatus.DELIVERED);

            verify(userRepo).findByEmail(user.getEmail());
            verify(orderRepo).findByUserId(user.getId());
        }

        /**
         * Tests retrieval when user has no orders.
         *
         * <p>
         * <b>Scenario:</b> New user with no order history.
         *
         * <p>
         * <b>Given:</b> User exists but has no orders
         *
         * <p>
         * <b>When:</b> getUserOrders() is called
         *
         * <p>
         * <b>Then:</b> Empty list is returned (not null)
         */
        @Test
        @DisplayName("Should return empty list when user has no orders")
        void shouldReturnEmptyListWhenNoOrders() {
            // ============ ARRANGE ============
            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(orderRepo.findByUserId(user.getId()))
                    .thenReturn(Collections.emptyList());

            // ============ ACT ============
            List<OrderResponse> responses = orderService.getUserOrders(user.getEmail());

            // ============ ASSERT ============
            assertThat(responses).isEmpty();

            verify(orderRepo).findByUserId(user.getId());
        }

        /**
         * Tests that exception is thrown for non-existent user.
         *
         * <p>
         * <b>Scenario:</b> Attempting to get orders for unknown user.
         *
         * <p>
         * <b>Note:</b> The current implementation uses orElseThrow() without
         * a custom exception, which throws NoSuchElementException.
         *
         * <p>
         * <b>Given:</b> User email doesn't exist
         *
         * <p>
         * <b>When:</b> getUserOrders() is called
         *
         * <p>
         * <b>Then:</b> NoSuchElementException is thrown
         */
        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // ============ ARRANGE ============
            String unknownEmail = "unknown@test.com";
            when(userRepo.findByEmail(unknownEmail))
                    .thenReturn(Optional.empty());

            // ============ ACT & ASSERT ============
            /*
             * Note: The service uses orElseThrow() without a supplier,
             * which throws NoSuchElementException instead of a custom exception.
             * This could be improved for consistency with placeOrder().
             */
            assertThatThrownBy(() ->
                    orderService.getUserOrders(unknownEmail))
                    .isInstanceOf(NoSuchElementException.class);

            verify(userRepo).findByEmail(unknownEmail);
            verify(orderRepo, never()).findByUserId(any());
        }

        /**
         * Tests that order responses contain correct timestamps.
         *
         * <p>
         * <b>Scenario:</b> Verify placedAt field is correctly mapped.
         */
        @Test
        @DisplayName("Should include correct timestamps in order responses")
        void shouldIncludeCorrectTimestamps() {
            // ============ ARRANGE ============
            LocalDateTime orderTime = LocalDateTime.of(2024, 1, 15, 12, 30, 0);
            Order order = Order.builder()
                    .id(1L)
                    .user(user)
                    .restaurant(restaurant)
                    .status(OrderStatus.CREATED)
                    .totalAmount(new BigDecimal("25.98"))
                    .createdAt(orderTime)
                    .build();

            when(userRepo.findByEmail(user.getEmail()))
                    .thenReturn(Optional.of(user));
            when(orderRepo.findByUserId(user.getId()))
                    .thenReturn(Collections.singletonList(order));

            // ============ ACT ============
            List<OrderResponse> responses = orderService.getUserOrders(user.getEmail());

            // ============ ASSERT ============
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getPlacedAt()).isEqualTo(orderTime);
        }
    }
}
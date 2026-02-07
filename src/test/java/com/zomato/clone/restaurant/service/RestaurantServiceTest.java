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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RestaurantService}.
 *
 * <p>
 * This test class verifies the business logic of the RestaurantService,
 * which handles restaurant management operations including:
 * <ul>
 *   <li>Restaurant creation by authorized owners</li>
 *   <li>Menu item management</li>
 *   <li>Restaurant and menu retrieval</li>
 * </ul>
 *
 * <h2>Testing Strategy</h2>
 * <p>
 * We use the following testing approach:
 * <ul>
 *   <li><b>Unit Testing</b>: Testing service layer in isolation</li>
 *   <li><b>Mocking</b>: Using Mockito to mock repository dependencies</li>
 *   <li><b>AAA Pattern</b>: Arrange-Act-Assert pattern for test structure</li>
 *   <li><b>Nested Classes</b>: Grouping related tests for better organization</li>
 * </ul>
 *
 * <h2>Key Annotations Used</h2>
 * <ul>
 *   <li>{@code @ExtendWith(MockitoExtension.class)} - Enables Mockito annotations</li>
 *   <li>{@code @Mock} - Creates mock instances of dependencies</li>
 *   <li>{@code @InjectMocks} - Injects mocks into the service under test</li>
 *   <li>{@code @Captor} - Captures arguments passed to mocked methods</li>
 *   <li>{@code @Nested} - Groups related test cases</li>
 *   <li>{@code @DisplayName} - Provides readable test names in reports</li>
 * </ul>
 *
 * @author Zomato Clone Team
 * @version 1.0
 * @see RestaurantService
 * @see Restaurant
 * @see MenuItem
 */
@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    // ==================== MOCK DEPENDENCIES ====================

    /**
     * Mocked RestaurantRepository.
     * <p>
     * This mock simulates database operations for Restaurant entities,
     * allowing us to test service logic without actual database connections.
     */
    @Mock
    private RestaurantRepository restaurantRepo;

    /**
     * Mocked MenuItemRepository.
     * <p>
     * This mock simulates database operations for MenuItem entities.
     */
    @Mock
    private MenuItemRepository menuItemRepo;

    /**
     * Mocked UserRepository.
     * <p>
     * This mock simulates user lookup operations, essential for
     * authorization checks in the service.
     */
    @Mock
    private UserRepository userRepo;

    // ==================== SERVICE UNDER TEST ====================

    /**
     * The service instance being tested.
     * <p>
     * {@code @InjectMocks} automatically injects the above mocks
     * into this service instance via constructor injection.
     */
    @InjectMocks
    private RestaurantService restaurantService;

    // ==================== ARGUMENT CAPTORS ====================

    /**
     * Captures Restaurant objects passed to repository save methods.
     * <p>
     * ArgumentCaptors allow us to inspect the actual objects that were
     * passed to mocked methods, enabling deeper verification of behavior.
     *
     * <p>Example usage:
     * <pre>{@code
     * verify(restaurantRepo).save(restaurantCaptor.capture());
     * Restaurant captured = restaurantCaptor.getValue();
     * assertThat(captured.getName()).isEqualTo("Expected Name");
     * }</pre>
     */
    @Captor
    private ArgumentCaptor<Restaurant> restaurantCaptor;

    /**
     * Captures MenuItem objects passed to repository save methods.
     */
    @Captor
    private ArgumentCaptor<MenuItem> menuItemCaptor;

    // ==================== TEST FIXTURES ====================

    /**
     * Pre-configured User with RESTAURANT role.
     * Used as a valid restaurant owner in tests.
     */
    private User restaurantOwner;

    /**
     * Pre-configured User with USER role.
     * Used to test authorization failures.
     */
    private User regularUser;

    /**
     * Pre-configured Restaurant entity.
     * Used as existing restaurant in tests.
     */
    private Restaurant restaurant;

    /**
     * Pre-configured request DTO for creating restaurants.
     */
    private CreateRestaurantRequest createRestaurantRequest;

    /**
     * Pre-configured request DTO for creating menu items.
     */
    private MenuItemRequest menuItemRequest;

    // ==================== TEST SETUP ====================

    /**
     * Initializes test fixtures before each test method.
     * <p>
     * This method runs before every {@code @Test} method, ensuring
     * each test starts with a fresh, consistent set of test data.
     *
     * <p>
     * <b>Why @BeforeEach?</b>
     * <ul>
     *   <li>Ensures test isolation - each test gets fresh objects</li>
     *   <li>Reduces code duplication across test methods</li>
     *   <li>Makes tests more readable by moving setup code out</li>
     * </ul>
     *
     * <p>
     * <b>Test Data Created:</b>
     * <ul>
     *   <li>Restaurant owner (RESTAURANT role)</li>
     *   <li>Regular user (USER role)</li>
     *   <li>Sample restaurant</li>
     *   <li>Restaurant creation request</li>
     *   <li>Menu item creation request</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        /*
         * Setup restaurant owner with RESTAURANT role.
         * This user should be able to create restaurants and manage menus.
         */
        restaurantOwner = User.builder()
                .id(1L)
                .email("owner@test.com")
                .name("Restaurant Owner")
                .role(UserRole.RESTAURANT)
                .build();

        /*
         * Setup regular user with USER role.
         * This user should NOT be able to create restaurants.
         * Used to verify authorization checks.
         */
        regularUser = User.builder()
                .id(2L)
                .email("user@test.com")
                .name("Regular User")
                .role(UserRole.USER)
                .build();

        /*
         * Setup a sample restaurant owned by restaurantOwner.
         * Used in tests that require an existing restaurant.
         */
        restaurant = Restaurant.builder()
                .id(1L)
                .owner(restaurantOwner)
                .name("Test Restaurant")
                .description("Test Description")
                .address("123 Test Street")
                .isOpen(true)
                .build();

        /*
         * Setup request DTO for creating new restaurants.
         * Contains all required fields for restaurant creation.
         */
        createRestaurantRequest = CreateRestaurantRequest.builder()
                .name("New Restaurant")
                .description("New Description")
                .address("456 New Street")
                .build();

        /*
         * Setup request DTO for creating new menu items.
         * Contains all required fields for menu item creation.
         */
        menuItemRequest = MenuItemRequest.builder()
                .name("Pizza")
                .description("Delicious pizza")
                .price(new BigDecimal("12.99"))
                .availableQuantity(100)
                .build();
    }

    // ==================== CREATE RESTAURANT TESTS ====================

    /**
     * Test suite for {@link RestaurantService#createRestaurant(CreateRestaurantRequest, String)}.
     * <p>
     * This nested class groups all tests related to restaurant creation,
     * providing better organization and readability in test reports.
     *
     * <p>
     * <b>Scenarios Tested:</b>
     * <ul>
     *   <li>Successful creation by authorized restaurant owner</li>
     *   <li>Failure when user does not exist</li>
     *   <li>Failure when user lacks RESTAURANT role</li>
     * </ul>
     *
     * <p>
     * <b>Business Rules Verified:</b>
     * <ul>
     *   <li>Only users with RESTAURANT role can create restaurants</li>
     *   <li>User must exist in the system</li>
     *   <li>Restaurant is created with isOpen=true by default</li>
     * </ul>
     */
    @Nested
    @DisplayName("createRestaurant Tests")
    class CreateRestaurantTests {

        /**
         * Tests successful restaurant creation by an authorized owner.
         * <p>
         * <b>Scenario:</b> A user with RESTAURANT role creates a new restaurant.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>User exists with RESTAURANT role</li>
         *   <li>Valid restaurant creation request</li>
         * </ul>
         *
         * <b>When:</b> createRestaurant() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>Restaurant is created with correct details</li>
         *   <li>Restaurant is marked as open</li>
         *   <li>Owner is correctly associated</li>
         *   <li>Repository save method is called once</li>
         * </ul>
         */
        @Test
        @DisplayName("Should create restaurant successfully when user is restaurant owner")
        void shouldCreateRestaurantSuccessfully() {
            // ============ ARRANGE ============
            /*
             * Configure mock to return the restaurant owner when queried by email.
             * This simulates finding the user in the database.
             */
            when(userRepo.findByEmail(restaurantOwner.getEmail()))
                    .thenReturn(Optional.of(restaurantOwner));

            /*
             * Configure mock to return the saved restaurant with an assigned ID.
             * Using thenAnswer() allows us to modify the input and return it,
             * simulating how JPA assigns IDs on save.
             */
            when(restaurantRepo.save(any(Restaurant.class)))
                    .thenAnswer(invocation -> {
                        Restaurant saved = invocation.getArgument(0);
                        saved.setId(1L);  // Simulate ID assignment by database
                        return saved;
                    });

            // ============ ACT ============
            /*
             * Call the method under test with valid inputs.
             */
            Restaurant result = restaurantService.createRestaurant(
                    createRestaurantRequest,
                    restaurantOwner.getEmail()
            );

            // ============ ASSERT ============
            /*
             * Verify the returned restaurant has correct properties.
             * Using AssertJ's fluent assertions for readability.
             */
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(createRestaurantRequest.getName());
            assertThat(result.getDescription()).isEqualTo(createRestaurantRequest.getDescription());
            assertThat(result.getAddress()).isEqualTo(createRestaurantRequest.getAddress());
            assertThat(result.getIsOpen()).isTrue();  // Default should be open

            /*
             * Verify that repository methods were called correctly.
             * This ensures the service interacts with dependencies as expected.
             */
            verify(userRepo).findByEmail(restaurantOwner.getEmail());
            verify(restaurantRepo).save(restaurantCaptor.capture());

            /*
             * Use ArgumentCaptor to verify the restaurant passed to save()
             * has the correct owner association.
             */
            Restaurant capturedRestaurant = restaurantCaptor.getValue();
            assertThat(capturedRestaurant.getOwner()).isEqualTo(restaurantOwner);
        }

        /**
         * Tests that UsernameNotFoundException is thrown for non-existent users.
         * <p>
         * <b>Scenario:</b> Attempting to create a restaurant with an email
         * that doesn't exist in the system.
         *
         * <p>
         * <b>Given:</b> Email does not match any user in the database
         *
         * <p>
         * <b>When:</b> createRestaurant() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>UsernameNotFoundException is thrown</li>
         *   <li>Exception message is "User not found"</li>
         *   <li>Restaurant is NOT saved (save() never called)</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw UsernameNotFoundException when user does not exist")
        void shouldThrowExceptionWhenUserNotFound() {
            // ============ ARRANGE ============
            String nonExistentEmail = "nonexistent@test.com";

            /*
             * Configure mock to return empty Optional, simulating user not found.
             */
            when(userRepo.findByEmail(nonExistentEmail))
                    .thenReturn(Optional.empty());

            // ============ ACT & ASSERT ============
            /*
             * Use assertThatThrownBy() from AssertJ to verify exception.
             * This is cleaner than try-catch or @Test(expected=...).
             */
            assertThatThrownBy(() ->
                    restaurantService.createRestaurant(createRestaurantRequest, nonExistentEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            /*
             * Verify that we attempted to find the user but never tried to save.
             * This confirms the service fails fast on invalid input.
             */
            verify(userRepo).findByEmail(nonExistentEmail);
            verify(restaurantRepo, never()).save(any());
        }

        /**
         * Tests that regular users cannot create restaurants.
         * <p>
         * <b>Scenario:</b> A user with USER role (not RESTAURANT) attempts
         * to create a restaurant.
         *
         * <p>
         * <b>Security Rule Verified:</b> Only RESTAURANT role can create restaurants.
         *
         * <p>
         * <b>Given:</b> User exists but has USER role (not RESTAURANT)
         *
         * <p>
         * <b>When:</b> createRestaurant() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>RuntimeException is thrown</li>
         *   <li>Exception message indicates authorization failure</li>
         *   <li>Restaurant is NOT saved</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw RuntimeException when user is not a restaurant owner")
        void shouldThrowExceptionWhenUserIsNotRestaurantOwner() {
            // ============ ARRANGE ============
            /*
             * Configure mock to return a regular user (not restaurant owner).
             */
            when(userRepo.findByEmail(regularUser.getEmail()))
                    .thenReturn(Optional.of(regularUser));

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    restaurantService.createRestaurant(createRestaurantRequest, regularUser.getEmail()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Only Restaurant Owners can create restaurants");

            /*
             * Verify authorization check happened before any save attempt.
             */
            verify(userRepo).findByEmail(regularUser.getEmail());
            verify(restaurantRepo, never()).save(any());
        }
    }

    // ==================== ADD MENU ITEM TESTS ====================

    /**
     * Test suite for {@link RestaurantService#addMenuItem(Long, MenuItemRequest, String)}.
     * <p>
     * This nested class tests menu item creation functionality.
     *
     * <p>
     * <b>Scenarios Tested:</b>
     * <ul>
     *   <li>Successful menu item addition by restaurant owner</li>
     *   <li>Failure when restaurant doesn't exist</li>
     *   <li>Failure when user is not the restaurant owner</li>
     *   <li>Failure when different restaurant owner tries to add items</li>
     * </ul>
     *
     * <p>
     * <b>Business Rules Verified:</b>
     * <ul>
     *   <li>Restaurant must exist</li>
     *   <li>Only the restaurant's owner can add menu items</li>
     *   <li>Menu items are available by default</li>
     * </ul>
     */
    @Nested
    @DisplayName("addMenuItem Tests")
    class AddMenuItemTests {

        /**
         * Tests successful menu item addition by the restaurant owner.
         * <p>
         * <b>Scenario:</b> Restaurant owner adds a new menu item to their restaurant.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>Restaurant exists</li>
         *   <li>User is the owner of the restaurant</li>
         *   <li>Valid menu item request</li>
         * </ul>
         *
         * <b>When:</b> addMenuItem() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>Menu item is created with correct details</li>
         *   <li>Menu item is associated with the restaurant</li>
         *   <li>Menu item is marked as available</li>
         * </ul>
         */
        @Test
        @DisplayName("Should add menu item successfully when user is restaurant owner")
        void shouldAddMenuItemSuccessfully() {
            // ============ ARRANGE ============
            /*
             * Configure mock to return the restaurant when queried by ID.
             */
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));

            /*
             * Configure mock to simulate saving menu item with ID assignment.
             */
            when(menuItemRepo.save(any(MenuItem.class)))
                    .thenAnswer(invocation -> {
                        MenuItem saved = invocation.getArgument(0);
                        saved.setId(1L);
                        return saved;
                    });

            // ============ ACT ============
            MenuItem result = restaurantService.addMenuItem(
                    restaurant.getId(),
                    menuItemRequest,
                    restaurantOwner.getEmail()  // Same as restaurant's owner
            );

            // ============ ASSERT ============
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(menuItemRequest.getName());
            assertThat(result.getDescription()).isEqualTo(menuItemRequest.getDescription());
            assertThat(result.getPrice()).isEqualTo(menuItemRequest.getPrice());
            assertThat(result.getAvailableQuantity()).isEqualTo(menuItemRequest.getAvailableQuantity());
            assertThat(result.getIsAvailable()).isTrue();  // Default should be available

            /*
             * Verify repository interactions and captured arguments.
             */
            verify(restaurantRepo).findById(restaurant.getId());
            verify(menuItemRepo).save(menuItemCaptor.capture());

            MenuItem capturedMenuItem = menuItemCaptor.getValue();
            assertThat(capturedMenuItem.getRestaurant()).isEqualTo(restaurant);
        }

        /**
         * Tests that adding menu item to non-existent restaurant fails.
         * <p>
         * <b>Scenario:</b> Attempting to add a menu item to a restaurant
         * that doesn't exist in the database.
         *
         * <p>
         * <b>Given:</b> Restaurant ID doesn't match any restaurant
         *
         * <p>
         * <b>When:</b> addMenuItem() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>RuntimeException is thrown</li>
         *   <li>Exception message is "Restaurant not found"</li>
         *   <li>Menu item is NOT saved</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw RuntimeException when restaurant not found")
        void shouldThrowExceptionWhenRestaurantNotFound() {
            // ============ ARRANGE ============
            Long nonExistentRestaurantId = 999L;
            when(restaurantRepo.findById(nonExistentRestaurantId))
                    .thenReturn(Optional.empty());

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    restaurantService.addMenuItem(
                            nonExistentRestaurantId,
                            menuItemRequest,
                            restaurantOwner.getEmail()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Restaurant not found");

            verify(restaurantRepo).findById(nonExistentRestaurantId);
            verify(menuItemRepo, never()).save(any());
        }

        /**
         * Tests that non-owners cannot add menu items.
         * <p>
         * <b>Scenario:</b> A regular user (not the owner) attempts to add
         * a menu item to a restaurant.
         *
         * <p>
         * <b>Security Rule Verified:</b> Only restaurant owner can modify menu.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>Restaurant exists</li>
         *   <li>User is NOT the owner of the restaurant</li>
         * </ul>
         *
         * <b>When:</b> addMenuItem() is called
         *
         * <p>
         * <b>Then:</b>
         * <ul>
         *   <li>RuntimeException is thrown</li>
         *   <li>Menu item is NOT saved</li>
         * </ul>
         */
        @Test
        @DisplayName("Should throw RuntimeException when user is not the restaurant owner")
        void shouldThrowExceptionWhenUserIsNotOwner() {
            // ============ ARRANGE ============
            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    restaurantService.addMenuItem(
                            restaurant.getId(),
                            menuItemRequest,
                            regularUser.getEmail()))  // Different from owner
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("You are not the owner of this restaurant");

            verify(restaurantRepo).findById(restaurant.getId());
            verify(menuItemRepo, never()).save(any());
        }

        /**
         * Tests that one restaurant owner cannot modify another's menu.
         * <p>
         * <b>Scenario:</b> A user with RESTAURANT role tries to add menu items
         * to a restaurant they don't own.
         *
         * <p>
         * <b>Security Rule Verified:</b> Ownership is strictly enforced,
         * even for users with RESTAURANT role.
         *
         * <p>
         * <b>Given:</b>
         * <ul>
         *   <li>Restaurant exists and is owned by owner A</li>
         *   <li>Owner B (different RESTAURANT role user) tries to add menu item</li>
         * </ul>
         *
         * <b>When:</b> addMenuItem() is called by owner B
         *
         * <p>
         * <b>Then:</b> Authorization fails, menu item is not added
         */
        @Test
        @DisplayName("Should throw RuntimeException when different restaurant owner tries to add menu item")
        void shouldThrowExceptionWhenDifferentOwnerTriesToAddMenuItem() {
            // ============ ARRANGE ============
            /*
             * Create another restaurant owner (different from the one who owns 'restaurant').
             */
            User anotherOwner = User.builder()
                    .id(3L)
                    .email("another.owner@test.com")
                    .name("Another Owner")
                    .role(UserRole.RESTAURANT)  // Has RESTAURANT role but doesn't own this restaurant
                    .build();

            when(restaurantRepo.findById(restaurant.getId()))
                    .thenReturn(Optional.of(restaurant));

            // ============ ACT & ASSERT ============
            assertThatThrownBy(() ->
                    restaurantService.addMenuItem(
                            restaurant.getId(),
                            menuItemRequest,
                            anotherOwner.getEmail()))  // Different owner
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("You are not the owner of this restaurant");

            verify(menuItemRepo, never()).save(any());
        }
    }

    // ==================== GET ALL RESTAURANTS TESTS ====================

    /**
     * Test suite for {@link RestaurantService#getAllRestaurants()}.
     * <p>
     * Tests the retrieval of all restaurants from the database.
     *
     * <p>
     * <b>Scenarios Tested:</b>
     * <ul>
     *   <li>Returns all restaurants when multiple exist</li>
     *   <li>Returns empty list when no restaurants exist</li>
     * </ul>
     *
     * <p>
     * <b>Note:</b> This is a public endpoint with no authorization checks.
     */
    @Nested
    @DisplayName("getAllRestaurants Tests")
    class GetAllRestaurantsTests {

        /**
         * Tests that all restaurants are returned.
         * <p>
         * <b>Scenario:</b> Multiple restaurants exist in the database.
         *
         * <p>
         * <b>Given:</b> Two restaurants exist
         *
         * <p>
         * <b>When:</b> getAllRestaurants() is called
         *
         * <p>
         * <b>Then:</b> Both restaurants are returned in the list
         */
        @Test
        @DisplayName("Should return all restaurants")
        void shouldReturnAllRestaurants() {
            // ============ ARRANGE ============
            Restaurant restaurant2 = Restaurant.builder()
                    .id(2L)
                    .owner(restaurantOwner)
                    .name("Second Restaurant")
                    .description("Second Description")
                    .address("789 Another Street")
                    .isOpen(true)
                    .build();

            List<Restaurant> expectedRestaurants = Arrays.asList(restaurant, restaurant2);
            when(restaurantRepo.findAll()).thenReturn(expectedRestaurants);

            // ============ ACT ============
            List<Restaurant> result = restaurantService.getAllRestaurants();

            // ============ ASSERT ============
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedRestaurants);

            verify(restaurantRepo).findAll();
        }

        /**
         * Tests behavior when no restaurants exist.
         * <p>
         * <b>Scenario:</b> Database has no restaurants.
         *
         * <p>
         * <b>Given:</b> No restaurants in database
         *
         * <p>
         * <b>When:</b> getAllRestaurants() is called
         *
         * <p>
         * <b>Then:</b> Empty list is returned (not null)
         */
        @Test
        @DisplayName("Should return empty list when no restaurants exist")
        void shouldReturnEmptyListWhenNoRestaurants() {
            // ============ ARRANGE ============
            when(restaurantRepo.findAll()).thenReturn(Collections.emptyList());

            // ============ ACT ============
            List<Restaurant> result = restaurantService.getAllRestaurants();

            // ============ ASSERT ============
            assertThat(result).isEmpty();

            verify(restaurantRepo).findAll();
        }
    }

    // ==================== GET MENU TESTS ====================

    /**
     * Test suite for {@link RestaurantService#getMenu(Long)}.
     * <p>
     * Tests the retrieval of menu items for a specific restaurant.
     *
     * <p>
     * <b>Scenarios Tested:</b>
     * <ul>
     *   <li>Returns all menu items for a restaurant</li>
     *   <li>Returns empty list when restaurant has no menu items</li>
     *   <li>Returns empty list for non-existent restaurant</li>
     * </ul>
     *
     * <p>
     * <b>Note:</b> This is a public endpoint. The current implementation
     * does not validate if the restaurant exists before querying menu items.
     */
    @Nested
    @DisplayName("getMenu Tests")
    class GetMenuTests {

        /**
         * Tests that menu items are returned for a restaurant.
         * <p>
         * <b>Scenario:</b> Restaurant has multiple menu items.
         *
         * <p>
         * <b>Given:</b> Restaurant has 2 menu items
         *
         * <p>
         * <b>When:</b> getMenu() is called with restaurant ID
         *
         * <p>
         * <b>Then:</b> Both menu items are returned
         */
        @Test
        @DisplayName("Should return menu items for a restaurant")
        void shouldReturnMenuItemsForRestaurant() {
            // ============ ARRANGE ============
            MenuItem menuItem1 = MenuItem.builder()
                    .id(1L)
                    .restaurant(restaurant)
                    .name("Pizza")
                    .description("Delicious pizza")
                    .price(new BigDecimal("12.99"))
                    .availableQuantity(100)
                    .isAvailable(true)
                    .build();

            MenuItem menuItem2 = MenuItem.builder()
                    .id(2L)
                    .restaurant(restaurant)
                    .name("Burger")
                    .description("Juicy burger")
                    .price(new BigDecimal("9.99"))
                    .availableQuantity(50)
                    .isAvailable(true)
                    .build();

            List<MenuItem> expectedMenuItems = Arrays.asList(menuItem1, menuItem2);
            when(menuItemRepo.findByRestaurantId(restaurant.getId()))
                    .thenReturn(expectedMenuItems);

            // ============ ACT ============
            List<MenuItem> result = restaurantService.getMenu(restaurant.getId());

            // ============ ASSERT ============
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedMenuItems);

            verify(menuItemRepo).findByRestaurantId(restaurant.getId());
        }

        /**
         * Tests behavior when restaurant has no menu items.
         * <p>
         * <b>Scenario:</b> Restaurant exists but has no menu items yet.
         *
         * <p>
         * <b>Given:</b> Restaurant has no menu items
         *
         * <p>
         * <b>When:</b> getMenu() is called
         *
         * <p>
         * <b>Then:</b> Empty list is returned
         */
        @Test
        @DisplayName("Should return empty list when restaurant has no menu items")
        void shouldReturnEmptyListWhenNoMenuItems() {
            // ============ ARRANGE ============
            when(menuItemRepo.findByRestaurantId(restaurant.getId()))
                    .thenReturn(Collections.emptyList());

            // ============ ACT ============
            List<MenuItem> result = restaurantService.getMenu(restaurant.getId());

            // ============ ASSERT ============
            assertThat(result).isEmpty();

            verify(menuItemRepo).findByRestaurantId(restaurant.getId());
        }

        /**
         * Tests behavior for non-existent restaurant.
         * <p>
         * <b>Scenario:</b> Querying menu for a restaurant ID that doesn't exist.
         *
         * <p>
         * <b>Given:</b> Restaurant ID doesn't exist
         *
         * <p>
         * <b>When:</b> getMenu() is called
         *
         * <p>
         * <b>Then:</b> Empty list is returned
         *
         * <p>
         * <b>Note:</b> Current implementation doesn't distinguish between
         * "restaurant not found" and "restaurant has no items". This could
         * be improved by adding restaurant existence validation.
         */
        @Test
        @DisplayName("Should return empty list for non-existent restaurant")
        void shouldReturnEmptyListForNonExistentRestaurant() {
            // ============ ARRANGE ============
            Long nonExistentRestaurantId = 999L;
            when(menuItemRepo.findByRestaurantId(nonExistentRestaurantId))
                    .thenReturn(Collections.emptyList());

            // ============ ACT ============
            List<MenuItem> result = restaurantService.getMenu(nonExistentRestaurantId);

            // ============ ASSERT ============
            assertThat(result).isEmpty();

            verify(menuItemRepo).findByRestaurantId(nonExistentRestaurantId);
        }
    }
}
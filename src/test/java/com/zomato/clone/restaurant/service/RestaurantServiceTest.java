package com.zomato.clone.restaurant.service;

import com.zomato.clone.enums.UserRole;
import com.zomato.clone.restaurant.dto.CreateRestaurantRequest;
import com.zomato.clone.restaurant.entity.Restaurant;
import com.zomato.clone.restaurant.repository.RestaurantRepository;
import com.zomato.clone.user.entity.User;
import com.zomato.clone.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RestaurantService}.
 *
 * <p>
 * Focus:
 * - Business logic validation
 * - Authorization rules
 * - Repository interaction verification
 *
 * <p>
 * Dependencies are mocked to keep tests fast and isolated.
 */
@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    void shouldCreateRestaurantWhenUserIsRestaurantOwner() {
        // Arrange
        User owner = User.builder()
                .email("owner@test.com")
                .role(UserRole.RESTAURANT)
                .build();

        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("Test Restaurant")
                .address("Test Address")
                .description("Test Description")
                .build();

        when(userRepository.findByEmail(owner.getEmail()))
                .thenReturn(Optional.of(owner));

        when(restaurantRepository.save(any(Restaurant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Restaurant restaurant =
                restaurantService.createRestaurant(request, owner.getEmail());

        // Assert
        assertThat(restaurant.getName()).isEqualTo("Test Restaurant");
        assertThat(restaurant.getOwner()).isEqualTo(owner);
        assertThat(restaurant.getIsOpen()).isTrue();

        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                restaurantService.createRestaurant(
                        CreateRestaurantRequest.builder().build(),
                        "missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void shouldRejectRestaurantCreationForNonOwnerUser() {
        // Arrange
        User user = User.builder()
                .email("user@test.com")
                .role(UserRole.USER)
                .build();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() ->
                restaurantService.createRestaurant(
                        CreateRestaurantRequest.builder().build(),
                        user.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only Restaurant Owners can create restaurants");

        verify(restaurantRepository, never()).save(any());
    }
}

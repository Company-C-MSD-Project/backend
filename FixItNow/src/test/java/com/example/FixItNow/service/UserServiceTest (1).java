package com.example.FixItNow.service;

import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User homeowner;
    private User provider;

    @BeforeEach
    void setUp() {
        homeowner = User.builder()
                .id(1L)
                .name("Alice Smith")
                .email("alice@example.com")
                .username("alice123")
                .passwordHash("$2a$hashed")
                .userType(UserType.HOMEOWNER)
                .isActive(true)
                .isBlacklisted(false)
                .build();

        provider = User.builder()
                .id(2L)
                .name("Bob Fix")
                .email("bob@example.com")
                .username("bob_fix")
                .passwordHash("$2a$hashed2")
                .userType(UserType.SERVICE_PROVIDER)
                .isActive(true)
                .isVerified(true)
                .build();
    }

    // findById() 

    /**
     * UT_07 – Existing user ID should return the correct user.
     */
    @Test
    @DisplayName("UT_07: findById() – existing user ID (1L) should return user")
    void ut07_findById_existingId_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(homeowner));

        User result = userService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    /**
     * UT_08 – Non-existent ID (9999L) should throw ResourceNotFoundException.
     */
    @Test
    @DisplayName("UT_08: findById() – non-existent ID (9999L) should throw ResourceNotFoundException")
    void ut08_findById_nonExistentId_shouldThrow() {
        when(userRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    //  updateProfile() 

    /**
     * UT_09 – Valid user ID with phone and address updates should persist
     *         and return the updated user.
     */
    @Test
    @DisplayName("UT_09: updateProfile() – valid ID with phone='0771234567', address='No.5 Main St' should update")
    void ut09_updateProfile_validIdWithUpdates_shouldSaveChanges() {
        User updates = new User();
        updates.setPhone("0771234567");
        updates.setAddress("No.5 Main St");

        when(userRepository.findById(1L)).thenReturn(Optional.of(homeowner));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile(1L, updates);

        assertThat(result.getPhone()).isEqualTo("0771234567");
        assertThat(result.getAddress()).isEqualTo("No.5 Main St");
        verify(userRepository).save(homeowner);
    }

    /**
     * UT_10 – Non-existent user ID should throw ResourceNotFoundException
     *         before any save is attempted.
     */
    @Test
    @DisplayName("UT_10: updateProfile() – non-existent user ID should throw ResourceNotFoundException")
    void ut10_updateProfile_nonExistentId_shouldThrow() {
        when(userRepository.findById(9999L)).thenReturn(Optional.empty());

        User updates = new User();
        updates.setPhone("0771234567");

        assertThatThrownBy(() -> userService.updateProfile(9999L, updates))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    //  verifyProvider() 
    /**
     * UT_11 – Unverified SERVICE_PROVIDER should have isVerified set to true.
     */
    @Test
    @DisplayName("UT_11: verifyProvider() – unverified SERVICE_PROVIDER should become verified")
    void ut11_verifyProvider_unverifiedProvider_shouldSetVerifiedTrue() {
        User unverifiedProvider = User.builder()
                .id(3L)
                .name("Carol Fix")
                .email("carol@example.com")
                .username("carol_fix")
                .passwordHash("hash")
                .userType(UserType.SERVICE_PROVIDER)
                .isVerified(false)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(unverifiedProvider));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.verifyProvider(3L);

        assertThat(result.isVerified()).isTrue();
        verify(userRepository).save(unverifiedProvider);
    }

    /**
     * UT_12 – Passing a HOMEOWNER ID to verifyProvider() should throw
     *         BadRequestException.
     */
    @Test
    @DisplayName("UT_12: verifyProvider() – HOMEOWNER ID should throw BadRequestException")
    void ut12_verifyProvider_homeownerId_shouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(homeowner));

        assertThatThrownBy(() -> userService.verifyProvider(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a service provider");

        verify(userRepository, never()).save(any());
    }

    // blacklistHomeowner() 

    /**
     * UT_13 – Active HOMEOWNER should be blacklisted (isBlacklisted=true)
     *         and deactivated (isActive=false).
     */
    @Test
    @DisplayName("UT_13: blacklistHomeowner() – active HOMEOWNER should be blacklisted and deactivated")
    void ut13_blacklistHomeowner_activeHomeowner_shouldBlacklistAndDeactivate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(homeowner));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.blacklistHomeowner(1L);

        assertThat(result.isBlacklisted()).isTrue();
        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(homeowner);
    }

    /**
     * UT_14 – Passing a SERVICE_PROVIDER ID to blacklistHomeowner() should
     *         throw BadRequestException — only homeowners can be blacklisted.
     */
    @Test
    @DisplayName("UT_14: blacklistHomeowner() – SERVICE_PROVIDER ID should throw BadRequestException")
    void ut14_blacklistHomeowner_serviceProviderId_shouldThrow() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> userService.blacklistHomeowner(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only homeowners");

        verify(userRepository, never()).save(any());
    }
}

package com.cookigram;

import com.cookigram.dto.RegistrationDto;
import com.cookigram.model.Role;
import com.cookigram.model.User;
import com.cookigram.repository.UserRepository;
import com.cookigram.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── Registration Tests ───────────────────────────────────────────

    @Test
    void testRegisterNewCustomerSuccessfully() {
        RegistrationDto dto = buildRegistrationDto();

        User saved = userService.registerNewCustomer(dto);

        assertNotNull(saved.getId());
        assertEquals(dto.getUsername(), saved.getUsername());
        assertEquals(Role.ROLE_CUSTOMER, saved.getRole());
        assertTrue(saved.isEnabled());
    }

    @Test
    void testPasswordIsEncodedOnRegistration() {
        RegistrationDto dto = buildRegistrationDto();

        User saved = userService.registerNewCustomer(dto);

        assertNotEquals("password123", saved.getPassword());
        assertTrue(passwordEncoder.matches("password123", saved.getPassword()));
    }

    @Test
    void testRegisteredUserHasCorrectFullName() {
        RegistrationDto dto = buildRegistrationDto();
        dto.setFullName("Parmatar Singh");

        User saved = userService.registerNewCustomer(dto);

        assertEquals("Parmatar Singh", saved.getFullName());
    }

    @Test
    void testRegisteredUserHasCustomerRole() {
        RegistrationDto dto = buildRegistrationDto();

        User saved = userService.registerNewCustomer(dto);

        assertEquals(Role.ROLE_CUSTOMER, saved.getRole());
    }

    // ─── Username/Email Existence Tests ───────────────────────────────

    @Test
    void testUsernameExistsReturnsTrueForExistingUser() {
        RegistrationDto dto = buildRegistrationDto();
        userService.registerNewCustomer(dto);

        assertTrue(userService.usernameExists(dto.getUsername()));
    }

    @Test
    void testUsernameExistsReturnsFalseForNewUsername() {
        assertFalse(userService.usernameExists("totally_new_user_" + UUID.randomUUID()));
    }

    @Test
    void testEmailExistsReturnsTrueForExistingEmail() {
        RegistrationDto dto = buildRegistrationDto();
        userService.registerNewCustomer(dto);

        assertTrue(userService.emailExists(dto.getEmail()));
    }

    @Test
    void testEmailExistsReturnsFalseForNewEmail() {
        assertFalse(userService.emailExists("newemail_" + UUID.randomUUID() + "@test.com"));
    }

    // ─── Admin/Data Tests ─────────────────────────────────────────────

    @Test
    void testGetRecentUsersReturnsAtMostTen() {
        List<User> recent = userService.getRecentUsers();

        assertTrue(recent.size() <= 10);
    }

    @Test
    void testGetTotalUsersIsPositive() {
        long total = userService.getTotalUsers();

        assertTrue(total > 0);
    }

    @Test
    void testGetTotalUsersIncreasesAfterRegistration() {
        long before = userService.getTotalUsers();
        userService.registerNewCustomer(buildRegistrationDto());
        long after = userService.getTotalUsers();

        assertEquals(before + 1, after);
    }

    // ─── Repository Direct Tests ──────────────────────────────────────

    @Test
    void testFindByUsernameReturnsCorrectUser() {
        RegistrationDto dto = buildRegistrationDto();
        userService.registerNewCustomer(dto);

        Optional<User> found = userRepository.findByUsername(dto.getUsername());

        assertTrue(found.isPresent());
        assertEquals(dto.getUsername(), found.get().getUsername());
    }

    @Test
    void testFindByUsernameReturnsEmptyForUnknownUser() {
        Optional<User> found = userRepository.findByUsername("ghost_user_" + UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private RegistrationDto buildRegistrationDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("user_" + UUID.randomUUID().toString().substring(0, 8));
        dto.setFullName("Test User");
        dto.setEmail("test_" + UUID.randomUUID() + "@cookiegram.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        return dto;
    }
}
package com.cookigram;

import com.cookigram.model.Order;
import com.cookigram.model.OrderStatus;
import com.cookigram.model.Role;
import com.cookigram.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderModelTest {

    private Order order;
    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUsername("modeltest_customer");
        customer.setFullName("Model Test User");
        customer.setRole(Role.ROLE_CUSTOMER);
        customer.setEnabled(true);

        order = new Order();
        order.setCustomer(customer);
        order.setStreetAddress("21 Oban Rd");
        order.setApartmentSuite("Unit 5");
        order.setCity("Brampton");
        order.setProvince("ON - Ontario");
        order.setPostalCode("L6Y 3X5");
        order.setCustomMessage("Happy Birthday Parmatar!");
        order.setPrice(new BigDecimal("24.99"));
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());
    }

    // ─── getFullAddress Tests ─────────────────────────────────────────

    @Test
    void testGetFullAddressWithApartment() {
        order.setDeliveryDate(LocalDate.now().plusDays(5));

        String address = order.getFullAddress();

        assertTrue(address.contains("21 Oban Rd"));
        assertTrue(address.contains("Unit 5"));
        assertTrue(address.contains("Brampton"));
        assertTrue(address.contains("L6Y 3X5"));
    }

    @Test
    void testGetFullAddressWithoutApartment() {
        order.setApartmentSuite(null);
        order.setDeliveryDate(LocalDate.now().plusDays(5));

        String address = order.getFullAddress();

        assertFalse(address.contains("null"));
        assertTrue(address.contains("21 Oban Rd"));
        assertTrue(address.contains("Brampton"));
    }

    @Test
    void testGetFullAddressWithEmptyApartment() {
        order.setApartmentSuite("");
        order.setDeliveryDate(LocalDate.now().plusDays(5));

        String address = order.getFullAddress();

        assertFalse(address.contains(", ,"));
    }

    // ─── getTruncatedMessage Tests ────────────────────────────────────

    @Test
    void testTruncatedMessageShortEnough() {
        order.setCustomMessage("Hi there!");

        assertEquals("Hi there!", order.getTruncatedMessage());
    }

    @Test
    void testTruncatedMessageExactly25Chars() {
        order.setCustomMessage("1234567890123456789012345");

        assertEquals("1234567890123456789012345", order.getTruncatedMessage());
    }

    @Test
    void testTruncatedMessageOver25Chars() {
        order.setCustomMessage("This message is definitely longer than 25 chars");

        String truncated = order.getTruncatedMessage();

        assertTrue(truncated.endsWith("..."));
        assertEquals(28, truncated.length()); // 25 chars + "..."
    }

    @Test
    void testTruncatedMessageNull() {
        order.setCustomMessage(null);

        assertEquals("", order.getTruncatedMessage());
    }

    // ─── canBeCancelled Tests ─────────────────────────────────────────

    @Test
    void testCanBeCancelledWhenPlacedAndFarEnough() {
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryDate(LocalDate.now().plusDays(10));

        assertTrue(order.canBeCancelled());
    }

    @Test
    void testCannotBeCancelledWhenTooClose() {
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryDate(LocalDate.now().plusDays(2));

        assertFalse(order.canBeCancelled());
    }

    @Test
    void testCannotBeCancelledWhenExactly3DaysAway() {
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryDate(LocalDate.now().plusDays(3));

        assertFalse(order.canBeCancelled());
    }

    @Test
    void testCannotBeCancelledWhenAlreadyCancelled() {
        order.setStatus(OrderStatus.CANCELLED);
        order.setDeliveryDate(LocalDate.now().plusDays(10));

        assertFalse(order.canBeCancelled());
    }

    @Test
    void testCannotBeCancelledWhenInPreparation() {
        order.setStatus(OrderStatus.IN_PREPARATION);
        order.setDeliveryDate(LocalDate.now().plusDays(10));

        assertFalse(order.canBeCancelled());
    }

    @Test
    void testCannotBeCancelledWhenDelivered() {
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryDate(LocalDate.now().plusDays(10));

        assertFalse(order.canBeCancelled());
    }

    // ─── OrderStatus Display Name Tests ──────────────────────────────

    @Test
    void testOrderStatusDisplayNames() {
        assertEquals("Placed", OrderStatus.PLACED.getDisplayName());
        assertEquals("In Preparation", OrderStatus.IN_PREPARATION.getDisplayName());
        assertEquals("Delivered", OrderStatus.DELIVERED.getDisplayName());
        assertEquals("Cancelled", OrderStatus.CANCELLED.getDisplayName());
    }

    // ─── Postal Code Formatting Tests ────────────────────────────────

    @Test
    void testPostalCodeStoredCorrectly() {
        order.setPostalCode("L6Y 3X5");

        assertEquals("L6Y 3X5", order.getPostalCode());
    }

    @Test
    void testPriceIsCorrect() {
        assertEquals(new BigDecimal("24.99"), order.getPrice());
    }
}
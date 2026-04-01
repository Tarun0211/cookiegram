package com.cookigram;

import com.cookigram.dto.OrderFormDto;
import com.cookigram.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceValidationTest {

    @Autowired
    private OrderService orderService;

    // ─── Date Validation ──────────────────────────────────────────────

    @Test
    void testValidationPassesForValidForm() {
        OrderFormDto form = buildValidForm("Happy Anniversary!");

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidationFailsWhenDateIsNull() {
        OrderFormDto form = buildValidForm("Hello");
        form.setDeliveryDate(null);

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("required")));
    }

    @Test
    void testValidationFailsWhenDateTooSoon() {
        OrderFormDto form = buildValidForm("Rush order");
        form.setDeliveryDate(LocalDate.now().plusDays(1));

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("4 days")));
    }

    @Test
    void testValidationFailsWhenDateTooFarAhead() {
        OrderFormDto form = buildValidForm("Future order");
        form.setDeliveryDate(LocalDate.now().plusYears(2));

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("1 year")));
    }

    @Test
    void testValidationPassesForMinimumDate() {
        OrderFormDto form = buildValidForm("Just in time");
        form.setDeliveryDate(LocalDate.now().plusDays(4));

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertTrue(result.isValid());
    }

    // ─── Inappropriate Word Validation ────────────────────────────────

    @Test
    void testValidationFailsForInappropriateMessage() {
        OrderFormDto form = buildValidForm("I hate this");

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("inappropriate")));
    }

    @Test
    void testValidationFailsForInappropriateMessageCaseInsensitive() {
        OrderFormDto form = buildValidForm("I HATE this");

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertFalse(result.isValid());
    }

    @Test
    void testValidationPassesForCleanMessage() {
        OrderFormDto form = buildValidForm("Happy Birthday Grandma!");

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        assertTrue(result.isValid());
    }

    @Test
    void testValidationPassesForEmptyMessage() {
        // Empty message passes inappropriate word check (caught by @NotBlank elsewhere)
        OrderFormDto form = buildValidForm("");

        OrderService.ValidationResult result = orderService.validateOrderForm(form);

        // No inappropriate words — passes the service-level validation
        assertTrue(result.getErrors().stream().noneMatch(e -> e.contains("inappropriate")));
    }

    // ─── Capacity Tests ───────────────────────────────────────────────

    @Test
    void testIsDateAvailableReturnsTrueForFutureDate() {
        LocalDate farFuture = LocalDate.now().plusDays(300);

        boolean available = orderService.isDateAvailable(farFuture);

        assertTrue(available);
    }

    @Test
    void testRemainingCapacityIsPositiveForFutureDate() {
        LocalDate farFuture = LocalDate.now().plusDays(300);

        int remaining = orderService.getRemainingCapacity(farFuture);

        assertTrue(remaining > 0);
        assertTrue(remaining <= 30);
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private OrderFormDto buildValidForm(String message) {
        OrderFormDto form = new OrderFormDto();
        form.setDeliveryDate(LocalDate.now().plusDays(5));
        form.setStreetAddress("456 Maple Ave");
        form.setCity("Brampton");
        form.setProvince("ON - Ontario");
        form.setPostalCode("L6Y3X5");
        form.setCustomMessage(message);
        return form;
    }
}
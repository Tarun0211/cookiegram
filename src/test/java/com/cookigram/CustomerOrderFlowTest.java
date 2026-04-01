package com.cookigram;

import com.cookigram.dto.OrderFormDto;
import com.cookigram.dto.PaymentFormDto;
import com.cookigram.model.Order;
import com.cookigram.model.OrderStatus;
import com.cookigram.model.Role;
import com.cookigram.model.User;
import com.cookigram.repository.OrderRepository;
import com.cookigram.repository.UserRepository;
import com.cookigram.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CustomerOrderFlowTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User testCustomer;

    @BeforeEach
    void setUp() {
        // Create a fresh test customer before each test
        testCustomer = new User();
        testCustomer.setUsername("testcustomer_" + UUID.randomUUID());
        testCustomer.setPassword("password");
        testCustomer.setFullName("Test Customer");
        testCustomer.setEmail("test_" + UUID.randomUUID() + "@test.com");
        testCustomer.setRole(Role.ROLE_CUSTOMER);
        testCustomer.setEnabled(true);
        userRepository.save(testCustomer);
    }

    // ─── Order Creation Tests ─────────────────────────────────────────

    @Test
    void testCreateOrderSuccessfully() {
        OrderFormDto form = buildValidOrderForm("Happy Birthday!");
        String transactionId = UUID.randomUUID().toString();

        Order order = orderService.createOrder(testCustomer, form, transactionId);

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals("Happy Birthday!", order.getCustomMessage());
        assertEquals("123 Main Street", order.getStreetAddress());
        assertEquals(transactionId, order.getTransactionId());
    }

    @Test
    void testCreateOrderSavesCorrectCustomer() {
        OrderFormDto form = buildValidOrderForm("Congrats!");
        String transactionId = UUID.randomUUID().toString();

        Order order = orderService.createOrder(testCustomer, form, transactionId);

        assertEquals(testCustomer.getUsername(), order.getCustomer().getUsername());
    }

    @Test
    void testCreateOrderWithMaxLengthMessage() {
        String maxMessage = "A".repeat(50);
        OrderFormDto form = buildValidOrderForm(maxMessage);
        String transactionId = UUID.randomUUID().toString();

        Order order = orderService.createOrder(testCustomer, form, transactionId);

        assertEquals(maxMessage, order.getCustomMessage());
    }

    // ─── Duplicate Transaction Tests ──────────────────────────────────

    @Test
    void testDuplicateTransactionThrowsException() {
        OrderFormDto form = buildValidOrderForm("Hello!");
        String transactionId = UUID.randomUUID().toString();

        orderService.createOrder(testCustomer, form, transactionId);

        assertThrows(OrderService.DuplicateOrderException.class, () ->
            orderService.createOrder(testCustomer, buildValidOrderForm("Hello again!"), transactionId)
        );
    }

    // ─── Order Retrieval Tests ─────────────────────────────────────────

    @Test
    void testGetCustomerOrdersReturnsCorrectOrders() {
        orderService.createOrder(testCustomer, buildValidOrderForm("Order 1"), UUID.randomUUID().toString());
        orderService.createOrder(testCustomer, buildValidOrderForm("Order 2"), UUID.randomUUID().toString());

        List<Order> orders = orderService.getCustomerOrders(testCustomer);

        assertTrue(orders.size() >= 2);
    }

    @Test
    void testGetOrderByIdAndCustomer() {
        Order created = orderService.createOrder(testCustomer, buildValidOrderForm("Find me!"), UUID.randomUUID().toString());

        Optional<Order> found = orderService.getOrderByIdAndCustomer(created.getId(), testCustomer);

        assertTrue(found.isPresent());
        assertEquals("Find me!", found.get().getCustomMessage());
    }

    @Test
    void testGetOrderByIdAndWrongCustomerReturnsEmpty() {
        Order created = orderService.createOrder(testCustomer, buildValidOrderForm("Secret!"), UUID.randomUUID().toString());

        User wrongUser = new User();
        wrongUser.setUsername("wronguser_" + UUID.randomUUID());
        wrongUser.setPassword("pass");
        wrongUser.setFullName("Wrong User");
        wrongUser.setRole(Role.ROLE_CUSTOMER);
        wrongUser.setEnabled(true);
        userRepository.save(wrongUser);

        Optional<Order> found = orderService.getOrderByIdAndCustomer(created.getId(), wrongUser);

        assertTrue(found.isEmpty());
    }

    // ─── Cancellation Tests ───────────────────────────────────────────

    @Test
    void testCancelOrderSuccessfully() {
        OrderFormDto form = buildValidOrderForm("Cancel me");
        // Set delivery date far enough for cancellation to be allowed
        form.setDeliveryDate(LocalDate.now().plusDays(10));
        Order created = orderService.createOrder(testCustomer, form, UUID.randomUUID().toString());

        Order cancelled = orderService.cancelOrder(created.getId(), testCustomer);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void testCancelOrderTooCloseThroughCanBeCancelledMethod() {
        OrderFormDto form = buildValidOrderForm("Too late to cancel");
        form.setDeliveryDate(LocalDate.now().plusDays(5));
        Order created = orderService.createOrder(testCustomer, form, UUID.randomUUID().toString());

        // Manually set delivery date to 2 days from now to simulate being too close
        created.setDeliveryDate(LocalDate.now().plusDays(2));
        orderRepository.save(created);

        assertFalse(created.canBeCancelled());
    }

    @Test
    void testCancelAlreadyCancelledOrderThrowsException() {
        OrderFormDto form = buildValidOrderForm("Double cancel");
        form.setDeliveryDate(LocalDate.now().plusDays(10));
        Order created = orderService.createOrder(testCustomer, form, UUID.randomUUID().toString());

        orderService.cancelOrder(created.getId(), testCustomer);

        assertThrows(OrderService.OrderCancellationException.class, () ->
            orderService.cancelOrder(created.getId(), testCustomer)
        );
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private OrderFormDto buildValidOrderForm(String message) {
        OrderFormDto form = new OrderFormDto();
        form.setDeliveryDate(LocalDate.now().plusDays(5));
        form.setStreetAddress("123 Main Street");
        form.setCity("Toronto");
        form.setProvince("ON - Ontario");
        form.setPostalCode("M5V1A1");
        form.setCustomMessage(message);
        return form;
    }
}
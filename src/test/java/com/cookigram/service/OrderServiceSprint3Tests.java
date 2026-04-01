package com.cookigram.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cookigram.model.Order;
import com.cookigram.model.OrderStatus;
import com.cookigram.model.Role;
import com.cookigram.model.User;
import com.cookigram.repository.OrderRepository;
import com.cookigram.service.OrderService.InvalidStatusTransitionException;

@ExtendWith(MockitoExtension.class)
class OrderServiceSprint3Tests {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void filterOrders_shouldReturnMatchingCustomerAndStatusResults() {
        Order matchingOrder = buildOrder("ORD-1001", "Krishna Patel", LocalDate.now().plusDays(3), OrderStatus.PLACED);
        Order nonMatchingOrder = buildOrder("ORD-1002", "Tarun Parmar", LocalDate.now().plusDays(10), OrderStatus.READY_FOR_DELIVERY);

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(matchingOrder, nonMatchingOrder));

        List<Order> filteredOrders = orderService.filterOrders(
                "Krishna",
                OrderStatus.PLACED,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7),
                "ORD-1001");

        assertEquals(1, filteredOrders.size());
        assertEquals("ORD-1001", filteredOrders.get(0).getId());
        assertEquals(OrderStatus.PLACED, filteredOrders.get(0).getStatus());
    }

    @Test
    void updateOrderStatus_shouldMovePlacedOrderToInPreparation_whenTransitionIsValid() {
        Order order = buildOrder("ORD-2001", "Khushi Shah", LocalDate.now().plusDays(2), OrderStatus.PLACED);

        when(orderRepository.findById("ORD-2001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.updateOrderStatus("ORD-2001", OrderStatus.IN_PREPARATION);

        assertEquals(OrderStatus.IN_PREPARATION, updatedOrder.getStatus());
        assertNotNull(updatedOrder.getUpdatedAt());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_shouldThrowException_whenTransitionIsInvalid() {
        Order order = buildOrder("ORD-3001", "Gurshaan Singh", LocalDate.now().plusDays(2), OrderStatus.PLACED);

        when(orderRepository.findById("ORD-3001")).thenReturn(Optional.of(order));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateOrderStatus("ORD-3001", OrderStatus.READY_FOR_DELIVERY));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getDailySummary_shouldCalculateTodayCountsCorrectly() {
        Order placedOrder = buildOrder("ORD-4001", "Krishna Patel", LocalDate.now(), OrderStatus.PLACED);
        Order prepOrder = buildOrder("ORD-4002", "Tarun Parmar", LocalDate.now(), OrderStatus.IN_PREPARATION);
        Order readyOrder = buildOrder("ORD-4003", "Khushi Shah", LocalDate.now(), OrderStatus.READY_FOR_DELIVERY);
        Order deliveredOrder = buildOrder("ORD-4004", "Gurshaan Singh", LocalDate.now(), OrderStatus.DELIVERED);
        Order cancelledOrder = buildOrder("ORD-4005", "Cancelled User", LocalDate.now(), OrderStatus.CANCELLED);

        when(orderRepository.findByDeliveryDate(LocalDate.now()))
                .thenReturn(List.of(placedOrder, prepOrder, readyOrder, deliveredOrder, cancelledOrder));

        OrderService.DailySummary summary = orderService.getDailySummary();

        assertEquals(4, summary.getTotalOrdersToday());
        assertEquals(1, summary.getOrdersInPreparation());
        assertEquals(1, summary.getReadyForDelivery());
        assertEquals(1, summary.getPlaced());
        assertEquals(1, summary.getDelivered());
    }

    private Order buildOrder(String id, String fullName, LocalDate deliveryDate, OrderStatus status) {
        User customer = new User("user-" + id, "password", fullName, id.toLowerCase() + "@cookigram.com", Role.ROLE_CUSTOMER);

        Order order = new Order();
        order.setId(id);
        order.setCustomer(customer);
        order.setDeliveryDate(deliveryDate);
        order.setStatus(status);
        order.setPrice(new BigDecimal("24.99"));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}

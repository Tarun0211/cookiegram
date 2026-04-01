package com.cookigram.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookigram.dto.OrderFormDto;
import com.cookigram.model.Order;
import com.cookigram.model.OrderStatus;
import com.cookigram.model.User;
import com.cookigram.repository.OrderRepository;
import com.cookigram.service.OrderService.OrderNotFoundException;

@Service
public class OrderService {

    private static final int DAILY_CAPACITY = 30;
    private static final BigDecimal ORDER_PRICE = new BigDecimal("24.99");
    private static final BigDecimal PROFIT_PER_ORDER = new BigDecimal("8.00"); // Fixed profit per order
    private static final Set<String> INAPPROPRIATE_WORDS = new HashSet<>(Arrays.asList(
        "damn", "hell", "crap", "stupid", "idiot", "hate", "kill", "die",
        "fuck", "shit", "ass", "bitch", "bastard"
    ));

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getCustomerOrders(User customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public boolean isDateAvailable(LocalDate date) {
        long count = orderRepository.findByDeliveryDateAndStatusNot(date, OrderStatus.CANCELLED).size();
        return count < DAILY_CAPACITY;
    }

    public int getRemainingCapacity(LocalDate date) {
        long count = orderRepository.findByDeliveryDateAndStatusNot(date, OrderStatus.CANCELLED).size();
        return Math.max(0, DAILY_CAPACITY - (int) count);
    }

    public ValidationResult validateOrderForm(OrderFormDto form) {
        List<String> errors = new ArrayList<>();
        LocalDate minDate = LocalDate.now().plusDays(4);
        LocalDate maxDate = LocalDate.now().plusYears(1);

        if (form.getDeliveryDate() == null) {
            errors.add("Delivery date is required");
        } else {
            if (form.getDeliveryDate().isBefore(minDate))
                errors.add("Delivery date must be at least 4 days from today (3 days preparation time required)");
            if (form.getDeliveryDate().isAfter(maxDate))
                errors.add("Delivery date cannot be more than 1 year from today");
            if (!isDateAvailable(form.getDeliveryDate()))
                errors.add("Selected date is fully booked. Please choose another date.");
        }

        if (form.getCustomMessage() != null && containsInappropriateWords(form.getCustomMessage()))
            errors.add("Your message contains inappropriate words. Please revise.");

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private boolean containsInappropriateWords(String message) {
        String lower = message.toLowerCase();
        for (String word : INAPPROPRIATE_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    @Transactional
    public Order createOrder(User customer, OrderFormDto form, String transactionId) {
        if (orderRepository.existsByTransactionId(transactionId))
            throw new DuplicateOrderException("Order already submitted");

        if (!isDateAvailable(form.getDeliveryDate()))
            throw new CapacityExceededException("Selected date is fully booked");

        Order order = new Order();
        order.setCustomer(customer);
        order.setDeliveryDate(form.getDeliveryDate());
        order.setStreetAddress(form.getStreetAddress());
        order.setCity(form.getCity());
        order.setApartmentSuite(form.getApartmentSuite());
        order.setProvince(form.getProvince());
        order.setPostalCode(form.getFormattedPostalCode());
        order.setCustomMessage(form.getCustomMessage());
        order.setPrice(ORDER_PRICE);
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());
        order.setTransactionId(transactionId);

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(String orderId, User customer) {
        Order order = orderRepository.findByIdAndCustomer(orderId, customer)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.canBeCancelled())
            throw new OrderCancellationException("This order cannot be cancelled");

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByIdAndCustomer(String id, User customer) {
        return orderRepository.findByIdAndCustomer(id, customer);
    }

    // ─── Sprint 3: Employee Features ─────────────────────────────────

    /**
     * Get all orders with optional filters
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Filter orders based on criteria
     */
    public List<Order> filterOrders(String customerName, OrderStatus status, 
                                     LocalDate startDate, LocalDate endDate, String orderId) {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        
        return orders.stream()
            .filter(order -> {
                // Filter by order ID
                if (orderId != null && !orderId.trim().isEmpty()) {
                    if (!order.getId().toLowerCase().contains(orderId.toLowerCase().trim())) {
                        return false;
                    }
                }
                
                // Filter by customer name
                if (customerName != null && !customerName.trim().isEmpty()) {
                    String fullName = order.getCustomer().getFullName().toLowerCase();
                    if (!fullName.contains(customerName.toLowerCase().trim())) {
                        return false;
                    }
                }
                
                // Filter by status
                if (status != null && order.getStatus() != status) {
                    return false;
                }
                
                // Filter by date range
                if (startDate != null && order.getDeliveryDate().isBefore(startDate)) {
                    return false;
                }
                if (endDate != null && order.getDeliveryDate().isAfter(endDate)) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * Update order status with validation
     * - PLACED → IN_PREPARATION → READY_FOR_DELIVERY → DELIVERED
     * - Only orders with delivery date in next 7 days can have status changed (except auto-update)
     */
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        // Check if order is already delivered or cancelled
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidStatusTransitionException("Cannot modify a delivered order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot modify a cancelled order");
        }
        
        // Only allow status changes for orders with delivery date in next 7 days
        // (unless it's auto-updating past orders to delivered)
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);
        
        if (newStatus != OrderStatus.DELIVERED || !order.getDeliveryDate().isBefore(today)) {
            // For normal status updates, check if delivery date is within 7 days
            if (order.getDeliveryDate().isAfter(sevenDaysLater)) {
                throw new InvalidStatusTransitionException(
                    "Can only update status for orders with delivery date in the next 7 days. " +
                    "This order is scheduled for " + order.getDeliveryDate());
            }
        }
        
        // Validate status transition
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Invalid status transition from " + order.getStatus().getDisplayName() + 
                " to " + newStatus.getDisplayName());
        }
        
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Auto-update orders with past delivery dates to DELIVERED status
     */
    @Transactional
    public int autoUpdatePastDeliveries() {
        LocalDate today = LocalDate.now();
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        int updatedCount = 0;
        
        for (Order order : orders) {
            if (order.getDeliveryDate().isBefore(today) && 
                order.getStatus() != OrderStatus.DELIVERED && 
                order.getStatus() != OrderStatus.CANCELLED) {
                order.setStatus(OrderStatus.DELIVERED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    /**
     * Get upcoming deliveries for today and next 7 days
     */
    public List<Order> getUpcomingDeliveries() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        
        return orderRepository.findByDeliveryDateBetweenAndStatusNotOrderByDeliveryDateAsc(
            today, nextWeek, OrderStatus.CANCELLED);
    }

    /**
     * Get today's deliveries
     */
    public List<Order> getTodayDeliveries() {
        LocalDate today = LocalDate.now();
        return orderRepository.findByDeliveryDate(today).stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .collect(Collectors.toList());
    }

    /**
     * Get daily summary statistics
     */
    public DailySummary getDailySummary() {
        LocalDate today = LocalDate.now();
        List<Order> todayOrders = getTodayDeliveries();
        
        long totalOrdersToday = todayOrders.size();
        long ordersInPreparation = todayOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.IN_PREPARATION)
            .count();
        long readyForDelivery = todayOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.READY_FOR_DELIVERY)
            .count();
        long placed = todayOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PLACED)
            .count();
        long delivered = todayOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .count();
        
        return new DailySummary(totalOrdersToday, ordersInPreparation, readyForDelivery, placed, delivered);
    }

    /**
     * Check if order can have its status updated (for display purposes)
     */
    public boolean canUpdateStatus(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);
        
        // Only orders with delivery date in next 7 days can be updated
        return !order.getDeliveryDate().isAfter(sevenDaysLater);
    }

    // ─── Sprint 3: Admin Features ────────────────────────────────────

    /**
     * Get total orders year-to-date (non-cancelled)
     */
    public long getTotalOrdersYTD() {
        LocalDateTime startOfYear = LocalDateTime.of(LocalDate.now().getYear(), 1, 1, 0, 0);
        List<Order> orders = orderRepository.findOrdersFromDate(startOfYear);
        return orders.size();
    }

    /**
     * Get total revenue (sum of all non-cancelled order prices)
     */
    public BigDecimal getTotalRevenue() {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .collect(Collectors.toList());
        
        return orders.stream()
            .map(Order::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total revenue for date range
     */
    public BigDecimal getRevenueForDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Order> orders = orderRepository.findOrdersInDateRange(startDateTime, endDateTime);
        return orders.stream()
            .map(Order::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get estimated profit (fixed amount per order)
     */
    public BigDecimal getEstimatedProfit() {
        long totalOrders = orderRepository.countByStatusNot(OrderStatus.CANCELLED);
        return PROFIT_PER_ORDER.multiply(BigDecimal.valueOf(totalOrders));
    }

    /**
     * Get orders per day for a date range (for reports)
     */
    public Map<LocalDate, Long> getOrdersPerDay(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Order> orders = orderRepository.findOrdersInDateRange(startDateTime, endDateTime);
        
        Map<LocalDate, Long> ordersPerDay = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            ordersPerDay.put(current, 0L);
            current = current.plusDays(1);
        }
        
        for (Order order : orders) {
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            ordersPerDay.merge(orderDate, 1L, Long::sum);
        }
        
        return ordersPerDay;
    }

    /**
     * Get orders per week for a date range
     */
    public Map<String, Long> getOrdersPerWeek(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dailyOrders = getOrdersPerDay(startDate, endDate);
        Map<String, Long> weeklyOrders = new LinkedHashMap<>();
        
        LocalDate weekStart = startDate;
        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) weekEnd = endDate;
            
            long weekTotal = 0;
            LocalDate current = weekStart;
            while (!current.isAfter(weekEnd)) {
                weekTotal += dailyOrders.getOrDefault(current, 0L);
                current = current.plusDays(1);
            }
            
            String weekLabel = weekStart + " to " + weekEnd;
            weeklyOrders.put(weekLabel, weekTotal);
            
            weekStart = weekStart.plusDays(7);
        }
        
        return weeklyOrders;
    }

    /**
     * Get highest selling day in a date range
     */
    public Map.Entry<LocalDate, Long> getHighestSellingDay(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> ordersPerDay = getOrdersPerDay(startDate, endDate);
        
        return ordersPerDay.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
    }

    /**
     * Get report data for a date range
     */
    public ReportData getReportData(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> ordersPerDay = getOrdersPerDay(startDate, endDate);
        Map<String, Long> ordersPerWeek = getOrdersPerWeek(startDate, endDate);
        Map.Entry<LocalDate, Long> highestSellingDay = getHighestSellingDay(startDate, endDate);
        BigDecimal revenue = getRevenueForDateRange(startDate, endDate);
        
        long totalOrders = ordersPerDay.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal profit = PROFIT_PER_ORDER.multiply(BigDecimal.valueOf(totalOrders));
        
        return new ReportData(ordersPerDay, ordersPerWeek, highestSellingDay, totalOrders, revenue, profit);
    }

    /**
     * Validate date range for reports
     */
    public ValidationResult validateDateRange(LocalDate startDate, LocalDate endDate) {
        List<String> errors = new ArrayList<>();
        
        if (startDate == null) {
            errors.add("Start date is required");
        }
        if (endDate == null) {
            errors.add("End date is required");
        }
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                errors.add("Start date cannot be after end date");
            }
            if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
                errors.add("Date range cannot exceed 1 year");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ─── Inner Classes ───────────────────────────────────────────────

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }

    public static class DailySummary {
        private final long totalOrdersToday;
        private final long ordersInPreparation;
        private final long readyForDelivery;
        private final long placed;
        private final long delivered;

        public DailySummary(long totalOrdersToday, long ordersInPreparation, 
                           long readyForDelivery, long placed, long delivered) {
            this.totalOrdersToday = totalOrdersToday;
            this.ordersInPreparation = ordersInPreparation;
            this.readyForDelivery = readyForDelivery;
            this.placed = placed;
            this.delivered = delivered;
        }

        public long getTotalOrdersToday() { return totalOrdersToday; }
        public long getOrdersInPreparation() { return ordersInPreparation; }
        public long getReadyForDelivery() { return readyForDelivery; }
        public long getPlaced() { return placed; }
        public long getDelivered() { return delivered; }
    }

    public static class ReportData {
        private final Map<LocalDate, Long> ordersPerDay;
        private final Map<String, Long> ordersPerWeek;
        private final Map.Entry<LocalDate, Long> highestSellingDay;
        private final long totalOrders;
        private final BigDecimal revenue;
        private final BigDecimal profit;

        public ReportData(Map<LocalDate, Long> ordersPerDay, Map<String, Long> ordersPerWeek,
                         Map.Entry<LocalDate, Long> highestSellingDay, long totalOrders,
                         BigDecimal revenue, BigDecimal profit) {
            this.ordersPerDay = ordersPerDay;
            this.ordersPerWeek = ordersPerWeek;
            this.highestSellingDay = highestSellingDay;
            this.totalOrders = totalOrders;
            this.revenue = revenue;
            this.profit = profit;
        }

        public Map<LocalDate, Long> getOrdersPerDay() { return ordersPerDay; }
        public Map<String, Long> getOrdersPerWeek() { return ordersPerWeek; }
        public Map.Entry<LocalDate, Long> getHighestSellingDay() { return highestSellingDay; }
        public long getTotalOrders() { return totalOrders; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getProfit() { return profit; }
    }

    // ─── Custom Exceptions ───────────────────────────────────────────

    public static class DuplicateOrderException extends RuntimeException {
        public DuplicateOrderException(String message) { super(message); }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) { super(message); }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) { super(message); }
    }

    public static class OrderCancellationException extends RuntimeException {
        public OrderCancellationException(String message) { super(message); }
    }

    public static class InvalidStatusTransitionException extends RuntimeException {
        public InvalidStatusTransitionException(String message) { super(message); }
    }
}

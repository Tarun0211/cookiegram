package com.cookigram.repository;

import com.cookigram.model.Order;
import com.cookigram.model.OrderStatus;
import com.cookigram.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    Optional<Order> findByTransactionId(String transactionId);
    boolean existsByTransactionId(String transactionId);
    Optional<Order> findByIdAndCustomer(String id, User customer);
    List<Order> findByDeliveryDateAndStatusNot(LocalDate date, OrderStatus status);
    
    // Sprint 3: Filter queries for Employee
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusIn(List<OrderStatus> statuses);
    List<Order> findByDeliveryDateBetween(LocalDate startDate, LocalDate endDate);
    List<Order> findByDeliveryDateBetweenAndStatusNot(LocalDate startDate, LocalDate endDate, OrderStatus status);
    List<Order> findByDeliveryDate(LocalDate date);
    
    // Find orders with status not cancelled and delivery date in range
    List<Order> findByDeliveryDateBetweenAndStatusNotOrderByDeliveryDateAsc(LocalDate startDate, LocalDate endDate, OrderStatus status);
    
    // Find all orders for filtering (sorted by created date desc)
    List<Order> findAllByOrderByCreatedAtDesc();
    
    // Count queries for dashboard summary
    long countByStatus(OrderStatus status);
    long countByDeliveryDateAndStatusNot(LocalDate date, OrderStatus status);
    long countByDeliveryDate(LocalDate date);
    
    // Sprint 3: Admin reports - orders by date range
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 }, 'status': { $ne: 'CANCELLED' } }")
    List<Order> findOrdersInDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find orders for a specific year (admin YTD)
    @Query("{ 'createdAt': { $gte: ?0 }, 'status': { $ne: 'CANCELLED' } }")
    List<Order> findOrdersFromDate(LocalDateTime fromDate);
    
    // Count all non-cancelled orders
    long countByStatusNot(OrderStatus status);
}

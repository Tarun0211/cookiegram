package com.cookigram.controller;

import com.cookigram.model.Order;
import com.cookigram.model.OrderStatus;
import com.cookigram.service.OrderService;
import com.cookigram.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    
    @Autowired
    private PromotionService promotionService;
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // Auto-update past deliveries to DELIVERED status
        orderService.autoUpdatePastDeliveries();
        
        model.addAttribute("username", authentication.getName());
        model.addAttribute("promotions", promotionService.getActivePromotions());
        
        // Get daily summary
        OrderService.DailySummary summary = orderService.getDailySummary();
        model.addAttribute("summary", summary);
        
        // Get upcoming deliveries (next 7 days)
        List<Order> upcomingDeliveries = orderService.getUpcomingDeliveries();
        model.addAttribute("upcomingDeliveries", upcomingDeliveries);
        
        // Get orders to prepare (PLACED and IN_PREPARATION for next 7 days)
        List<Order> ordersToPrepare = orderService.filterOrders(null, null, 
            LocalDate.now(), LocalDate.now().plusDays(7), null);
        // Filter only PLACED and IN_PREPARATION
        ordersToPrepare = ordersToPrepare.stream()
            .filter(o -> o.getStatus() == OrderStatus.PLACED || 
                        o.getStatus() == OrderStatus.IN_PREPARATION)
            .limit(10)
            .toList();
        model.addAttribute("ordersToPrepare", ordersToPrepare);
        
        return "employee/dashboard";
    }
    
    @GetMapping("/orders")
    public String viewOrders(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String orderId,
            Authentication authentication,
            Model model) {
        
        // Auto-update past deliveries
        orderService.autoUpdatePastDeliveries();
        
        model.addAttribute("username", authentication.getName());
        
        // Get filtered orders
        List<Order> orders = orderService.filterOrders(customerName, status, startDate, endDate, orderId);
        model.addAttribute("orders", orders);
        
        // Pass filter values back to form
        model.addAttribute("customerName", customerName);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("orderId", orderId);
        
        // Pass all statuses for dropdown
        model.addAttribute("statuses", OrderStatus.values());
        
        // Add order service for canUpdateStatus check
        model.addAttribute("orderService", orderService);
        
        return "employee/orders";
    }
    
    @PostMapping("/orders/{orderId}/update-status")
    public String updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus newStatus,
            RedirectAttributes redirectAttributes) {
        
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Order status updated to " + updatedOrder.getStatus().getDisplayName());
        } catch (OrderService.OrderNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found");
        } catch (OrderService.InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update order status: " + e.getMessage());
        }
        
        return "redirect:/employee/orders";
    }
    
    @GetMapping("/upcoming-deliveries")
    public String upcomingDeliveries(Authentication authentication, Model model) {
        // Auto-update past deliveries
        orderService.autoUpdatePastDeliveries();
        
        model.addAttribute("username", authentication.getName());
        
        // Get today's deliveries
        List<Order> todayDeliveries = orderService.getTodayDeliveries();
        model.addAttribute("todayDeliveries", todayDeliveries);
        
        // Get upcoming deliveries (next 7 days, excluding today)
        List<Order> upcomingDeliveries = orderService.getUpcomingDeliveries().stream()
            .filter(o -> !o.getDeliveryDate().equals(LocalDate.now()))
            .toList();
        model.addAttribute("upcomingDeliveries", upcomingDeliveries);
        
        // Get daily summary
        OrderService.DailySummary summary = orderService.getDailySummary();
        model.addAttribute("summary", summary);
        
        // Pass order service for canUpdateStatus check
        model.addAttribute("orderService", orderService);
        model.addAttribute("statuses", OrderStatus.values());
        
        return "employee/upcoming-deliveries";
    }
}

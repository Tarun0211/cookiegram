package com.cookigram.controller;

import com.cookigram.service.OrderService;
import com.cookigram.service.PromotionService;
import com.cookigram.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private PromotionService promotionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("promotions", promotionService.getActivePromotions());
        model.addAttribute("recentUsers", userService.getRecentUsers());
        model.addAttribute("totalUsers", userService.getTotalUsers());
        
        // Sprint 3: Add real sales data from database
        long totalOrdersYTD = orderService.getTotalOrdersYTD();
        BigDecimal totalRevenue = orderService.getTotalRevenue();
        BigDecimal estimatedProfit = orderService.getEstimatedProfit();
        
        model.addAttribute("totalOrdersYTD", totalOrdersYTD);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("estimatedProfit", estimatedProfit);
        
        // Get daily summary for quick stats
        OrderService.DailySummary summary = orderService.getDailySummary();
        model.addAttribute("todayOrders", summary.getTotalOrdersToday());
        
        return "admin/dashboard";
    }
    
    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication,
            Model model) {
        
        model.addAttribute("username", authentication.getName());
        
        // Default date range: last 30 days
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        // Validate date range
        OrderService.ValidationResult validation = orderService.validateDateRange(startDate, endDate);
        if (!validation.isValid()) {
            model.addAttribute("errors", validation.getErrors());
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            return "admin/reports";
        }
        
        // Get report data
        OrderService.ReportData reportData = orderService.getReportData(startDate, endDate);
        
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("reportData", reportData);
        
        // Calculate max orders for chart scaling
        long maxOrders = reportData.getOrdersPerDay().values().stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(1L);
        if (maxOrders == 0) maxOrders = 1; // Prevent division by zero
        model.addAttribute("maxOrders", maxOrders);
        
        // Check if there's data
        boolean hasData = reportData.getTotalOrders() > 0;
        model.addAttribute("hasData", hasData);
        
        return "admin/reports";
    }
}

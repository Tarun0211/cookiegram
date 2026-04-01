package com.cookigram.controller;

import com.cookigram.model.Order;
import com.cookigram.model.User;
import com.cookigram.repository.UserRepository;
import com.cookigram.service.OrderService;
import com.cookigram.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    
    @Autowired
    private PromotionService promotionService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        model.addAttribute("username", username);
        model.addAttribute("user", user);
        model.addAttribute("promotions", promotionService.getActivePromotions());
        if (user != null) {
            List<Order> orders = orderService.getCustomerOrders(user);
            model.addAttribute("orders", orders);
            model.addAttribute("hasOrders", !orders.isEmpty());
        } else {
            model.addAttribute("orders", List.of());
            model.addAttribute("hasOrders", false);
        }
        return "customer/dashboard";
    }
}
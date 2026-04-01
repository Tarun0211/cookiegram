package com.cookigram.controller;

import com.cookigram.model.Promotion;
import com.cookigram.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {
    
    @Autowired
    private PromotionService promotionService;
    
    @GetMapping("/")
    public String home(Model model) {
        List<Promotion> promotions = promotionService.getActivePromotions();
        model.addAttribute("promotions", promotions);
        return "index";
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
package com.cookigram.controller;

import com.cookigram.model.Promotion;
import com.cookigram.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PromotionRestController {
    
    @Autowired
    private PromotionService promotionService;
    
    @GetMapping("/promotions")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }
}
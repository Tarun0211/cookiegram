package com.cookigram.service;

import com.cookigram.model.Promotion;
import com.cookigram.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PromotionService {
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByIsActiveTrueOrderByIdDesc();
    }
    
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }
    
    public Promotion savePromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }
}
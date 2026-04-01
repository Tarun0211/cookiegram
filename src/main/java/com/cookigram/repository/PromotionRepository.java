package com.cookigram.repository;

import com.cookigram.model.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PromotionRepository extends MongoRepository<Promotion, String> {
    List<Promotion> findByIsActiveTrue();
    List<Promotion> findByIsActiveTrueOrderByIdDesc();

}
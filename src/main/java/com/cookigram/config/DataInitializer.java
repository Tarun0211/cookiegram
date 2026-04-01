package com.cookigram.config;

import com.cookigram.model.Promotion;
import com.cookigram.model.Role;
import com.cookigram.model.User;
import com.cookigram.repository.PromotionRepository;
import com.cookigram.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin1")) {
            User admin = new User("admin1", passwordEncoder.encode("password"), "Admin User", "admin@cookigram.com", Role.ROLE_ADMIN);
            userRepository.save(admin);
        }
        if (!userRepository.existsByUsername("employee1")) {
            User employee = new User("employee1", passwordEncoder.encode("password"), "Jake Baker", "employee@cookigram.com", Role.ROLE_EMPLOYEE);
            userRepository.save(employee);
        }
        if (!userRepository.existsByUsername("customer1")) {
            User customer = new User("customer1", passwordEncoder.encode("password"), "Sarah Customer", "customer@cookigram.com", Role.ROLE_CUSTOMER);
            userRepository.save(customer);
        }
        
        if (promotionRepository.count() == 0) {
            Promotion promo1 = new Promotion("Spring Special", "20% off All Orders",
                "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=400&h=300&fit=crop",
                LocalDate.now().plusMonths(1), true);
            promo1.setDiscountCode("SPRING20");
            promotionRepository.save(promo1);

            Promotion promo2 = new Promotion("Buy One, Get One Free", "This Week Only!",
                "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?w=400&h=300&fit=crop",
                LocalDate.now().plusWeeks(1), true);
            promo2.setDiscountCode("BOGO");
            promotionRepository.save(promo2);

            Promotion promo3 = new Promotion("Free Delivery", "On Orders Over $50",
                "https://images.unsplash.com/photo-1486427944299-d1955d23e34d?w=400&h=300&fit=crop",
                LocalDate.now().plusMonths(2), true);
            promo3.setDiscountCode("FREEDEL50");
            promotionRepository.save(promo3);
        }
    }
}
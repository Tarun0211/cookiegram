package com.cookigram.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    private String id;

    private String title;
    private String description;
    private String imageUrl;
    private LocalDate validUntil;
    private boolean isActive = true;
    private String discountCode;

    public Promotion(String title, String description, String imageUrl, LocalDate validUntil, boolean isActive) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.validUntil = validUntil;
        this.isActive = isActive;
    }
}
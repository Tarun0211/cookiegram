package com.cookigram.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class OrderFormDto {
    
    @NotNull(message = "Delivery date is required")
    @Future(message = "Delivery date must be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;
    
    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must be less than 200 characters")
    private String streetAddress;
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;
    
    @Size(max = 50, message = "Apartment/Suite must be less than 50 characters")
    private String apartmentSuite;
    
    @NotBlank(message = "Province is required")
    private String province;
    
    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$", message = "Invalid Canadian postal code format")
    private String postalCode;
    
    @NotBlank(message = "Custom message is required")
    @Size(max = 50, message = "Custom message must be 50 characters or less")
    private String customMessage;
    
    public String getFormattedPostalCode() {
        if (postalCode == null) return null;
        String clean = postalCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (clean.length() == 6) {
            return clean.substring(0, 3) + " " + clean.substring(3);
        }
        return postalCode.toUpperCase();
    }
}
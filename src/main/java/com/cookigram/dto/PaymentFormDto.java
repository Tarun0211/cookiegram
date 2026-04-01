package com.cookigram.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentFormDto {
    
    @NotBlank(message = "Cardholder name is required")
    @Size(max = 100)
    private String cardholderName;
    
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{13,19}$", message = "Invalid card number")
    private String cardNumber;
    
    @NotBlank(message = "Expiry date is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Invalid expiry date format (MM/YY)")
    private String expiryDate;
    
    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "Invalid CVV")
    private String cvv;
    
    private boolean saveCard;
    private boolean sameAsDelivery;
    private String billingStreet;
    private String billingCity;
    private String billingProvince;
    private String billingPostalCode;
    private String promoCode;
    
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
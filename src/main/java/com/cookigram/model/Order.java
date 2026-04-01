package com.cookigram.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String id;

    private User customer;

    private LocalDate deliveryDate;
    private String streetAddress;
    private String city;
    private String apartmentSuite;
    private String province;
    private String postalCode;
    private String customMessage;
    private BigDecimal price = new BigDecimal("24.99");
    private OrderStatus status = OrderStatus.PLACED;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @Indexed(unique = true)
    private String transactionId;

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder(streetAddress);
        if (apartmentSuite != null && !apartmentSuite.isEmpty()) sb.append(", ").append(apartmentSuite);
        sb.append(", ").append(city).append(", ").append(province).append(" ").append(postalCode);
        return sb.toString();
    }

    public String getTruncatedMessage() {
        if (customMessage == null) return "";
        return customMessage.length() <= 25 ? customMessage : customMessage.substring(0, 25) + "...";
    }

    public boolean canBeCancelled() {
        if (status != OrderStatus.PLACED) return false;
        return deliveryDate.isAfter(LocalDate.now().plusDays(3));
    }
}
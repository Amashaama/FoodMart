package lk.sa.foodmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderId;
    private String userId;

    private String fullName;
    private String email;
    private String phone;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;

    private String deliveryType;
    private double deliveryFee;

    private String paymentMethod;
    private String paymentReference;

    private double subtotal;
    private double total;

    private String status;
    private long createdAt;
}
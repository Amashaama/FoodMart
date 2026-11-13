package lk.sa.foodmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrderDisplay {
    private String orderId;
    private String buyerName;
    private String buyerPhone;
    private String status;
    private double total;
    private long createdAt;
    private int sellerItemCount;

}

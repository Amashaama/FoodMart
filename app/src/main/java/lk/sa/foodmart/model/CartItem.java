package lk.sa.foodmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private String itemId;
    private String sellerId;
    private String itemName;
    private double itemPrice;
    private int quantity;
    private String portionSize;
    private String imageUrl;
    private boolean available;
    private long addedAt;

}

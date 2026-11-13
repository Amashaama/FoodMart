package lk.sa.foodmart.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem {

    private String itemId;
    private String sellerId;
    private String itemName;
    private String itemDescription;
    private String categoryId;
    private String categoryName;
    private double itemPrice;
    private int quantity;
    private String portionSize;
    private boolean available;
    private List<String> imageUris;

    private String sellerAddress;
    private String sellerCity;
    private double sellerLatitude;
    private double sellerLongitude;
    private String sellerName;
    private long createdAt;

    private int soldCount;

}

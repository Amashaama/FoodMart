package lk.sa.foodmart.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private String id;
    private String name;
    private boolean active;
    private String imageUrl;

}

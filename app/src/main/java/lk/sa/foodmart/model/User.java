package lk.sa.foodmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String profilePicUrl;
    private String address;
    private String city;
    private Double longitude;
    private Double latitude;

    private double distanceKm;
}

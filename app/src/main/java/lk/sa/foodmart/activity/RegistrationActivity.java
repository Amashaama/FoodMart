package lk.sa.foodmart.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ActivityRegistrationBinding;
import lk.sa.foodmart.model.User;

public class RegistrationActivity extends AppCompatActivity {

    private ActivityRegistrationBinding binding;
    private FirebaseAuth firebaseAuth;

    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;

    private Uri imageUri;

    private static final int MAP_REQUEST_CODE =500;

    private Double selectedLatitude;
    private Double selectedLongitude;
    private String selectedAddress;

    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        binding.signupBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.signupLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.signupFabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);

                activityResultLauncher.launch(intent);
            }
        });

        binding.btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, MapsActivity.class);
            startActivityForResult(intent,MAP_REQUEST_CODE);
        });

        binding.btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.signupName.getText().toString().trim();
                String email = binding.signupEmail.getText().toString().trim();
                String phone=binding.signupPhone.getText().toString().trim();
                String password = binding.signupPassword.getText().toString().trim();
                String confirmPassword = binding.signupConfirmPassword.getText().toString().trim();
                String address = binding.signupAddress.getText().toString().trim();

                if(imageUri == null){
                    Toast.makeText(RegistrationActivity.this,
                            "Please select a profile image",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (name.isEmpty()) {
                    binding.signupName.setError("Name is required");
                    binding.signupName.requestFocus();
                    return;
                }

                if (email.isEmpty()) {
                    binding.signupEmail.setError("Email is required");
                    binding.signupEmail.requestFocus();
                    return;
                }

                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    binding.signupEmail.setError("Enter valid Email");
                    binding.signupEmail.requestFocus();
                    return;
                }

                if (phone.isEmpty()) {
                    binding.signupPhone.setError("Phone number is required");
                    binding.signupPhone.requestFocus();
                    return;
                }


                String srilankanPhoneRegex = "^(?:0|94|\\+94)?(?:7(0|1|2|4|5|6|7|8)\\d{7})$";
                if (!phone.matches(srilankanPhoneRegex)) {
                    binding.signupPhone.setError("Enter a valid Phone number");
                    binding.signupPhone.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    binding.signupPassword.setError("Password Required");
                    binding.signupPassword.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    binding.signupPassword.setError("Password must be 6 characters long");
                    binding.signupPassword.requestFocus();
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    binding.signupConfirmPassword.setError("Confirm Password Required");
                    binding.signupConfirmPassword.requestFocus();
                    return;
                }

                if (!confirmPassword.equals(password)) {
                    binding.signupConfirmPassword.setError("Confirm Password doesn't match with Password");
                    binding.signupConfirmPassword.requestFocus();
                    return;
                }

                if(address.isEmpty()){
                    binding.signupAddress.setError("Please select your Location");
                    binding.signupAddress.requestFocus();
                    return;
                }


                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        if(imageUri != null){
                            firebaseStorage =FirebaseStorage.getInstance();
                           StorageReference storageReference= firebaseStorage.getReference("profile-images")
                                    .child(uid+".jpg");

                           storageReference.putFile(imageUri)
                                   .addOnSuccessListener(taskSnapshot -> {
                                       storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                           String imageUrl = uri.toString();

                                           User user = User.builder()
                                                   .uid(uid)
                                                   .name(name)
                                                   .email(email)
                                                   .phone(phone)
                                                   .password(password)
                                                   .profilePicUrl(imageUrl)
                                                   .address(address)
                                                   .city(city)
                                                   .latitude(selectedLatitude)
                                                   .longitude(selectedLongitude)
                                                   .build();


                                           firebaseFirestore.collection("users")
                                                   .document(uid)
                                                   .set(user)
                                                   .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                       @Override
                                                       public void onSuccess(Void unused) {
                                                           Toast.makeText(getApplicationContext(), "Account Registration Success !", Toast.LENGTH_SHORT).show();
                                                           
                                                           // sign out to  make new user log in
                                                           firebaseAuth.signOut();

                                                           Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                                           startActivity(intent);
                                                           finish();
                                                       }
                                                   }).addOnFailureListener(new OnFailureListener() {
                                                       @Override
                                                       public void onFailure(@NonNull Exception e) {
                                                           Toast.makeText(getApplicationContext(), "Registration Saving Failed", Toast.LENGTH_SHORT).show();

                                                       }
                                                   });

                                       });
                                   });

                        }






                    }else{
                        Toast.makeText(RegistrationActivity.this,"Registration Failed: "+task.getException(),Toast.LENGTH_SHORT).show();
                    }

                });


            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null){
            selectedLatitude=data.getDoubleExtra("latitude",0.0);
            selectedLongitude=data.getDoubleExtra("longitude",0.0);
            selectedAddress = data.getStringExtra("address");
            city=data.getStringExtra("city");

            binding.signupAddress.setText(selectedAddress);
            binding.tvSelectedLocation.setText("Lat: "+ selectedLatitude+", Lng: "+ selectedLongitude);

        }
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RegistrationActivity.RESULT_OK) {
                   imageUri= result.getData().getData();

                    Glide.with(RegistrationActivity.this)
                            .load(imageUri)
                            .circleCrop()
                            .into(binding.signupProfileImage);



                }
            }
    );


}

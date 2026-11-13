package lk.sa.foodmart.fragment;

import android.location.Address;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.databinding.FragmentMyProfileBinding;
import lk.sa.foodmart.model.User;

public class MyProfileFragment extends Fragment {

  private FragmentMyProfileBinding binding;
  private FirebaseAuth auth;
  private FirebaseFirestore firestore;
  private FirebaseStorage storage;

  private Uri selectedImageUri = null;
  private String currentProfileImageUrl = "";

  private final ActivityResultLauncher<String> imagePickerLauncher =
      registerForActivityResult(
          new ActivityResultContracts.GetContent(),
          uri -> {
            if (uri != null) {
              selectedImageUri = uri;
              binding.ivProfile.setImageURI(uri);
            }
          });

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentMyProfileBinding.inflate(inflater, container, false);
    auth = FirebaseAuth.getInstance();
    firestore = FirebaseFirestore.getInstance();
    storage = FirebaseStorage.getInstance();

    setupClicks();
    loadProfile();

    return binding.getRoot();
  }


    private void setupClicks(){
    binding.btnBack.setOnClickListener(
        v -> {
          if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                .navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
          }
        });

      binding.btnEditImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
      binding.btnSave.setOnClickListener(v -> saveProfile());
  }

    private void loadProfile() {
      if(auth.getCurrentUser() == null) return;

      String uid =auth.getCurrentUser().getUid();

      firestore.collection("users")
              .document(uid)
              .get()
              .addOnSuccessListener(documentSnapshot -> {
                  User user = documentSnapshot.toObject(User.class);

                  if(user != null){
                      binding.etName.setText(user.getName() != null ? user.getName(): "");
                      binding.etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                      binding.etPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                      binding.etAddress.setText(user.getAddress() != null ? user.getAddress() : "");
                      binding.etCity.setText(user.getCity() != null ? user.getCity() : "");

                      currentProfileImageUrl = user.getProfilePicUrl() != null ? user.getProfilePicUrl():"";

                      if(!currentProfileImageUrl.isEmpty()){
                          Glide.with(requireContext())
                                  .load(currentProfileImageUrl)
                                  .placeholder(R.drawable.baseline_image_24)
                                  .error(R.drawable.baseline_image_24)
                                  .centerCrop()
                                  .into(binding.ivProfile);
                      }
                  }
              }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
    }
    private void saveProfile() {
      if(auth.getCurrentUser() == null){
          Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
          return;
      }

        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
        String address = binding.etAddress.getText() != null ? binding.etAddress.getText().toString().trim() : "";
        String city = binding.etCity.getText() != null ? binding.etCity.getText().toString().trim() : "";

        if(name.isEmpty()){
            binding.tilName.setError("Name is required");
            return;
        }else{
            binding.tilName.setError(null);
        }

        if(phone.isEmpty()){
            binding.tilPhone.setError("Phone Number is required");
            return;
        }else{
            binding.tilPhone.setError(null);
        }

        if(address.isEmpty()){
            binding.tilAddress.setError("Address is required");
            return;
        }else{
            binding.tilAddress.setError(null);
        }

        if(city.isEmpty()){
            binding.tilCity.setError("City is required");
            return;
        }else{
            binding.tilCity.setError(null);
        }

        if(selectedImageUri != null){
            uploadProfileImageAndSave(name,phone,address,city);
        }else{
            updateProfileData(name,phone,address,city,currentProfileImageUrl);
        }







    }

    private void uploadProfileImageAndSave(String name, String phone, String address, String city) {
      String uid = auth.getCurrentUser().getUid();

        StorageReference imageRef = storage.getReference()
                .child("profile_images")
                .child(uid+".jpg");

        imageRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateProfileData(name,phone,address,city,imageUrl);
                })
                .addOnFailureListener(e ->   Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show());
    }

    private void updateProfileData(String name, String phone, String address, String city, String imageUrl) {
      String uid = auth.getCurrentUser().getUid();

      Map<String,Object> updateMap = new HashMap<>();
      updateMap.put("name",name);
      updateMap.put("phone",phone);
      updateMap.put("address",address);
      updateMap.put("city",city);
      updateMap.put("profilePicUrl",imageUrl);

      firestore.collection("users")
              .document(uid)
              .update(updateMap)
              .addOnSuccessListener(unused -> {
                  Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                  if(requireActivity() instanceof MainActivity){
                      ((MainActivity) requireActivity()).loadProfileDetailsOnSideNav();
                  }
              })  .addOnFailureListener(e ->
                      Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
              );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package lk.sa.foodmart.fragment;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.SelectedPhotoAdapter;
import lk.sa.foodmart.databinding.FragmentSellerAddItemBinding;

public class SellerAddItemFragment extends Fragment {

    private FragmentSellerAddItemBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private SelectedPhotoAdapter selectedPhotoAdapter;

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri mainImageUri;
    private boolean isImagePickerOpen = false;

    private String sellerAddress = "";
    private String sellerCity = "";
    private double sellerLatitude = 0.0;
    private double sellerLongitude = 0.0;

    private final ArrayList<String> categoryNames = new ArrayList<>();
    private final ArrayList<String> categoryIds = new ArrayList<>();

    private String selectedCategoryId = "";
    private String sellerName = "";

    private boolean isEditMode = false;
    private String editingItemId = "";

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSellerAddItemBinding.inflate(inflater, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("isEdit", false);
            editingItemId = getArguments().getString("itemId", "");
        }

        if (isEditMode) {
            binding.tvItemTitle.setText("Edit Food Item");
            binding.btnSave.setText("Update Item");
        } else {
            binding.tvItemTitle.setText("Add New Food Item");
            binding.btnSave.setText("Save Item");
        }

        setupRecyclerView();
        setupImagePicker();
        setupListeners();
        loadCategories();
        loadSellerLocationDate();
        setupErrorClearListeners();

        if (isEditMode) {
            loadItemForEdit();
        }

        return binding.getRoot();
    }

    private void setupErrorClearListeners() {
        addTextWatcher(binding.etItemName, () -> { if (binding != null) binding.tilItemName.setError(null); });
        addTextWatcher(binding.etDescription, () -> { if (binding != null) binding.tilDescription.setError(null); });
        addTextWatcher(binding.etBasePrice, () -> { if (binding != null) binding.tilBasePrice.setError(null); });
        addTextWatcher(binding.etQuantity, () -> { if (binding != null) binding.tilQuantity.setError(null); });
        addTextWatcher(binding.actCategory, () -> { if (binding != null) binding.tilCategory.setError(null); });
    }

    private void addTextWatcher(android.widget.TextView textView, Runnable onTextChanged) {
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                onTextChanged.run();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupListeners() {
        binding.cardMainImage.setOnClickListener(v -> {
            if (isImagePickerOpen) return;
            isImagePickerOpen = true;
            imagePickerLauncher.launch("image/*");
        });

        binding.btnSave.setOnClickListener(v -> validateAndSaveItem());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    isImagePickerOpen = false;
                    if (binding == null || uris == null || uris.isEmpty()) return;

                    int availableSlots = 3 - selectedImageUris.size();
                    if (availableSlots <= 0) {
                        Toast.makeText(getContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int countToAdd = Math.min(uris.size(), availableSlots);
                    for (int i = 0; i < countToAdd; i++) {
                        Uri newUri = uris.get(i);
                        if (!selectedImageUris.contains(newUri)) {
                            selectedImageUris.add(newUri);
                        }
                    }
                    updatePhotoUI();
                });
    }

    private void setupRecyclerView() {
        if (binding == null) return;
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        selectedPhotoAdapter = new SelectedPhotoAdapter(
                selectedImageUris,
                position -> {
                    if (position >= 0 && position < selectedImageUris.size()) {
                        selectedImageUris.remove(position);
                        updatePhotoUI();
                    }
                });
        binding.rvPhotos.setAdapter(selectedPhotoAdapter);
    }

    private void updatePhotoUI() {
        if (binding == null) return;
        binding.tvPhotoCount.setText(selectedImageUris.size() + " /3 Images");
        if (!selectedImageUris.isEmpty()) {
            mainImageUri = selectedImageUris.get(0);
            binding.ivMainPhoto.setVisibility(View.VISIBLE);
            binding.layoutUploadPlaceholder.setVisibility(View.GONE);
            binding.ivPhotoAddImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(mainImageUri).centerCrop().into(binding.ivMainPhoto);
            binding.rvPhotos.setVisibility(View.VISIBLE);
        } else {
            mainImageUri = null;
            binding.ivMainPhoto.setVisibility(View.GONE);
            binding.layoutUploadPlaceholder.setVisibility(View.VISIBLE);
            binding.ivPhotoAddImage.setVisibility(View.GONE);
            binding.rvPhotos.setVisibility(View.GONE);
        }

        if (selectedPhotoAdapter != null) {
            selectedPhotoAdapter.notifyDataSetChanged();
        }
    }

    private void loadSellerLocationDate() {
        if (auth.getCurrentUser() == null) return;
        String sellerId = auth.getCurrentUser().getUid();
        firestore.collection("users").document(sellerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    sellerAddress = documentSnapshot.getString("address") != null ? documentSnapshot.getString("address") : "";
                    sellerCity = documentSnapshot.getString("city") != null ? documentSnapshot.getString("city") : "";
                    Double lat = documentSnapshot.getDouble("latitude");
                    Double lng = documentSnapshot.getDouble("longitude");
                    if (lat != null) sellerLatitude = lat;
                    if (lng != null) sellerLongitude = lng;
                    sellerName = documentSnapshot.getString("name");
                });
    }

    private void loadCategories() {
        categoryNames.clear();
        categoryIds.clear();
        firestore.collection("categories").whereEqualTo("active", true).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("name");
                        if (name != null) {
                            categoryIds.add(id);
                            categoryNames.add(name);
                        }
                    }
                    if (getContext() != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categoryNames);
                        binding.actCategory.setAdapter(adapter);
                        binding.actCategory.setOnItemClickListener((parent, view, position, id) -> {
                            selectedCategoryId = categoryIds.get(position);
                            binding.actCategory.setText(categoryNames.get(position), false);
                            binding.tilCategory.setError(null);
                        });
                    }
                });
    }

    private void validateAndSaveItem() {
        if (binding == null) return;
        clearErrors();

        String itemName = binding.etItemName.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String categoryName = binding.actCategory.getText().toString().trim();
        String basePriceStr = binding.etBasePrice.getText().toString().trim();
        String quantityStr = binding.etQuantity.getText().toString().trim();
        String portionSize = getSelectedPortionSize();

        boolean valid = true;
        if (itemName.isEmpty()) { binding.tilItemName.setError("Item name is required"); valid = false; }
        if (description.isEmpty()) { binding.tilDescription.setError("Description is required"); valid = false; }
        if (categoryName.isEmpty() || selectedCategoryId.isEmpty()) { binding.tilCategory.setError("Select a category"); valid = false; }
        if (basePriceStr.isEmpty()) { binding.tilBasePrice.setError("Price is required"); valid = false; }
        if (quantityStr.isEmpty()) { binding.tilQuantity.setError("Quantity is required"); valid = false; }
        if (portionSize.isEmpty()) { Toast.makeText(requireContext(), "Select portion size", Toast.LENGTH_SHORT).show(); valid = false; }
        if (selectedImageUris.isEmpty()) { Toast.makeText(requireContext(), "Select at least one image", Toast.LENGTH_SHORT).show(); valid = false; }

        if (!valid) return;

        try {
            double price = Double.parseDouble(basePriceStr);
            int qty = Integer.parseInt(quantityStr);
            saveItem(itemName, description, categoryName, price, qty, portionSize);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid price or quantity", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveItem(String itemName, String description, String categoryName, double itemPrice, int quantity, String portionSize) {
        if (auth.getCurrentUser() == null) return;
        String sellerId = auth.getCurrentUser().getUid();
        DocumentReference itemRef = isEditMode ? firestore.collection("food_items").document(editingItemId) : firestore.collection("food_items").document();
        String itemId = itemRef.getId();

        uploadImagesAndSaveData(itemRef, itemId, sellerId, itemName, description, categoryName, itemPrice, quantity, portionSize);
    }

    private void uploadImagesAndSaveData(DocumentReference itemRef, String itemId, String sellerId, String itemName, String description, String categoryName, double itemPrice, int quantity, String portionSize) {
        final int totalImages = selectedImageUris.size();
        final String[] uploadedUrls = new String[totalImages];
        final int[] completedCount = {0};

        for (int i = 0; i < totalImages; i++) {
            final int index = i;
            Uri uri = selectedImageUris.get(i);

            if (uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
                uploadedUrls[index] = uri.toString();
                handleUploadComplete(completedCount, totalImages, itemRef, itemId, sellerId, itemName, description, categoryName, itemPrice, quantity, portionSize, uploadedUrls);
            } else {
                StorageReference imageRef = storage.getReference().child("item_images").child(sellerId).child(itemId).child("image_" + System.currentTimeMillis() + "_" + index + ".jpg");
                imageRef.putFile(uri).continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return imageRef.getDownloadUrl();
                }).addOnSuccessListener(downloadUri -> {
                    uploadedUrls[index] = downloadUri.toString();
                    handleUploadComplete(completedCount, totalImages, itemRef, itemId, sellerId, itemName, description, categoryName, itemPrice, quantity, portionSize, uploadedUrls);
                });
            }
        }
    }

    private void handleUploadComplete(int[] completedCount, int totalImages, DocumentReference itemRef, String itemId, String sellerId, String itemName, String description, String categoryName, double itemPrice, int quantity, String portionSize, String[] uploadedUrls) {
        completedCount[0]++;
        if (completedCount[0] == totalImages) {
            saveItemDataToFirestore(itemRef, itemId, sellerId, itemName, description, categoryName, itemPrice, quantity, portionSize, Arrays.asList(uploadedUrls));
        }
    }

    private void saveItemDataToFirestore(DocumentReference itemRef, String itemId, String sellerId, String itemName, String description, String categoryName, double itemPrice, int quantity, String portionSize, List<String> imageUris) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("itemId", itemId);
        itemMap.put("sellerId", sellerId);
        itemMap.put("itemName", itemName);
        itemMap.put("itemDescription", description);
        itemMap.put("categoryId", selectedCategoryId);
        itemMap.put("categoryName", categoryName);
        itemMap.put("itemPrice", itemPrice);
        itemMap.put("quantity", quantity);
        itemMap.put("portionSize", portionSize);
        itemMap.put("imageUris", imageUris);
        itemMap.put("sellerAddress", sellerAddress);
        itemMap.put("sellerCity", sellerCity);
        itemMap.put("sellerLatitude", sellerLatitude);
        itemMap.put("sellerLongitude", sellerLongitude);
        itemMap.put("available", quantity > 0);

        if (!isEditMode) {
            itemMap.put("createdAt", System.currentTimeMillis());
            itemMap.put("soldCount", 0);
        } else {
            itemMap.put("updatedAt", System.currentTimeMillis());
        }

        itemRef.set(itemMap, SetOptions.merge()).addOnSuccessListener(unused -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), isEditMode ? "Item updated" : "Item saved", Toast.LENGTH_SHORT).show();
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToFragment(new SellerMyShopFragment(), R.id.bottom_nav_my_shop, R.id.side_nav_seller_my_shop, false);
                }
            }
        });
    }

    private String getSelectedPortionSize() {
        if (binding == null) return "";
        int id = binding.rgPortionSize.getCheckedRadioButtonId();
        if (id == binding.rbSmall.getId()) return "Small";
        if (id == binding.rbRegular.getId()) return "Regular";
        if (id == binding.rbLarge.getId()) return "Large";
        if (id == binding.rbFamily.getId()) return "Family";
        return "";
    }

    private void loadItemForEdit() {
        if (editingItemId == null || editingItemId.isEmpty()) return;
        firestore.collection("food_items").document(editingItemId).get().addOnSuccessListener(doc -> {
            if (binding == null || !doc.exists()) return;
            binding.etItemName.setText(doc.getString("itemName"));
            binding.etDescription.setText(doc.getString("itemDescription"));
            binding.actCategory.setText(doc.getString("categoryName"), false);
            selectedCategoryId = doc.getString("categoryId");
            Double price = doc.getDouble("itemPrice");
            binding.etBasePrice.setText(price != null ? String.valueOf(price) : "");
            Long qty = doc.getLong("quantity");
            binding.etQuantity.setText(qty != null ? String.valueOf(qty) : "");

            String portion = doc.getString("portionSize");
            if (portion != null) {
                switch (portion) {
                    case "Small": binding.rbSmall.setChecked(true); break;
                    case "Regular": binding.rbRegular.setChecked(true); break;
                    case "Large": binding.rbLarge.setChecked(true); break;
                    case "Family": binding.rbFamily.setChecked(true); break;
                }
            }

            List<String> urls = (List<String>) doc.get("imageUris");
            if (urls != null) {
                selectedImageUris.clear();
                for (String url : urls) {
                    selectedImageUris.add(Uri.parse(url));
                }
                updatePhotoUI();
            }
        });
    }

    private void clearErrors() {
        if (binding == null) return;
        binding.tilItemName.setError(null);
        binding.tilDescription.setError(null);
        binding.tilCategory.setError(null);
        binding.tilBasePrice.setError(null);
        binding.tilQuantity.setError(null);
    }

    private void clearForm() {
        if (binding == null) return;
        binding.etItemName.setText("");
        binding.etDescription.setText("");
        binding.actCategory.setText("");
        binding.etBasePrice.setText("");
        binding.etQuantity.setText("");
        binding.rgPortionSize.clearCheck();
        selectedImageUris.clear();
        updatePhotoUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package lk.sa.foodmart.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.databinding.FragmentSingleProductBinding;
import lk.sa.foodmart.model.FoodItem;
import lk.sa.foodmart.model.User;

public class SingleProductFragment extends Fragment {

  private FragmentSingleProductBinding binding;
  private FirebaseFirestore firestore;
  private FirebaseAuth auth;

  private String itemId = "";
  private FoodItem currentItem;
  private int selectedQuantity = 1;

  private final ArrayList<String> productImages = new ArrayList<>();

  private String sellerPhone = "";


  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentSingleProductBinding.inflate(inflater, container, false);
    firestore = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    if (getArguments() != null) {
      itemId = getArguments().getString("itemId", "");


    }

    setupClicks();
    loadProduct();

    return binding.getRoot();
  }

  private void setupClicks() {
    binding.btnBack.setOnClickListener(
        v -> {


            requireActivity().getSupportFragmentManager().popBackStack();

        });

    binding.btnIncrease.setOnClickListener(
        v -> {
          if (currentItem == null) {
            return;
          }

          if (selectedQuantity < currentItem.getQuantity()) {
            selectedQuantity++;
            updateQuantityUI();
          }
        });

    binding.btnDecrease.setOnClickListener(
        v -> {
          if (currentItem == null) return;

          if (selectedQuantity > 1) {
            selectedQuantity--;

            updateQuantityUI();
          }
        });



    binding.btnAddToCart.setOnClickListener(v -> addToCart());

    binding.btnCallSeller.setOnClickListener(v -> callSeller());
  }

    private void callSeller() {
      if(auth.getCurrentUser() == null){
          Toast.makeText(requireContext(),"Please Login First",Toast.LENGTH_SHORT).show();
          return;
      }

      if(sellerPhone == null || sellerPhone.isEmpty()){
          Toast.makeText(requireContext(),"Seller Phone Number Not Available",Toast.LENGTH_SHORT).show();
          return;
      }

      Intent intent = new Intent(Intent.ACTION_DIAL);
      intent.setData(Uri.parse("tel:"+sellerPhone));
      startActivity(intent);
    }

    private void updateQuantityUI() {
    binding.tvQuantity.setText(String.valueOf(selectedQuantity));
  }

  private void addToCart() {
    if (auth.getCurrentUser() == null) {
      Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
      return;
    }

    if (currentItem == null) {
      Toast.makeText(requireContext(), "Product not loaded", Toast.LENGTH_SHORT).show();
      return;
    }

    String userId = auth.getCurrentUser().getUid();
    String itemId = currentItem.getItemId();

    if (itemId == null || itemId.isEmpty()) {
      Toast.makeText(requireContext(), "Invalid product", Toast.LENGTH_SHORT).show();
      return;
    }

    firestore
        .collection("carts")
        .document(userId)
        .collection("items")
        .document(itemId)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              if (documentSnapshot.exists()) {
                Long existingQtyLong = documentSnapshot.getLong("quantity");
                int existingQty = existingQtyLong != null ? existingQtyLong.intValue() : 0;

                int newQty = existingQty + selectedQuantity;
                if (newQty > currentItem.getQuantity()) {
                  Toast.makeText(
                          requireContext(), "Cannot exceed available stock", Toast.LENGTH_SHORT)
                      .show();
                  return;
                }

                firestore
                    .collection("carts")
                    .document(userId)
                    .collection("items")
                    .document(itemId)
                    .update("quantity", newQty)
                    .addOnSuccessListener(
                        unused -> {
                          Toast.makeText(requireContext(), "Cart updated", Toast.LENGTH_SHORT)
                              .show();

                          if(requireActivity() instanceof MainActivity){
                              ((MainActivity) requireActivity()).countCartItems();
                          }
                        })
                    .addOnFailureListener(
                        e -> {
                          Toast.makeText(
                                  requireContext(), "Failed to update cart", Toast.LENGTH_SHORT)
                              .show();
                        });
              } else {
                String imageUrl = "";
                if (currentItem.getImageUris() != null && !currentItem.getImageUris().isEmpty()) {
                  imageUrl = currentItem.getImageUris().get(0);
                }

                HashMap<String, Object> cartMap = new HashMap<>();
                cartMap.put("itemId", currentItem.getItemId());
                cartMap.put("sellerId", currentItem.getSellerId());
                cartMap.put("itemName", currentItem.getItemName());
                cartMap.put("itemPrice", currentItem.getItemPrice());
                cartMap.put("quantity", selectedQuantity);
                cartMap.put("portionSize", currentItem.getPortionSize());
                cartMap.put("imageUrl", imageUrl);
                cartMap.put("available", currentItem.isAvailable());
                cartMap.put("addedAt", System.currentTimeMillis());

                firestore
                    .collection("carts")
                    .document(userId)
                    .collection("items")
                    .document(itemId)
                    .set(cartMap)
                    .addOnSuccessListener(
                        unused -> {
                          Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT)
                              .show();
                            if(requireActivity() instanceof MainActivity){
                                ((MainActivity) requireActivity()).countCartItems();
                            }
                        })
                    .addOnFailureListener(
                        e -> {
                          Toast.makeText(
                                  requireContext(), "Failed to add to cart", Toast.LENGTH_SHORT)
                              .show();
                        });
              }
            })
        .addOnFailureListener(
            e -> {
              Toast.makeText(requireContext(), "Cart operation failed", Toast.LENGTH_SHORT).show();
            });
  }

  private void loadProduct() {
    if (itemId.isEmpty()) {
      Toast.makeText(requireContext(), "Invalid product", Toast.LENGTH_SHORT).show();
      return;
    }

    firestore
        .collection("food_items")
        .document(itemId)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              if (documentSnapshot.exists()) {
                currentItem = documentSnapshot.toObject(FoodItem.class);

                if (currentItem != null) {
                  currentItem.setItemId(documentSnapshot.getId());
                  bindProductData(currentItem);
                  loadSellerInfo(currentItem.getSellerId());
                }
              } else {
                Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show();
              }
            })
        .addOnFailureListener(
            e -> {
              Toast.makeText(requireContext(), "Failed to load product", Toast.LENGTH_SHORT).show();
            });
  }

  private void bindProductData(FoodItem item) {

    binding.tvItemName.setText(item.getItemName());

    binding.tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", item.getItemPrice()));
    binding.tvStockQty.setText(String.valueOf(item.getQuantity()));
    binding.tvDescription.setText(item.getItemDescription());
    binding.chipCategory.setText(item.getCategoryName());
    binding.chipPortion.setText(item.getPortionSize());

    if (item.getQuantity() > 0) {
      binding.tvStockStatus.setText("In Stock");
    } else {
      binding.tvStockStatus.setText("Out of Stock");
    }

    productImages.clear();
    if (item.getImageUris() != null) {
      productImages.addAll(item.getImageUris());
    }

    displayImages();
    setupImageClicks();
  }

  private void setupImageClicks() {
    binding.cardImage2.setOnClickListener(v -> swapImages(1));
    binding.cardImage3.setOnClickListener(v -> swapImages(2));
  }

  private void swapImages(int clickedIndex) {
    if (productImages.size() <= clickedIndex) {
      return;
    }

    String temp = productImages.get(0);
    productImages.set(0, productImages.get(clickedIndex));
    productImages.set(clickedIndex, temp);

    displayImages();
  }

  private void displayImages() {
    if (!productImages.isEmpty()) {

      if (productImages.get(0) != null && !productImages.get(0).isEmpty()) {
        Glide.with(this)
            .load(productImages.get(0))
            .placeholder(R.drawable.baseline_image_24)
            .error(R.drawable.baseline_image_24)
            .centerCrop()
            .into(binding.ivMainImage);
      }

      if (productImages.size() > 1
          && productImages.get(1) != null
          && !productImages.get(1).isEmpty()) {
        binding.cardImage2.setVisibility(View.VISIBLE);
        binding.ivImage2.setVisibility(View.VISIBLE);

        Glide.with(this)
            .load(productImages.get(1))
            .placeholder(R.drawable.baseline_image_24)
            .error(R.drawable.baseline_image_24)
            .centerCrop()
            .into(binding.ivImage2);
      } else {
        binding.cardImage2.setVisibility(View.GONE);
      }

      if (productImages.size() > 2
          && productImages.get(2) != null
          && !productImages.get(2).isEmpty()) {
        binding.cardImage3.setVisibility(View.VISIBLE);
        binding.ivImage3.setVisibility(View.VISIBLE);

        Glide.with(this)
            .load(productImages.get(2))
            .placeholder(R.drawable.baseline_image_24)
            .error(R.drawable.baseline_image_24)
            .centerCrop()
            .into(binding.ivImage3);
      } else {
        binding.cardImage3.setVisibility(View.GONE);
      }
    }
  }

  private void loadSellerInfo(String sellerId) {
    if (sellerId.isEmpty()) {
      return;
    }

    firestore
        .collection("users")
        .document(sellerId)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              User seller = documentSnapshot.toObject(User.class);

              if (seller != null) {
                binding.tvSellerNameCard.setText(seller.getName());
                sellerPhone = seller.getPhone() != null ? seller.getPhone(): "";
                if (seller.getCity() != null && !seller.getCity().isEmpty()) {
                  binding.tvSellerLocation.setText(seller.getCity());
                } else if (seller.getAddress() != null) {
                  binding.tvSellerLocation.setText(seller.getAddress());
                }

                if (seller.getProfilePicUrl() != null && !seller.getProfilePicUrl().isEmpty()) {
                  Glide.with(requireContext())
                      .load(seller.getProfilePicUrl())
                      .placeholder(R.drawable.baseline_image_24)
                      .error(R.drawable.baseline_image_24)
                      .into(binding.ivSeller);
                }
              }
            });
  }
}

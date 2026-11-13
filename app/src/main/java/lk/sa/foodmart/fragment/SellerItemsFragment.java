package lk.sa.foodmart.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.SearchFoodAdapter;
import lk.sa.foodmart.databinding.FragmentSellerItemsBinding;
import lk.sa.foodmart.model.FoodItem;

public class SellerItemsFragment extends Fragment {

  private FragmentSellerItemsBinding binding;
  private FirebaseFirestore firestore;
  private FirebaseAuth auth;

  private final ArrayList<FoodItem> sellerItems = new ArrayList<>();
  private SearchFoodAdapter adapter;

  private String sellerId;
  private String sellerName = "Seller Items";

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentSellerItemsBinding.inflate(inflater, container, false);

    firestore = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    if (getArguments() != null) {
      sellerId = getArguments().getString("sellerId", "");
      sellerName = getArguments().getString("sellerName", "Seller Items");
    }

    binding.tvTitle.setText("Seller Name: "+sellerName);
    binding.btnBack.setOnClickListener(
        v -> {
          requireActivity().getSupportFragmentManager().popBackStack();
        });

    setupRecyclerView();
    loadSellerItems();

    // Inflate the layout for this fragment
    return binding.getRoot();
  }

    private void loadSellerItems() {
      binding.layoutEmpty.setVisibility(View.GONE);

      firestore.collection("food_items")
              .whereEqualTo("available",true)
              .whereEqualTo("sellerId",sellerId)
              .get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
                  sellerItems.clear();
                  for(QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots){
                      FoodItem item = documentSnapshot.toObject(FoodItem.class);
                      if(item != null){
                          sellerItems.add(item);
                      }
                  }

                  Collections.sort(sellerItems,(a,b)-> Long.compare(b.getCreatedAt(),a.getCreatedAt()));
                  adapter.notifyDataSetChanged();
                  binding.layoutEmpty.setVisibility(sellerItems.isEmpty()? View.VISIBLE: View.GONE);
              })
              .addOnFailureListener(e -> {
                  Toast.makeText(requireContext(), "Failed to load seller items", Toast.LENGTH_SHORT).show();
              });
    }

    private void setupRecyclerView() {
    binding.rvSellerItems.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    binding.rvSellerItems.setNestedScrollingEnabled(false);

    adapter =
        new SearchFoodAdapter(
            sellerItems,
            item -> {
              SingleProductFragment singleProductFragment = new SingleProductFragment();
              Bundle bundle = new Bundle();
              bundle.putString("itemId", item.getItemId());
              singleProductFragment.setArguments(bundle);

              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToFragment(singleProductFragment,-1,-1,true);
              }
            },
            currentItem -> {
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
                          int existingQty =
                              existingQtyLong != null ? existingQtyLong.intValue() : 0;

                          int newQty = existingQty + 1;
                          if (newQty > currentItem.getQuantity()) {
                            Toast.makeText(
                                    requireContext(),
                                    "Cannot exceed available stock",
                                    Toast.LENGTH_SHORT)
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
                                    Toast.makeText(
                                            requireContext(), "Cart updated", Toast.LENGTH_SHORT)
                                        .show();
                                  })
                              .addOnFailureListener(
                                  e -> {
                                    Toast.makeText(
                                            requireContext(),
                                            "Failed to update cart",
                                            Toast.LENGTH_SHORT)
                                        .show();
                                  });
                        } else {
                          String imageUrl = "";
                          if (currentItem.getImageUris() != null
                              && !currentItem.getImageUris().isEmpty()) {
                            imageUrl = currentItem.getImageUris().get(0);
                          }

                          HashMap<String, Object> cartMap = new HashMap<>();
                          cartMap.put("itemId", currentItem.getItemId());
                          cartMap.put("sellerId", currentItem.getSellerId());
                          cartMap.put("itemName", currentItem.getItemName());
                          cartMap.put("itemPrice", currentItem.getItemPrice());
                          cartMap.put("quantity", 1);
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
                                    Toast.makeText(
                                            requireContext(), "Added to cart", Toast.LENGTH_SHORT)
                                        .show();
                                  })
                              .addOnFailureListener(
                                  e -> {
                                    Toast.makeText(
                                            requireContext(),
                                            "Failed to add to cart",
                                            Toast.LENGTH_SHORT)
                                        .show();
                                  });
                        }
                      })
                  .addOnFailureListener(
                      e -> {
                        Toast.makeText(
                                requireContext(), "Cart operation failed", Toast.LENGTH_SHORT)
                            .show();
                      });
            });
    binding.rvSellerItems.setAdapter(adapter);
  }
}

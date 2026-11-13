package lk.sa.foodmart.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.BannerAdapter;
import lk.sa.foodmart.adapter.CategoryAdapter;
import lk.sa.foodmart.adapter.NearbySellerAdapter;
import lk.sa.foodmart.adapter.SearchFoodAdapter;
import lk.sa.foodmart.databinding.FragmentHomeBinding;
import lk.sa.foodmart.model.Category;
import lk.sa.foodmart.model.FoodItem;
import lk.sa.foodmart.model.User;

public class HomeFragment extends Fragment {

  private FragmentHomeBinding binding;
  private FirebaseFirestore firestore;
  private FirebaseStorage storage;
  private FirebaseAuth auth;

  private final ArrayList<String> bannerList = new ArrayList<>();
  private final ArrayList<Category> categoryList = new ArrayList<>();
  private final ArrayList<FoodItem> popularItemList = new ArrayList<>();
  private final ArrayList<User> nearbySellerList = new ArrayList<>();

  private BannerAdapter bannerAdapter;
  private CategoryAdapter categoryAdapter;
  private SearchFoodAdapter popularFoodAdapter;
  private NearbySellerAdapter nearbySellerAdapter;

  private double currentUserLat = 0.0;
  private double currentUserLng = 0.0;
  private boolean hasCurrentUserLocation = false;

  private static final Double NEARBY_DISTANCE_THRESHOLD =15.0;

  BottomNavigationView bottomNavigationView;
  NavigationView sideNavigationView;

  @Override
  public View onCreateView(
          LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentHomeBinding.inflate(inflater, container, false);

    firestore = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();
    storage = FirebaseStorage.getInstance();

    bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation_view);
    sideNavigationView = requireActivity().findViewById(R.id.side_navigation_view);

    setupRecyclerViews();
    setupBanners();
    setupClicks();
    loadCurrentUserLocationalData();

    // Inflate the layout for this fragment
    return binding.getRoot();
  }

  private void loadCurrentUserLocationalData() {
    if (auth.getCurrentUser() == null) {
      loadCategories();
      loadPopularItems();
      loadNearBySellers();
      return;
    }

    String uid = auth.getCurrentUser().getUid();

    firestore
        .collection("users")
        .document(uid)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              Double lat = documentSnapshot.getDouble("latitude");
              Double lng = documentSnapshot.getDouble("longitude");

              if (lat != null && lng != null) {
                currentUserLat = lat;
                currentUserLng = lng;
                hasCurrentUserLocation = true;
              }
              loadCategories();
              loadPopularItems();
              loadNearBySellers();
            })
        .addOnFailureListener(
            e -> {
              loadCategories();
              loadPopularItems();
              loadNearBySellers();
            });
  }

  private void loadNearBySellers() {

    firestore
        .collection("food_items")
        .whereEqualTo("available", true)
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              HashSet<String> sellerIds = new HashSet<>();
              String currentUserId = null;

              if (auth.getCurrentUser() != null) {
                currentUserId = auth.getCurrentUser().getUid();
              }

              for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String sellerId = doc.getString("sellerId");

                if (sellerId == null || sellerId.isEmpty()) continue;

                if (sellerId.equals(currentUserId)) {
                  continue;
                }

                sellerIds.add(sellerId);
              }

              firestore
                  .collection("users")
                  .get()
                  .addOnSuccessListener(
                      userSnapshots -> {
                        nearbySellerList.clear();
                        ArrayList<User> allActiveSellers = new ArrayList<>();
                        for (QueryDocumentSnapshot userDoc : userSnapshots) {
                          User seller = userDoc.toObject(User.class);

                          seller.setUid(userDoc.getId());

                          if (!sellerIds.contains(seller.getUid())) {
                            continue;
                          }

                          if (hasCurrentUserLocation
                              && seller.getLatitude() != null
                              && seller.getLongitude() != null) {
                            double distanceKm =
                                calculateDistanceKm(
                                    currentUserLat,
                                    currentUserLng,
                                    seller.getLatitude(),
                                    seller.getLongitude());
                            seller.setDistanceKm(distanceKm);

                            if(distanceKm <= NEARBY_DISTANCE_THRESHOLD){
                                nearbySellerList.add(seller);
                            }
                          }
                          allActiveSellers.add(seller);
                        }

                        if (hasCurrentUserLocation && !nearbySellerList.isEmpty()) {
                            binding.tvNearbySeller.setText("Nearby Sellers");
                          Collections.sort(
                              nearbySellerList,
                              (a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));
                        }else {
                            binding.tvNearbySeller.setText("Sellers");
                            nearbySellerList.clear();
                            nearbySellerList.addAll(allActiveSellers);
                        }

                        if (nearbySellerList.size() > 10) {
                          while (nearbySellerList.size() > 10) {
                            nearbySellerList.remove(nearbySellerList.size() - 1);
                          }
                        }

                        nearbySellerAdapter.notifyDataSetChanged();
                      })
                  .addOnFailureListener(
                      e ->
                          Toast.makeText(
                                  requireContext(), "Failed to load sellers", Toast.LENGTH_SHORT)
                              .show());
            })
        .addOnFailureListener(
            e ->
                Toast.makeText(requireContext(), "Failed to load seller items", Toast.LENGTH_SHORT)
                    .show());
  }

  private double calculateDistanceKm(
      double startLat, double startLng, Double endLat, Double endLng) {
    float[] results = new float[1];
    android.location.Location.distanceBetween(startLat, startLng, endLat, endLng, results);

    return results[0] / 1000.0;
  }

  private void loadPopularItems() {
    firestore
        .collection("food_items")
        .whereEqualTo("available", true)
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              popularItemList.clear();
              String currentUserId = null;

              if (auth.getCurrentUser() != null) {
                currentUserId = auth.getCurrentUser().getUid();
              }

              for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                FoodItem item = document.toObject(FoodItem.class);

                if (item.getSellerId() != null && item.getSellerId().equals(currentUserId))
                  continue;

                popularItemList.add(item);
              }

              Collections.sort(
                  popularItemList, (a, b) -> Integer.compare(b.getSoldCount(), a.getSoldCount()));

              if (popularItemList.size() > 6) {
                while (popularItemList.size() > 6) {
                  popularItemList.remove(popularItemList.size() - 1);
                }
              }

              popularFoodAdapter.notifyDataSetChanged();
            })
        .addOnFailureListener(
            e ->
                Toast.makeText(requireContext(), "Failed to load popular items", Toast.LENGTH_SHORT)
                    .show());
  }

  private void loadCategories() {
    firestore
        .collection("categories")
        .whereEqualTo("active", true)
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              categoryList.clear();

              for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Category category = document.toObject(Category.class);
                category.setId(document.getId());
                categoryList.add(category);
              }

              categoryAdapter.notifyDataSetChanged();
            })
        .addOnFailureListener(
            e -> {
              Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT)
                  .show();
            });
  }

  private void setupClicks() {

    binding.tvSeeAllCategories.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {

            if (requireActivity() instanceof MainActivity) {
              ((MainActivity) requireActivity())
                  .navigateToFragment(new SearchFragment(), R.id.bottom_nav_search, -1, false);
            }
          }
        });

    binding.tvSeeAllPopular.setOnClickListener(v -> {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                    .navigateToFragment(new SearchFragment(), R.id.bottom_nav_search, -1, false);
        }
    });

    if (auth.getCurrentUser() == null) {
      binding.tvSeeAllSellers.setVisibility(View.VISIBLE);
    }

  }

  private void setupRecyclerViews() {
    binding.rvCategories.setLayoutManager(
        new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    binding.rvPopularItems.setLayoutManager(new LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false));
    binding.rvNearbySellers.setLayoutManager(
        new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

    binding.rvCategories.setNestedScrollingEnabled(false);
    binding.rvNearbySellers.setNestedScrollingEnabled(false);
    binding.rvPopularItems.setNestedScrollingEnabled(false);

    categoryAdapter =
        new CategoryAdapter(
            categoryList,
            category -> {
              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                    .openSearchFragmentWithCategory(category.getId(), category.getName());
              }
            });
    popularFoodAdapter= new SearchFoodAdapter(popularItemList,
            item -> {
                SingleProductFragment singleProductFragment = new SingleProductFragment();
                Bundle bundle = new Bundle();
                bundle.putString("itemId", item.getItemId());
                singleProductFragment.setArguments(bundle);

                if(requireActivity() instanceof MainActivity){
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

                firestore.collection("carts")
                        .document(userId)
                        .collection("items")
                        .document(itemId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Long existingQtyLong = documentSnapshot.getLong("quantity");
                                int existingQty = existingQtyLong != null ? existingQtyLong.intValue() : 0;

                                int newQty = existingQty + 1;
                                if (newQty > currentItem.getQuantity()) {
                                    Toast.makeText(requireContext(), "Cannot exceed available stock", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                firestore.collection("carts")
                                        .document(userId)
                                        .collection("items")
                                        .document(itemId)
                                        .update("quantity", newQty)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(requireContext(), "Cart updated", Toast.LENGTH_SHORT).show();
                                            if (requireActivity() instanceof MainActivity) {
                                                ((MainActivity) requireActivity()).countCartItems();
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(requireContext(), "Failed to update cart", Toast.LENGTH_SHORT).show()
                                        );
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
                                cartMap.put("quantity", 1);
                                cartMap.put("portionSize", currentItem.getPortionSize());
                                cartMap.put("imageUrl", imageUrl);
                                cartMap.put("available", currentItem.isAvailable());
                                cartMap.put("addedAt", System.currentTimeMillis());

                                firestore.collection("carts")
                                        .document(userId)
                                        .collection("items")
                                        .document(itemId)
                                        .set(cartMap)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show();
                                            if (requireActivity() instanceof MainActivity) {
                                                ((MainActivity) requireActivity()).countCartItems();
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(requireContext(), "Failed to add to cart", Toast.LENGTH_SHORT).show()
                                        );
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Cart operation failed", Toast.LENGTH_SHORT).show()
                        );
            }

            );

    nearbySellerAdapter =
        new NearbySellerAdapter(
            nearbySellerList,
            seller -> {
              SellerItemsFragment fragment = new SellerItemsFragment();
              Bundle bundle = new Bundle();
              bundle.putString("sellerId", seller.getUid());
              bundle.putString("sellerName", seller.getName());
              fragment.setArguments(bundle);

              requireActivity()
                  .getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.fragment_container, fragment)
                  .addToBackStack(null)
                  .commit();

              if (bottomNavigationView != null) {
                bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                  bottomNavigationView.getMenu().getItem(i).setChecked(false);
                }
                bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
              }

              if (sideNavigationView != null) {
                sideNavigationView
                    .getMenu()
                    .setGroupCheckable(R.id.side_nav_group_main, true, false);
                sideNavigationView
                    .getMenu()
                    .setGroupCheckable(R.id.side_nav_group_auth, true, false);
                for (int i = 0; i < sideNavigationView.getMenu().size(); i++) {
                  android.view.MenuItem item = sideNavigationView.getMenu().getItem(i);
                  if (item.hasSubMenu()) {
                    for (int j = 0; j < item.getSubMenu().size(); j++) {
                      item.getSubMenu().getItem(j).setChecked(false);
                    }
                  } else {
                    item.setChecked(false);
                  }
                }
                sideNavigationView.setCheckedItem(-1);
                sideNavigationView
                    .getMenu()
                    .setGroupCheckable(R.id.side_nav_group_main, true, true);
                sideNavigationView
                    .getMenu()
                    .setGroupCheckable(R.id.side_nav_group_auth, true, true);
              }
            });

    binding.rvCategories.setAdapter(categoryAdapter);
    binding.rvPopularItems.setAdapter(popularFoodAdapter);
    binding.rvNearbySellers.setAdapter(nearbySellerAdapter);
  }

  private void setupBanners() {

    bannerAdapter = new BannerAdapter(bannerList);
    binding.vpBanners.setAdapter(bannerAdapter);
    binding.homeDotsIndicator.attachTo(binding.vpBanners);

    loadBannerImagesFromStorage();
  }

  private void loadBannerImagesFromStorage() {
    bannerList.clear();

    StorageReference storageReference = storage.getReference().child("banner-images");

    storageReference
        .listAll()
        .addOnSuccessListener(
            listResult -> {
              for (StorageReference fileRef : listResult.getItems()) {
                fileRef
                    .getDownloadUrl()
                    .addOnSuccessListener(
                        uri -> {
                          bannerList.add(uri.toString());
                          bannerAdapter.notifyDataSetChanged();
                        })
                    .addOnFailureListener(
                        e -> {
                          Toast.makeText(
                                  requireContext(),
                                  "Failed to load banner image",
                                  Toast.LENGTH_SHORT)
                              .show();
                        });
              }
            })
        .addOnFailureListener(
            e ->
                Toast.makeText(requireContext(), "Failed to load banners", Toast.LENGTH_SHORT)
                    .show());
  }
}

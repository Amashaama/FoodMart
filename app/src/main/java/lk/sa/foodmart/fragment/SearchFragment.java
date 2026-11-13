package lk.sa.foodmart.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.SearchFoodAdapter;
import lk.sa.foodmart.databinding.FragmentSearchBinding;
import lk.sa.foodmart.model.FoodItem;

public class SearchFragment extends Fragment implements SensorEventListener {

  private FragmentSearchBinding binding;
  private FirebaseFirestore firestore;
  private FirebaseAuth auth;

  private final ArrayList<FoodItem> allFoodItems = new ArrayList<>();
  private final ArrayList<FoodItem> filteredFoodItems = new ArrayList<>();

  private SearchFoodAdapter adapter;

  private final ArrayList<String> categoryNames = new ArrayList<>();
  private final ArrayList<String> categoryIds = new ArrayList<>();
  private String selectedCategoryId = "";

  private final ArrayList<String> sortOptions = new ArrayList<>();
  private String selectedSortOption = "Newest";

  private String preSelectedCategoryId = "";
  private String preSelectedCategoryName = "";

  private SensorManager sensorManager;
  private Sensor accelerometer;

  private long lastShakeTime = 0;
  private float lastX = 0f;
  private static final float SHAKE_THRESHOLD = 7.5f;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentSearchBinding.inflate(inflater, container, false);

    firestore = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    if (getArguments() != null) {
      preSelectedCategoryId = getArguments().getString("categoryId", "");
      preSelectedCategoryName = getArguments().getString("categoryName", "");
    }

    Log.i("SearchFragment", preSelectedCategoryId + "," + preSelectedCategoryName);

    setupRecyclerView();

    setupSortDropDown();

    setupListeners();
    loadCategories();
    loadFoodItems();

    // sensor part
    sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
    if (sensorManager != null) {
      accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // Inflate the layout for this fragment
    return binding.getRoot();
  }

  private void showFilterCardAnimation() {
    binding.cardFilters.setVisibility(View.VISIBLE);
    binding.cardFilters.setAlpha(0f);
    binding.cardFilters.setTranslationY(-50f);

    binding.cardFilters.animate().alpha(1f).translationY(0f).setDuration(250).start();
  }

  private void hideFilterCardAnimation() {
    binding
        .cardFilters
        .animate()
        .alpha(0f)
        .translationY(-50f)
        .setDuration(250)
        .withEndAction(
            () -> {
              binding.cardFilters.setVisibility(View.GONE);
              binding.cardFilters.setAlpha(1f);
              binding.cardFilters.setTranslationY(0f);
            })
        .start();
  }

  private void setupSortDropDown() {
    if (!isAdded()) return;
    sortOptions.clear();
    sortOptions.add("Newest");
    sortOptions.add("Price Low to High");
    sortOptions.add("Price High to Low");

    ArrayAdapter<String> sortAdapter =
        new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions);

    binding.actSort.setAdapter(sortAdapter);
    binding.actSort.setText("Newest", false);

    binding.actSort.setOnItemClickListener(
        (parent, view, position, id) -> {
          selectedSortOption = sortOptions.get(position);

          applyFilters();
        });
  }

  private void loadFoodItems() {
    firestore
        .collection("food_items")
        .whereEqualTo("available", true)
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              if (!isAdded()) return;
              allFoodItems.clear();
              String currentUserId = null;
              if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
              }
              for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                FoodItem item = document.toObject(FoodItem.class);
                if (item.getSellerId() != null && item.getSellerId().equals(currentUserId)) {
                  continue;
                }

                allFoodItems.add(item);
              }

              // making newest item first
              Collections.sort(
                  allFoodItems, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
              applyFilters();
            })
        .addOnFailureListener(
            e -> {
              if (isAdded()) {
                Toast.makeText(requireContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
              }
            });
  }
  ;

  private void loadCategories() {
    categoryNames.clear();
    categoryIds.clear();

    categoryNames.add("All Categories");
    categoryIds.add("");

    firestore
        .collection("categories")
        .whereEqualTo("active", true)
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              if (!isAdded()) return;
              for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                String id = document.getId();
                String name = document.getString("name");

                if (name != null) {
                  categoryIds.add(id);
                  categoryNames.add(name);
                }
              }

              ArrayAdapter<String> categoryAdapter =
                  new ArrayAdapter<>(
                      requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames);

              binding.actCategory.setAdapter(categoryAdapter);

              binding.actCategory.setOnItemClickListener(
                  ((parent, view, position, id) -> {
                    selectedCategoryId = categoryIds.get(position);
                    binding.actCategory.setText(categoryNames.get(position), false);
                    applyFilters();
                  }));

              if (!preSelectedCategoryId.isEmpty() && !preSelectedCategoryName.isEmpty()) {
                selectedCategoryId = preSelectedCategoryId;
                binding.actCategory.setText(preSelectedCategoryName, false);
              }

              applyFilters();
            })
        .addOnFailureListener(
            e -> {
              if (isAdded()) {
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  private void setupListeners() {
    binding.btnFilter.setOnClickListener(
        v -> {
          if (binding.cardFilters.getVisibility() == View.VISIBLE) {
            hideFilterCardAnimation();

            binding.btnFilter.setImageResource(R.drawable.baseline_filter_list_24);
          } else {
            showFilterCardAnimation();
            binding.btnFilter.setImageResource(R.drawable.top_panel_close_24px);
          }
        });
    binding.btnClear.setOnClickListener(
        v -> {
          binding.etSearch.setText("");
          binding.btnClear.setVisibility(View.GONE);
          applyFilters();
        });

    binding.etSearch.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void afterTextChanged(Editable s) {}

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            String query = s.toString().trim();
            binding.btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

            applyFilters();
          }
        });

    binding.btnApplyFilters.setOnClickListener(
        v -> {
          applyFilters();
          hideFilterCardAnimation();

          binding.btnFilter.setImageResource(R.drawable.baseline_filter_list_24);
        });

    binding.tvClearFilters.setOnClickListener(
        v -> {
          clearAllFilters();
        });
  }

  @SuppressLint("NotifyDataSetChanged")
  private void applyFilters() {
    String searchText = binding.etSearch.getText().toString().trim().toLowerCase();
    String priceFromStr =
        binding.etPriceFrom.getText() != null
            ? binding.etPriceFrom.getText().toString().trim()
            : "";
    String priceToStr =
        binding.etPriceTo.getText() != null ? binding.etPriceTo.getText().toString().trim() : "";
    String selectedPortion = getSelectedPortionFilter();

    double priceFrom = 0;
    double priceTo = Double.MAX_VALUE;

    try {
      if (!priceFromStr.isEmpty()) {
        priceFrom = Double.parseDouble(priceFromStr);
      }

      if (!priceToStr.isEmpty()) {
        priceTo = Double.parseDouble(priceToStr);
      }
    } catch (Exception e) {
      if (isAdded()) {
        Toast.makeText(requireContext(), "Invalid price range", Toast.LENGTH_SHORT).show();
      }
      return;
    }
    filteredFoodItems.clear();

    for (FoodItem item : allFoodItems) {
      boolean matchesSearch =
          searchText.isEmpty()
              || (item.getItemName() != null)
                  && item.getItemName().toLowerCase().contains(searchText);

      boolean matchesCategory =
          selectedCategoryId.isEmpty()
              || (item.getCategoryId() != null && item.getCategoryId().equals(selectedCategoryId));

      boolean matchesPrice = item.getItemPrice() >= priceFrom && item.getItemPrice() <= priceTo;

      boolean matchesPortion =
          selectedPortion.isEmpty()
              || (item.getPortionSize() != null
                  && item.getPortionSize().equalsIgnoreCase(selectedPortion));

      if (matchesSearch && matchesCategory && matchesPrice && matchesPortion) {
        filteredFoodItems.add(item);
      }
    }

    sortFilteredItems();
    updateActiveFilterChips(selectedPortion, priceFromStr, priceToStr);
    updateUIStates();
    adapter.notifyDataSetChanged();
  }

  private void sortFilteredItems() {
    switch (selectedSortOption) {
      case "Price Low to High":
        Collections.sort(
            filteredFoodItems, ((o1, o2) -> Double.compare(o1.getItemPrice(), o2.getItemPrice())));
        break;

      case "Price High to Low":
        Collections.sort(
            filteredFoodItems, ((o1, o2) -> Double.compare(o2.getItemPrice(), o1.getItemPrice())));
        break;

      case "Newest":
      default:
        Collections.sort(
            filteredFoodItems, (a, b) -> Double.compare(b.getCreatedAt(), a.getCreatedAt()));
        break;
    }
  }

  private void updateUIStates() {
    binding.layoutInitial.setVisibility(View.GONE);

    binding.tvResultsCount.setVisibility(View.VISIBLE);
    binding.tvResultsCount.setText(filteredFoodItems.size() + " Results");

    if (filteredFoodItems.isEmpty()) {
      binding.cardFilters.setVisibility(View.GONE);
      binding.btnFilter.setImageResource(R.drawable.baseline_filter_list_24);
      binding.layoutEmpty.setVisibility(View.VISIBLE);
      binding.rvSearchResults.setVisibility(View.GONE);
    } else {
      binding.layoutEmpty.setVisibility(View.GONE);
      binding.rvSearchResults.setVisibility(View.VISIBLE);
    }
  }

  private void updateActiveFilterChips(String portion, String priceFrom, String priceTo) {
    binding.chipGroupActiveFilters.removeAllViews();

    boolean hasAnyFilter = false;

    if (!selectedCategoryId.isEmpty()) {
      if (!isAdded()) return;
      Chip chip = new Chip(requireContext());
      chip.setText(binding.actCategory.getText().toString());
      chip.setCloseIconVisible(true);
      chip.setOnCloseIconClickListener(
          v -> {
            selectedCategoryId = "";
            binding.actCategory.setText("");
            applyFilters();
          });
      binding.chipGroupActiveFilters.addView(chip);
      hasAnyFilter = true;
    }
  }

  private String getSelectedPortionFilter() {
    int selectedId = binding.chipGroupPortion.getCheckedChipId();

    if (selectedId == binding.chipSmall.getId()) {
      return "Small";
    } else if (selectedId == binding.chipRegular.getId()) {
      return "Regular";

    } else if (selectedId == binding.chipLarge.getId()) {
      return "Large";
    } else if (selectedId == binding.chipFamily.getId()) {
      return "Family";
    }

    return "";
  }

  private void setupRecyclerView() {
    binding.rvSearchResults.setLayoutManager(new GridLayoutManager(getContext(), 2));
    adapter =
        new SearchFoodAdapter(
            filteredFoodItems,
            item -> {
              SingleProductFragment singleProductFragment = new SingleProductFragment();
              Bundle bundle = new Bundle();
              bundle.putString("itemId", item.getItemId());
              singleProductFragment.setArguments(bundle);

              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                    .navigateToFragment(singleProductFragment, -1, -1, true);
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

                                    if (requireActivity() instanceof MainActivity) {
                                      ((MainActivity) requireActivity()).countCartItems();
                                    }
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

                                    if (requireActivity() instanceof MainActivity) {
                                      ((MainActivity) requireActivity()).countCartItems();
                                    }
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
    binding.rvSearchResults.setAdapter(adapter);
  }

  private void clearAllFilters() {
    selectedCategoryId = "";
    selectedSortOption = "Newest";

    binding.actCategory.setText("");
    binding.actSort.setText("Newest", false);
    binding.etPriceFrom.setText("");
    binding.etPriceTo.setText("");
    binding.chipGroupPortion.clearCheck();

    binding.etSearch.setText("");

    applyFilters();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (sensorManager != null && accelerometer != null) {
      sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_UI);
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    if (sensorManager != null) {
      sensorManager.unregisterListener(this);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

    float x = event.values[0];
    float deltaX = Math.abs(x - lastX);
    lastX = x;

    if (deltaX > SHAKE_THRESHOLD) {
      long currentTime = System.currentTimeMillis();

      if (currentTime - lastShakeTime > 1500) {
        lastShakeTime = currentTime;
        Toast.makeText(requireContext(), "Search refreshed", Toast.LENGTH_SHORT).show();
        clearAllFilters();
      }
    }
  }
}

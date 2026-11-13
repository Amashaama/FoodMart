package lk.sa.foodmart.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.CartAdapter;
import lk.sa.foodmart.databinding.FragmentCartBinding;
import lk.sa.foodmart.model.CartItem;

public class CartFragment extends Fragment {

  private FragmentCartBinding binding;
  private FirebaseFirestore firestore;
  private FirebaseAuth auth;

  private final ArrayList<CartItem> cartItems = new ArrayList<>();
  private CartAdapter adapter;

  private int currentQty;

  private static final double DELIVERY_FEE = 300.0;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    binding = FragmentCartBinding.inflate(inflater, container, false);
    firestore = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    setupRecyclerView();
    setupClicks();
    loadCartItems();

    return binding.getRoot();
  }

  private void setupRecyclerView() {
    binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
    // Removed setNestedScrollingEnabled(false) to allow proper scrolling in the new ConstraintLayout
    
    adapter =
        new CartAdapter(
            cartItems,
            new CartAdapter.CartActionListener() {
              @Override
              public void onIncrease(CartItem item) {

                checkStockAndIncrease(item);
              }

              @Override
              public void onDecrease(CartItem item) {
                if (item.getQuantity() > 1) {
                  updateCartQuantity(item, item.getQuantity() - 1);
                } else {
                  removeCartItem(item);
                }
              }

              @Override
              public void onDelete(CartItem item) {
                removeCartItem(item);
              }
            });

    binding.rvCart.setAdapter(adapter);
  }

  private void setupClicks() {
    binding.btnBack.setOnClickListener(
        v -> {
          if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                .navigateToFragment(
                    new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
          }
        });

    binding.btnBrowse.setOnClickListener(
        v -> {
          if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                .navigateToFragment(new SearchFragment(), R.id.bottom_nav_search, -1, false);
          }
        });

    binding.btnClearCart.setOnClickListener(v -> clearCart());

    binding.btnCheckout.setOnClickListener(
        v -> {
          if (auth.getCurrentUser() == null) return;

          if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please add items to Cart", Toast.LENGTH_SHORT).show();
            return;
          }
          CheckoutFragment checkoutFragment = new CheckoutFragment();

          if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                .navigateToFragment(new CheckoutFragment(), -1, -1, true);
          }
        });
  }

  private void loadCartItems() {
    if (auth.getCurrentUser() == null) {
      showEmptyState();
      Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
      return;
    }

    String userId = auth.getCurrentUser().getUid();

    firestore
        .collection("carts")
        .document(userId)
        .collection("items")
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              if (binding == null) return;
              cartItems.clear();

              for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                CartItem item = document.toObject(CartItem.class);
                cartItems.add(item);
              }
              adapter.notifyDataSetChanged();
              updateSummary();
              updateUIState();
            })
        .addOnFailureListener(
            e -> {
              if (getContext() != null) {
                Toast.makeText(requireContext(), "Failed to load cart", Toast.LENGTH_SHORT).show();
              }
            });
  }

  private void updateCartQuantity(CartItem item, int newQuantity) {
    if (auth.getCurrentUser() == null) return;

    String userId = auth.getCurrentUser().getUid();

    firestore
        .collection("carts")
        .document(userId)
        .collection("items")
        .document(item.getItemId())
        .update("quantity", newQuantity)
        .addOnSuccessListener(unused -> loadCartItems())
        .addOnFailureListener(
            e -> {
              if (getContext() != null) {
                Toast.makeText(requireContext(), "Failed to update quantity", Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  private void removeCartItem(CartItem item) {
    if (auth.getCurrentUser() == null) {
      return;
    }

    String userId = auth.getCurrentUser().getUid();

    firestore
        .collection("carts")
        .document(userId)
        .collection("items")
        .document(item.getItemId())
        .delete()
        .addOnSuccessListener(
            unused -> {
              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).countCartItems();
              }
              loadCartItems();
            })
        .addOnFailureListener(
            e -> {
              if (getContext() != null) {
                Toast.makeText(requireContext(), "Failed to remove item", Toast.LENGTH_SHORT).show();
              }
            });
  }

  private void clearCart() {

    if (auth.getCurrentUser() == null) {
      return;
    }

    String userId = auth.getCurrentUser().getUid();

    firestore
        .collection("carts")
        .document(userId)
        .collection("items")
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                documentSnapshot.getReference().delete();
              }
              if (getContext() != null) {
                Toast.makeText(requireContext(), "Cart cleared", Toast.LENGTH_SHORT).show();
              }
              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).countCartItems();
              }
              loadCartItems();
            })
        .addOnFailureListener(
            e -> {
              if (getContext() != null) {
                Toast.makeText(requireContext(), "Failed to clear cart", Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  private void updateSummary() {
    if (binding == null) return;
    double subtotal = 0;

    for (CartItem item : cartItems) {
      subtotal += item.getItemPrice() * item.getQuantity();
    }

    binding.tvTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", subtotal));
    binding.tvTitle.setText("My Cart (" + cartItems.size() + ") ");
  }

  private void updateUIState() {
    if (binding == null) return;
    if (cartItems.isEmpty()) {
      showEmptyState();
      binding.btnClearCart.setVisibility(View.GONE);
    } else {
      binding.layoutEmpty.setVisibility(View.GONE);
      binding.rvCart.setVisibility(View.VISIBLE);
      binding.cardSummary.setVisibility(View.VISIBLE);
      binding.btnClearCart.setVisibility(View.VISIBLE);
    }
  }

  private void checkStockAndIncrease(CartItem cartItem) {
    firestore
        .collection("food_items")
        .document(cartItem.getItemId())
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              if (binding == null) return;
              if (documentSnapshot.exists()) {
                Long stockLong = documentSnapshot.getLong("quantity");
                int availableStock = stockLong != null ? stockLong.intValue() : 0;

                if (cartItem.getQuantity() < availableStock) {
                  updateCartQuantity(cartItem, cartItem.getQuantity() + 1);
                } else {
                  if (getContext() != null) {
                    Toast.makeText(
                            requireContext(), "Cannot exceed available stock", Toast.LENGTH_SHORT)
                        .show();
                  }
                }
              } else {
                if (getContext() != null) {
                  Toast.makeText(requireContext(), "Item no longer available", Toast.LENGTH_SHORT)
                      .show();
                }
              }
            })
        .addOnFailureListener(
            e -> {
              if (getContext() != null) {
                Toast.makeText(requireContext(), "Failed to check stock", Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  private void showEmptyState() {
    if (binding == null) return;
    binding.layoutEmpty.setVisibility(View.VISIBLE);
    binding.rvCart.setVisibility(View.GONE);
    binding.cardSummary.setVisibility(View.GONE);
    binding.tvTitle.setText("My Cart (0)");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}

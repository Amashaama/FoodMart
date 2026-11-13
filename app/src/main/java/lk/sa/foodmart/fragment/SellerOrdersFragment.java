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
import java.util.Collections;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.SellerOrdersAdapter;
import lk.sa.foodmart.databinding.FragmentSellerOrdersBinding;
import lk.sa.foodmart.model.Order;
import lk.sa.foodmart.model.OrderItem;
import lk.sa.foodmart.model.SellerOrderDisplay;

public class SellerOrdersFragment extends Fragment {

  private FragmentSellerOrdersBinding binding;
  private FirebaseFirestore firestore;
  private FirebaseAuth auth;

  private final ArrayList<SellerOrderDisplay> sellerOrders = new ArrayList<>();
  private SellerOrdersAdapter adapter;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    binding = FragmentSellerOrdersBinding.inflate(inflater, container, false);

    firestore = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    setupRecyclerView();
    setupClicks();
    loadSellerOrders();

    return binding.getRoot();
  }

  private void setupRecyclerView() {
      if (binding == null) return;
    binding.rvSellerOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new SellerOrdersAdapter(sellerOrders);
    binding.rvSellerOrders.setAdapter(adapter);
  }

  private void setupClicks() {
      if (binding == null) return;
    binding.btnBack.setOnClickListener(
        v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                        .navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
            }
        });
  }

  private void loadSellerOrders() {
    if (binding == null) return;
    if (auth.getCurrentUser() == null) {
      showEmptyState();
      return;
    }

    binding.progressLoading.setVisibility(View.VISIBLE);
    binding.layoutEmpty.setVisibility(View.GONE);

    String currentSellerId = auth.getCurrentUser().getUid();

    firestore
        .collection("orders")
        .get()
        .addOnSuccessListener(
            queryDocumentSnapshots -> {
              if (binding == null) return;
              sellerOrders.clear();

              final int totalOrders = queryDocumentSnapshots.size();
              if (totalOrders == 0) {
                finishLoading();
                return;
              }
              final int[] checkedOrders = {0};
              for (QueryDocumentSnapshot orderDoc : queryDocumentSnapshots) {
                Order order = orderDoc.toObject(Order.class);

                if (order == null) {
                  checkedOrders[0]++;
                  if (checkedOrders[0] == totalOrders) finishLoading();
                  continue;
                }

                firestore
                    .collection("orders")
                    .document(orderDoc.getId())
                    .collection("items")
                    .get()
                    .addOnSuccessListener(
                        itemSnapShots -> {
                          if (binding == null) return;
                          int sellerItemCount = 0;

                          for (QueryDocumentSnapshot itemDoc : itemSnapShots) {
                            OrderItem item = itemDoc.toObject(OrderItem.class);

                            if (item.getSellerId() != null
                                && item.getSellerId().equals(currentSellerId)) {
                              sellerItemCount++;
                            }
                          }

                          if (sellerItemCount > 0) {
                            SellerOrderDisplay display =
                                SellerOrderDisplay.builder()
                                    .orderId(order.getOrderId())
                                    .buyerName(order.getFullName())
                                    .buyerPhone(order.getPhone())
                                    .status(order.getStatus())
                                    .total(order.getTotal())
                                    .createdAt(order.getCreatedAt())
                                    .sellerItemCount(sellerItemCount)
                                    .build();

                            sellerOrders.add(display);
                          }

                          checkedOrders[0]++;
                          if (checkedOrders[0] == totalOrders) {
                            finishLoading();
                          }
                        })
                    .addOnFailureListener(
                        e -> {
                          if (binding == null) return;
                          checkedOrders[0]++;
                          if (checkedOrders[0] == totalOrders) {
                            finishLoading();
                          }
                        });
              }
            })
        .addOnFailureListener(
            e -> {
              if (binding == null) return;
              binding.progressLoading.setVisibility(View.GONE);

              if (getContext() != null) {
                  Toast.makeText(requireContext(), "Failed to load seller orders", Toast.LENGTH_SHORT)
                      .show();
              }
            });
  }

  private void finishLoading() {
      if (binding == null) return;
    Collections.sort(sellerOrders, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

    adapter.notifyDataSetChanged();

    binding.progressLoading.setVisibility(View.GONE);

    if (sellerOrders.isEmpty()) {
      showEmptyState();
    } else {
      binding.layoutEmpty.setVisibility(View.GONE);
      binding.rvSellerOrders.setVisibility(View.VISIBLE);
    }
  }

  private void showEmptyState() {
      if (binding == null) return;
    binding.layoutEmpty.setVisibility(View.VISIBLE);
    binding.rvSellerOrders.setVisibility(View.GONE);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}

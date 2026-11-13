package lk.sa.foodmart.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.OrderAdapter;
import lk.sa.foodmart.databinding.FragmentMyOrdersBinding;
import lk.sa.foodmart.model.Order;

public class MyOrdersFragment extends Fragment {


    private FragmentMyOrdersBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private final ArrayList<Order> orderList = new ArrayList<>();
    private OrderAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyOrdersBinding.inflate(inflater,container,false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupClicks();

        loadOrders();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderAdapter(orderList,order -> {
            OrderDetailsFragment fragment = new OrderDetailsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("orderId",order.getOrderId());
            fragment.setArguments(bundle);

            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToFragment(
                       fragment,
                        -1,
                        -1,
                        false
                );
            }
        });

        binding.rvOrders.setAdapter(adapter);
    }

    private void setupClicks(){
        binding.btnBack.setOnClickListener(
            v -> {
              if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity())
                    .navigateToFragment(new HomeFragment(), R.id.bottom_nav_home, R.id.side_nav_home, false);
              }
            });

        binding.btnBrowse.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToFragment(new SearchFragment(),R.id.bottom_nav_search,-1,false);
            }
        });

        binding.btnViewReceipt.setOnClickListener(v -> showLastReceipt());
    }

    private void showLastReceipt() {
        try (FileInputStream fis = requireContext().openFileInput("last_order_receipt.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Last Order Receipt")
                    .setMessage(sb.toString())
                    .setPositiveButton("Close", null)
                    .show();

        } catch (FileNotFoundException e) {
            Toast.makeText(requireContext(), "No recent receipt found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error reading receipt", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadOrders(){
        if(auth.getCurrentUser() == null){
            showEmptyState("Please login","Login to view your orders");
            return;
        }

        binding.layoutEmpty.setVisibility(View.GONE);

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("orders")
                .whereEqualTo("userId",uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                   orderList.clear();
                   for(QueryDocumentSnapshot document: queryDocumentSnapshots){
                       Order order = document.toObject(Order.class);
                       orderList.add(order);
                   }

                    Collections.sort(orderList,(a,b) -> Long.compare(b.getCreatedAt(),a.getCreatedAt()));
                   adapter.notifyDataSetChanged();
                   updateUIStates();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
                });

    }

    private void updateUIStates(){
        if(orderList.isEmpty()){
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvOrders.setVisibility(View.GONE);
        }else{
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvOrders.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(String title, String message) {
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.rvOrders.setVisibility(View.GONE);
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptyMessage.setText(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

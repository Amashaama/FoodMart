package lk.sa.foodmart.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.MainActivity;
import lk.sa.foodmart.adapter.OrderItemAdapter;
import lk.sa.foodmart.databinding.FragmentOrderDetailsBinding;
import lk.sa.foodmart.model.Order;
import lk.sa.foodmart.model.OrderItem;


public class OrderDetailsFragment extends Fragment {


  private FragmentOrderDetailsBinding binding;
  private FirebaseFirestore firestore;

  private String orderId = "";
  private Order currentOrder;

  private final ArrayList<OrderItem> orderItems = new ArrayList<>();
  private OrderItemAdapter adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderDetailsBinding.inflate(inflater,container,false);

        firestore = FirebaseFirestore.getInstance();

        if(getArguments() != null){
            orderId =  getArguments().getString("orderId","");
        }

    binding.btnBack.setOnClickListener(
        v -> {
          if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                .navigateToFragment(new MyOrdersFragment(), -1, R.id.side_nav_my_orders, false);
          }
        });

        setupRecyclerView();
        loadOrderDetails();
        loadOrderItems();

        // Inflate the layout for this fragment
        return binding.getRoot();
    }


    private void loadOrderDetails() {
        if(orderId.isEmpty()){
            Toast.makeText(requireContext(),"Invalid order",Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        currentOrder = documentSnapshot.toObject(Order.class);

                        if (currentOrder != null){
                            bindOrderData(currentOrder);
                        }
                    }else{
                        Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show();
                    }
                }) .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load order", Toast.LENGTH_SHORT).show()
                );
    }
    private void loadOrderItems() {
        if (orderId.isEmpty()) return;

        firestore.collection("orders")
                .document(orderId)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderItems.clear();

                    for(QueryDocumentSnapshot document: queryDocumentSnapshots){
                        OrderItem item = document.toObject(OrderItem.class);
                        orderItems.add(item);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load order items", Toast.LENGTH_SHORT).show()
                );
    }

    private void bindOrderData(Order order) {
        binding.tvOrderId.setText("Order #"+order.getOrderId());

        String formattedDate = new SimpleDateFormat("dd MM yyyy", Locale.getDefault())
                .format(new Date(order.getCreatedAt()));
        binding.tvOrderDate.setText(formattedDate);

        binding.tvOrderStatus.setText(order.getStatus());
        binding.tvCustomerName.setText(order.getFullName());
        binding.tvCustomerPhone.setText(order.getPhone());

        String fullAddress = order.getAddressLine1();
        if(order.getAddressLine2() != null && !order.getAddressLine2().isEmpty()){
            fullAddress += ", "+order.getAddressLine2();
        }

        if(order.getCity() != null && !order.getCity().isEmpty()){
            fullAddress += ", "+ order.getCity();
        }

        if(order.getPostalCode() != null && !order.getPostalCode().isEmpty()){
            fullAddress += " - "+order.getPostalCode();
        }

        binding.tvCustomerAddress.setText(fullAddress);
        binding.tvPaymentMethod.setText(order.getPaymentMethod());

        binding.tvSubtotal.setText(String.format(Locale.getDefault(), "Subtotal: LKR %.2f", order.getSubtotal()));
        binding.tvDeliveryFee.setText(String.format(Locale.getDefault(), "Delivery: LKR %.2f", order.getDeliveryFee()));
        binding.tvTotal.setText(String.format(Locale.getDefault(), "Total: LKR %.2f", order.getTotal()));



    }


    private void setupRecyclerView() {
    binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new OrderItemAdapter(orderItems);
    binding.rvOrderItems.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
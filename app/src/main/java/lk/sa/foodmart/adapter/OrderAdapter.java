package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import lk.sa.foodmart.databinding.ItemOrderBinding;
import lk.sa.foodmart.model.Order;
import lk.sa.foodmart.model.OrderItem;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderClickListener{
       void onOrderClick(Order order);
   }
    public OrderAdapter(ArrayList<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

   private final ArrayList<Order> orderList;
   private final OnOrderClickListener listener;
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
    return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
Order order = orderList.get(position);
holder.binding.tvOrderId.setText("Order #"+order.getOrderId());
holder.binding.tvStatus.setText(order.getStatus() != null ? order.getStatus(): "Pending");
holder.binding.tvTotal.setText(String.format(Locale.getDefault(),"LKR %.2f",order.getTotal()));
holder.binding.tvPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod() : "Card Payment");

String formattedDate = new SimpleDateFormat("dd MMM yyyy",Locale.getDefault())
        .format(new Date(order.getCreatedAt()));

holder.binding.tvDate.setText(formattedDate);

holder.itemView.setOnClickListener(v -> {
    if(listener != null){
        listener.onOrderClick(order);
    }
});

    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private ItemOrderBinding binding;
        public ViewHolder(@NonNull ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}

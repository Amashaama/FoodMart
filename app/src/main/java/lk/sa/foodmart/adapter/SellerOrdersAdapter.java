package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

import lk.sa.foodmart.databinding.ItemSellerOrderBinding;
import lk.sa.foodmart.model.SellerOrderDisplay;

public class SellerOrdersAdapter extends RecyclerView.Adapter<SellerOrdersAdapter.ViewHolder> {

    private final ArrayList<SellerOrderDisplay> orderList;

    public SellerOrdersAdapter(ArrayList<SellerOrderDisplay> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerOrderBinding binding = ItemSellerOrderBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
          SellerOrderDisplay order = orderList.get(position);

          holder.binding.tvOrderId.setText("Order #"+ order.getOrderId());
          holder.binding.tvStatus.setText(order.getStatus());
          holder.binding.tvBuyerName.setText("Buyer: "+order.getBuyerName());
          holder.binding.tvItemCount.setText("Your Items: "+order.getSellerItemCount());
          holder.binding.tvTotal.setText(String.format(Locale.getDefault(),"LKR %.2f",order.getTotal()));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ItemSellerOrderBinding binding;
        public ViewHolder(@NonNull ItemSellerOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}

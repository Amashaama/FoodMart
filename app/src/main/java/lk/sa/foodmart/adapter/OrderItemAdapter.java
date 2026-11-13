package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ItemOrderDetailProductBinding;
import lk.sa.foodmart.model.OrderItem;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {
    private final ArrayList<OrderItem> itemList;

    public OrderItemAdapter(ArrayList<OrderItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderDetailProductBinding binding = ItemOrderDetailProductBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
    return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
           OrderItem item = itemList.get(position);

        holder.binding.tvItemName.setText(item.getItemName());
        holder.binding.tvItemPortion.setText(item.getPortionSize());
        holder.binding.tvItemQty.setText("Qty: " + item.getQuantity());
        holder.binding.tvItemPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", item.getItemPrice()));

        if(item.getImageUrl() != null && !item.getImageUrl().isEmpty()){
            Glide.with(holder.binding.ivItem.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_image_24)
                    .centerCrop()
                    .into(holder.binding.ivItem);
        }else{
            holder.binding.ivItem.setImageResource(R.drawable.baseline_image_24);
        }


    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
 private ItemOrderDetailProductBinding binding;
        public ViewHolder(@NonNull ItemOrderDetailProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

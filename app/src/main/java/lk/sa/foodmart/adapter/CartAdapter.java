package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ItemCartBinding;
import lk.sa.foodmart.model.CartItem;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartActionListener {
        void onIncrease(CartItem item);
        void onDecrease(CartItem item);
        void onDelete(CartItem item);
    }

    private final ArrayList<CartItem> cartItems;
    private final CartActionListener listener;

    public CartAdapter(ArrayList<CartItem> cartItems, CartActionListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.binding.tvFoodName.setText(item.getItemName());
        holder.binding.tvOptions.setText(item.getPortionSize());
        holder.binding.tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", item.getItemPrice()));
        holder.binding.tvQuantity.setText(String.valueOf(item.getQuantity()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.binding.ivFood.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_image_24)
                    .centerCrop()
                    .into(holder.binding.ivFood);
        } else {
            holder.binding.ivFood.setImageResource(R.drawable.baseline_image_24);
        }

        holder.binding.btnIncrease.setOnClickListener(v -> {
            if (listener != null) listener.onIncrease(item);
        });

        holder.binding.btnDecrease.setOnClickListener(v -> {
            if (listener != null) listener.onDecrease(item);
        });

        holder.binding.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ItemCartBinding binding;

        public CartViewHolder(@NonNull ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
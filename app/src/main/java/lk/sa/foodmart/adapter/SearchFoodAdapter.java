package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.OnItemAddToCart;
import lk.sa.foodmart.activity.OnItemClickListener;
import lk.sa.foodmart.databinding.ItemSearchFoodBinding;
import lk.sa.foodmart.model.FoodItem;

public class SearchFoodAdapter extends RecyclerView.Adapter<SearchFoodAdapter.ViewHolder> {
  private final ArrayList<FoodItem> foodItems;
  private final OnItemClickListener onItemClickListener;

  private final OnItemAddToCart addToCart;

  public SearchFoodAdapter(ArrayList<FoodItem> foodItems, OnItemClickListener onItemClickListener, OnItemAddToCart addToCart) {
    this.foodItems = foodItems;
    this.onItemClickListener = onItemClickListener;
      this.addToCart = addToCart;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemSearchFoodBinding binding =
        ItemSearchFoodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    FoodItem item = foodItems.get(position);

    holder.binding.tvItemName.setText(item.getItemName());
    holder.binding.tvCategory.setText(item.getCategoryName());
    holder.binding.tvPortion.setText(item.getPortionSize());
    holder.binding.tvPrice.setText(
        "LKR " + String.format(Locale.getDefault(), "%.2f", item.getItemPrice()));

    if (item.getImageUris() != null && !item.getImageUris().isEmpty()) {
      Glide.with(holder.binding.ivFoodImage.getContext())
          .load(item.getImageUris().get(0))
          .placeholder(R.drawable.baseline_image_24)
          .error(R.drawable.baseline_image_24)
          .centerCrop()
          .into(holder.binding.ivFoodImage);
    } else {
      holder.binding.ivFoodImage.setImageResource(R.drawable.baseline_image_24);
    }

    holder.binding.btnBuyNow.setOnClickListener(
        v -> {
          onItemClickListener.onItemClick(item);
        });

    holder.binding.btnAddToCart.setOnClickListener(v -> {
        onItemClickListener.onItemClick(item);
    });

    holder.binding.btnAddToCart.setOnClickListener(v -> {
        addToCart.btnAddToCart(item);
    });
  }

  @Override
  public int getItemCount() {
    return foodItems.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    ItemSearchFoodBinding binding;

    public ViewHolder(@NonNull ItemSearchFoodBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}

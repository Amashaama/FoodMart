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
import lk.sa.foodmart.databinding.ItemSellerShopBinding;
import lk.sa.foodmart.model.FoodItem;

public class SellerMyShopAdapter extends RecyclerView.Adapter<SellerMyShopAdapter.ViewHolder> {
    public SellerMyShopAdapter(ArrayList<FoodItem> itemArrayList, OnItemClickListener listener) {
        this.itemArrayList = itemArrayList;
        this.listener = listener;
    }

    public interface OnItemClickListener{
        void onItemClick(FoodItem item);
    }

    private final ArrayList<FoodItem> itemArrayList;
    private final OnItemClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerShopBinding binding = ItemSellerShopBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);

    return new ViewHolder(binding);
  }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
          FoodItem item =itemArrayList.get(position);

          holder.binding.tvItemName.setText(item.getItemName());
          holder.binding.tvCategory.setText(item.getCategoryName());
          holder.binding.tvPrice.setText(String.format(Locale.getDefault(),"LKR %.2f",item.getItemPrice()));
          holder.binding.tvStock.setText("Stock: "+item.getQuantity());

          if(item.getImageUris() != null && !item.getImageUris().isEmpty()){
              Glide.with(holder.binding.ivItemImage.getContext())
                      .load(item.getImageUris().get(0))
                      .placeholder(R.drawable.baseline_image_24)
                      .error(R.drawable.baseline_image_24)
                      .centerCrop()
                      .into(holder.binding.ivItemImage);
          }else{
              holder.binding.ivItemImage.setImageResource(R.drawable.baseline_image_24);
          }

          holder.itemView.setOnClickListener(v -> {
              if(listener != null){
                  listener.onItemClick(item);
              }
          });
    }

    @Override
    public int getItemCount() {
        return itemArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ItemSellerShopBinding binding;
        public ViewHolder(@NonNull ItemSellerShopBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}

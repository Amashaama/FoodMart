package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.OnCategoryClickListener;
import lk.sa.foodmart.databinding.ItemCategoryBinding;
import lk.sa.foodmart.model.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final ArrayList<Category> categoryList;
    private final OnCategoryClickListener listener;

    public CategoryAdapter(ArrayList<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ViewHolder(binding);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);

        holder.binding.tvCategoryName.setText(category.getName());

        if(!category.getImageUrl().isEmpty()){
            Glide.with(holder.binding.ivCategoryImage.getContext())
                    .load(category.getImageUrl())
                    .error(R.drawable.baseline_image_24)
                    .centerCrop()
                    .into(holder.binding.ivCategoryImage);
        }else{
            holder.binding.ivCategoryImage.setImageResource(R.drawable.baseline_image_24);
        }

        holder.itemView.setOnClickListener(v -> {
            if(listener != null){
                listener.onCategoryClick(category);
            }
        });

    }

    @Override
    public int getItemCount() {
    return categoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private ItemCategoryBinding binding;
        public ViewHolder(@NonNull ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding= binding;
        }
    }

}

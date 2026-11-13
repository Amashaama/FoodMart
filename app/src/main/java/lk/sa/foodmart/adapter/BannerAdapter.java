package lk.sa.foodmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import lk.sa.foodmart.R;
import lk.sa.foodmart.databinding.ItemBannerBinding;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder>{

    private final ArrayList<String> bannerUrls;

    public BannerAdapter(ArrayList<String> bannerUrls) {
        this.bannerUrls= bannerUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBannerBinding binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
       String imageUrl = bannerUrls.get(position);

        Glide.with(holder.binding.ivBanner.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_image_24)
                .centerCrop()
                .into(holder.binding.ivBanner);

    }

    @Override
    public int getItemCount() {
    return bannerUrls.size();
  }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ItemBannerBinding binding;
        public ViewHolder(@NonNull ItemBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}

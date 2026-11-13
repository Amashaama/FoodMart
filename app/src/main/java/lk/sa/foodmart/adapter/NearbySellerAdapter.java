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
import lk.sa.foodmart.activity.OnSellerClickListener;
import lk.sa.foodmart.databinding.ItemNearbySellerBinding;
import lk.sa.foodmart.model.User;

public class NearbySellerAdapter extends RecyclerView.Adapter<NearbySellerAdapter.ViewHolder> {

    private final ArrayList<User> sellerList;
    private final OnSellerClickListener listener;

    public NearbySellerAdapter(ArrayList<User> sellerList, OnSellerClickListener listener) {
        this.sellerList = sellerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNearbySellerBinding binding = ItemNearbySellerBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);

    return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        User seller = sellerList.get(position);

        holder.binding.tvSellerName.setText(seller.getName());
        holder.binding.tvSellerCity.setText(seller.getCity());

        if(seller.getDistanceKm() >0){
            holder.binding.tvSellerDistance.setText(
                    String.format(Locale.getDefault(),"%.1f km away",seller.getDistanceKm()));

        }else{
            holder.binding.tvSellerDistance.setText("Nearby seller");
        }

        if(seller.getProfilePicUrl() != null && !seller.getProfilePicUrl().isEmpty()){
            Glide.with(holder.binding.ivSellerImage.getContext())
                    .load(seller.getProfilePicUrl())
                    .placeholder(R.drawable.baseline_image_24)
                    .error(R.drawable.baseline_image_24)
                    .centerCrop()
                    .into(holder.binding.ivSellerImage);
        }else{
            holder.binding.ivSellerImage.setImageResource(R.drawable.baseline_image_24);
        }

        holder.itemView.setOnClickListener(v -> {
            if(listener != null){
                listener.onSellerClick(seller);
            }
        });

    }

    @Override
    public int getItemCount() {
        return sellerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ItemNearbySellerBinding binding;
        public ViewHolder(@NonNull ItemNearbySellerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }



}

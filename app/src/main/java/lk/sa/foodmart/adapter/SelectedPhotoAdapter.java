package lk.sa.foodmart.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import lk.sa.foodmart.R;
import lk.sa.foodmart.activity.OnPhotoRemovalListener;

public class SelectedPhotoAdapter extends RecyclerView.Adapter<SelectedPhotoAdapter.ViewHolder> {

    private final ArrayList<Uri> imageUris;
    private final OnPhotoRemovalListener removalListener;

    public SelectedPhotoAdapter(ArrayList<Uri> imageUris, OnPhotoRemovalListener removalListener) {
        this.imageUris = imageUris;
        this.removalListener = removalListener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view=  LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_photo,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
          Uri imageUri = imageUris.get(position);
          
          Glide.with(holder.itemView.getContext())
                  .load(imageUri)
                  .centerCrop()
                  .into(holder.itemImage);

          holder.itemImageRemove.setOnClickListener(v -> {
              if(removalListener != null){
                  removalListener.onRemove(position);
              }
          });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView itemImage, itemImageRemove;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImageRemove=itemView.findViewById(R.id.iv_remove);
            itemImage=itemView.findViewById(R.id.iv_photo);
        }
    }
}

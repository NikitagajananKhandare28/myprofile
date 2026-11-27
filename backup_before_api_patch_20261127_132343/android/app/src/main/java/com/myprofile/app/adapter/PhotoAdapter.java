package com.myprofile.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.myprofile.app.data.model.Photo;
import com.myprofile.app.databinding.ItemPhotoBinding;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.Holder> {

    public interface Listener {
        void onDelete(Photo photo);
    }

    private final List<Photo> items = new ArrayList<>();
    private final Listener listener;

    public PhotoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Photo> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Photo photo = items.get(position);
        Glide.with(holder.binding.getRoot()).load(photo.getUrl()).centerCrop()
                .into(holder.binding.ivPhoto);
        if (photo.getCaption().isEmpty()) {
            holder.binding.tvCaption.setVisibility(View.GONE);
        } else {
            holder.binding.tvCaption.setText(photo.getCaption());
            holder.binding.tvCaption.setVisibility(View.VISIBLE);
        }
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(photo));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPhotoBinding binding;

        Holder(ItemPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
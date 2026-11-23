package com.myprofile.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myprofile.app.R;
import com.myprofile.app.data.model.SectionItem;
import com.myprofile.app.databinding.ItemSectionBinding;

import java.util.ArrayList;
import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.Holder> {

    public interface Listener {
        void onEdit(SectionItem item);

        void onDelete(SectionItem item);
    }

    private final List<SectionItem> items = new ArrayList<>();
    private final Listener listener;

    public SectionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<SectionItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSectionBinding binding = ItemSectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SectionItem item = items.get(position);
        ItemSectionBinding b = holder.binding;

        b.tvTitle.setText(item.getTitle());
        setOptional(b.tvSubtitle, item.getSubtitle());
        setOptional(b.tvPeriod, item.getPeriod());
        setOptional(b.tvDescription, item.getDescription());

        if (item.getVisibility().isEmpty()) {
            b.tvVisibility.setVisibility(View.GONE);
        } else {
            b.tvVisibility.setText(item.isPublic()
                    ? R.string.visibility_public : R.string.visibility_private);
            b.tvVisibility.setVisibility(View.VISIBLE);
        }

        b.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        b.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void setOptional(TextView view, String text) {
        if (text == null || text.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemSectionBinding binding;

        Holder(ItemSectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
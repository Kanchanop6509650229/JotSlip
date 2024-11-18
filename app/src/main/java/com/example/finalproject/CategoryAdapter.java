package com.example.finalproject;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<TransferSlip> dataSet;
    private List<CategoryGroup> groupedData;
    private MyClickListener mCallback;

    public CategoryAdapter(List<TransferSlip> myDataSet) {
        this.dataSet = myDataSet != null ? myDataSet : new ArrayList<>();
        groupSlipsByCategory();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.category_layout, parent, false);

        CardView cardView = view.findViewById(R.id.list_view);
        cardView.setCardElevation(0);
        cardView.setRadius(0);
        cardView.setCardBackgroundColor(Color.TRANSPARENT);
        cardView.setContentPadding(0, 0, 0, 0);
        cardView.setLayoutParams(new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ((ViewGroup.MarginLayoutParams) cardView.getLayoutParams()).setMargins(0, 0, 0, 0);

        return new ViewHolder(view);
    }

    private void groupSlipsByCategory() {
        Map<String, CategoryGroup> categoryGroups = new HashMap<>();
        groupedData = new ArrayList<>();

        for (TransferSlip slip : dataSet) {
            String category = slip.getCategory();
            if (!categoryGroups.containsKey(category)) {
                categoryGroups.put(category, new CategoryGroup(category));
            }
            categoryGroups.get(category).addSlip(slip);
        }

        for (CategoryGroup group : categoryGroups.values()) {
            if (group.getTotalAmount() > 0) {
                groupedData.add(group);
            }
        }
        
        Collections.sort(groupedData, (a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CategoryGroup group = groupedData.get(position);

        holder.categoryIcon.setImageResource(R.drawable.ic_launcher_foreground);
        holder.categoryName.setText(group.getCategory());
        holder.transactionAmount.setText(String.format("- %.2f ฿", group.getTotalAmount()));
        holder.transactionAmount.setTextColor(holder.itemView.getContext()
                .getResources().getColor(android.R.color.holo_red_dark));

        double totalExpense = groupedData.stream()
                .mapToDouble(CategoryGroup::getTotalAmount)
                .sum();
        double percentage = (group.getTotalAmount() / totalExpense) * 100;
        holder.transactionPercent.setText(String.format("%.1f%%", percentage));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CategoryActivity.class);
            Bundle options = ActivityOptions.makeSceneTransitionAnimation((MainActivity) v.getContext()).toBundle();
            v.getContext().startActivity(intent, options);
        });
    }

    @Override
    public int getItemCount() {
        return groupedData.size();
    }

    public void setOnItemClickListener(MyClickListener listener) {
        this.mCallback = listener;
    }

    public interface MyClickListener {
        void onItemClick(int position, View v);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView categoryIcon;
        TextView categoryName;
        TextView transactionPercent;
        TextView transactionAmount;

        public ViewHolder(View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            categoryName = itemView.findViewById(R.id.categoryName);
            transactionPercent = itemView.findViewById(R.id.transactionPercent);
            transactionAmount = itemView.findViewById(R.id.transactionAmount);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mCallback != null) {
                mCallback.onItemClick(getAdapterPosition(), v);
            }
        }
    }

    private static class CategoryGroup {
        private String category;
        private List<TransferSlip> slips;
        private double totalExpense;

        public CategoryGroup(String category) {
            this.category = category;
            this.slips = new ArrayList<>();
            this.totalExpense = 0;
        }

        public void addSlip(TransferSlip slip) {
            slips.add(slip);
            if (slip.getType() != 1) { // รายรับ
                totalExpense += slip.getAmount();
            }
        }

        public String getCategory() {
            return category;
        }

        public List<TransferSlip> getSlips() {
            return slips;
        }

        public double getTotalAmount() {
            return totalExpense;
        }
    }
}
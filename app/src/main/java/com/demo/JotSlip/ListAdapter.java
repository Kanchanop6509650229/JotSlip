package com.demo.JotSlip;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private List<TransferSlip> dataSet;
    private MyClickListener mCallback;
    private boolean isFromMainActivity;
    private Context context;

    public ListAdapter(List<TransferSlip> myDataSet) {
        this.dataSet = myDataSet;
        this.isFromMainActivity = true;
    }

    public ListAdapter(List<TransferSlip> myDataSet, boolean isFromMainActivity, Context context) {
        this.dataSet = myDataSet;
        this.isFromMainActivity = isFromMainActivity;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_layout, parent, false);

        CardView cardView = view.findViewById(R.id.list_view);
        if (isFromMainActivity) {
            cardView.setCardElevation(0);
            cardView.setRadius(0);
            cardView.setCardBackgroundColor(Color.TRANSPARENT);
            cardView.setContentPadding(0, 0, 0, 0);
            cardView.setLayoutParams(new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            ((ViewGroup.MarginLayoutParams) cardView.getLayoutParams()).setMargins(0, 0, 0, 0);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TransferSlip slip = dataSet.get(position);

        // Set category icon based on category name
        int iconResId = getCategoryIcon(slip.getCategory());
        holder.categoryIcon.setImageResource(iconResId);

        // Set category name using localized string
        String categoryKey = slip.getCategory();
        holder.categoryName.setText(holder.itemView.getContext().getString(
                CategoryConstants.getDisplayStringResource(categoryKey)
        ));

        if (isFromMainActivity) {
            // Parse date for main activity display
            String[] dateTimeParts = slip.getDateTime().split(" ");
            if (dateTimeParts.length > 0) {
                String[] dateParts = dateTimeParts[0].split("/");
                if (dateParts.length == 3) {
                    // Get month names from resources safely
                    String[] thaiMonths = holder.itemView.getContext().getResources().getStringArray(R.array.months_array);
                    int monthIndex = Integer.parseInt(dateParts[1]) - 1; // Convert to 0-based index

                    // Ensure monthIndex is within bounds
                    if (monthIndex >= 0 && monthIndex < thaiMonths.length) {
                        String month = thaiMonths[monthIndex];
                        holder.transactionNote.setText(String.format(
                                holder.itemView.getContext().getString(R.string.date_format_string),
                                dateParts[0], month, dateParts[2]));
                    }
                }
            }
        } else {
            // Show description for non-main activity views
            String description = slip.getDescription();
            if (description != null && !description.isEmpty()) {
                holder.transactionNote.setText(description);
            } else {
                holder.transactionNote.setText(holder.itemView.getContext().getString(R.string.no_description));
            }
        }

        // Set amount with proper formatting and color based on type
        String amount;
        Context context = holder.itemView.getContext();
        if (slip.getType() == 1) { // Income
            amount = String.format(context.getString(R.string.amount_format_positive), slip.getAmount());
            holder.transactionAmount.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else { // Expense
            amount = String.format(context.getString(R.string.amount_format_negative), slip.getAmount());
            holder.transactionAmount.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }
        holder.transactionAmount.setText(amount);

        holder.itemView.setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, SlipInfoActivity.class);
                intent.putExtra("slip_id", slip.getId());
                context.startActivity(intent);
            }
        });
    }

    private int getCategoryIcon(String categoryKey) {
        switch (categoryKey) {
            case CategoryConstants.CATEGORY_FOOD:
                return R.mipmap.foodandbaverages;
            case CategoryConstants.CATEGORY_SHOPPING:
                return R.mipmap.shopping;
            case CategoryConstants.CATEGORY_FAMILY:
                return R.mipmap.family;
            case CategoryConstants.CATEGORY_SAVINGS:
                return R.mipmap.saving;
            case CategoryConstants.CATEGORY_BILLS:
                return R.mipmap.bill;
            case CategoryConstants.CATEGORY_ENTERTAINMENT:
                return R.mipmap.entertainment;
            case CategoryConstants.CATEGORY_GIFTS:
                return R.mipmap.gift;
            case CategoryConstants.CATEGORY_TRAVEL:
                return R.mipmap.transportation;
            case CategoryConstants.CATEGORY_EDUCATION:
                return R.mipmap.education;
            case CategoryConstants.CATEGORY_HOTEL:
                return R.mipmap.travelandtourism;
            case CategoryConstants.CATEGORY_INSURANCE:
                return R.mipmap.insuarance;
            case CategoryConstants.CATEGORY_WITHDRAWAL:
                return R.mipmap.withdrawal;
            case CategoryConstants.CATEGORY_CREDIT:
                return R.mipmap.loan;
            default:
                return R.mipmap.others;
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void setOnItemClickListener(MyClickListener listener) {
        this.mCallback = listener;
    }

    public interface MyClickListener {
        void onItemClick(int position, View v);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView categoryIcon;
        TextView categoryName;
        TextView transactionNote;
        TextView transactionAmount;

        public ViewHolder(View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            categoryName = itemView.findViewById(R.id.categoryName);
            transactionNote = itemView.findViewById(R.id.transactionNote);
            transactionAmount = itemView.findViewById(R.id.transactionAmount);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // Handle click event if needed
        }
    }
}
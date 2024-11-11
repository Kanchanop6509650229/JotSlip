package com.example.finalproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private List<TransferSlip> dataSet;
    private MyClickListener mCallback;

    public ListAdapter(List<TransferSlip> myDataSet) {
        this.dataSet = myDataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TransferSlip slip = dataSet.get(position);

        // Set category icon based on category name
        // TODO: Add proper category icons
        holder.categoryIcon.setImageResource(R.drawable.ic_launcher_foreground);

        // Set category name
        holder.categoryName.setText(slip.getCategory());

        // Set transaction note/description
        String description = slip.getDescription();
        if (description != null && !description.isEmpty()) {
            holder.transactionNote.setText(description);
        } else {
            holder.transactionNote.setText("ไม่มีบันทึกเพิ่มเติม");
        }

        // Set amount with proper formatting and color based on type
        String amount;
        if (slip.getType() == 1) { // รายรับ
            amount = String.format("+ %.2f ฿", slip.getAmount());
            holder.transactionAmount.setTextColor(holder.itemView.getContext()
                .getResources().getColor(android.R.color.holo_green_dark));
        } else { // รายจ่าย
            amount = String.format("- %.2f ฿", slip.getAmount());
            holder.transactionAmount.setTextColor(holder.itemView.getContext()
                .getResources().getColor(android.R.color.holo_red_dark));
        }
        holder.transactionAmount.setText(amount);
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

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
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
            if (mCallback != null) {
                mCallback.onItemClick(getAdapterPosition(), v);
            }
        }
    }
} 
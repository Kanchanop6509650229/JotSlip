package com.example.finalproject;

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

        // Set category name
        holder.categoryName.setText(slip.getCategory());

        if (isFromMainActivity) {
            // แยกวันที่และเวลาจาก dateTime
            String[] dateTimeParts = slip.getDateTime().split(" ");
            if (dateTimeParts.length > 0) {
                String[] dateParts = dateTimeParts[0].split("/");
                if (dateParts.length == 3) {
                    // แปลงรูปแบบวันที่เป็น "dd เดือน พ.ศ."
                    String[] thaiMonths = { "", "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม" };

                    String day = dateParts[0];
                    int monthIndex = Integer.parseInt(dateParts[1]);
                    String month = thaiMonths[monthIndex];
                    String year = dateParts[2];

                    holder.transactionNote.setText(String.format("%s %s %s", day, month, year));
                }
            }
        } else {
            // แสดง description ตามปกติ
            String description = slip.getDescription();
            if (description != null && !description.isEmpty()) {
                holder.transactionNote.setText(description);
            } else {
                holder.transactionNote.setText("ไม่มีบันทึกเพิ่มเติม");
            }
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

        holder.itemView.setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, SlipInfoActivity.class);
                intent.putExtra("slip_id", slip.getId());
                context.startActivity(intent);
            }
        });
    }

    private int getCategoryIcon(String category) {
        switch (category) {
            case "อาหาร/เครื่องดื่ม":
                return R.mipmap.foodandbaverages;
            case "ช็อปปิ้ง":
                return R.mipmap.shopping;
            case "ครอบครัว/ส่วนตัว":
                return R.mipmap.family;
            case "ออมเงิน/ลงทุน":
                return R.mipmap.saving;
            case "ชำระบิล":
                return R.mipmap.bill;
            case "บันเทิง":
                return R.mipmap.entertainment;
            case "ของขวัญ/บริจาค":
                return R.mipmap.gift;
            case "ค่าเดินทาง":
                return R.mipmap.transportation;
            case "การศึกษา":
                return R.mipmap.education;
            case "โรงแรม/ท่องเที่ยว":
                return R.mipmap.travelandtourism;
            case "ประกัน":
                return R.mipmap.insuarance;
            case "ถอนเงิน":
                return R.mipmap.withdrawal;
            case "สินเชื่อ/เช่าซื้อ":
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
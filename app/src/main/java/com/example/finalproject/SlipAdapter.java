package com.example.finalproject;

import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;
import java.util.Calendar;

public class SlipAdapter extends RecyclerView.Adapter<SlipAdapter.ViewHolder> {
    private List<TransferSlip> dataSet;
    private MyClickListener mCallback;

    public SlipAdapter(List<TransferSlip> myDataSet) {
        this.dataSet = myDataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TransferSlip slip = dataSet.get(position);
        
        // แยกวันที่และเวลา
        String[] dateTimeComponents = slip.getDateTime().split(" ");
        if (dateTimeComponents.length > 0) {
            String dateStr = dateTimeComponents[0]; // เอาเฉพาะส่วนวันที่
            
            // Parse date components
            String[] dateComponents = dateStr.split("/");
            if (dateComponents.length >= 3) {
                // Set date (day of month)
                holder.date.setText(dateComponents[0]);
                
                // แปลงวันที่เป็น Calendar object
                Calendar calendar = Calendar.getInstance();
                try {
                    int day = Integer.parseInt(dateComponents[0]);
                    int month = Integer.parseInt(dateComponents[1]) - 1; // Calendar เริ่มที่ 0
                    int year = Integer.parseInt(dateComponents[2]) - 543; // แปลงปี พ.ศ. เป็น ค.ศ.
                    calendar.set(year, month, day);
                    
                    // ตรวจสอบว่าเป็นวันนี้หรือไม่
                    Calendar today = Calendar.getInstance();
                    if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                        holder.day.setText("วันนี้");
                    } else {
                        // แปลงเป็นชื่อวันภาษาไทย
                        String[] thaiDays = {"อาทิตย์", "จันทร์", "อังคาร", "พุธ", "พฤหัสบดี", "ศุกร์", "เสาร์"};
                        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Calendar.DAY_OF_WEEK เริ่มที่ 1
                        holder.day.setText("วัน" + thaiDays[dayOfWeek]);
                    }

                    // แปลงเลขเดือนเป็นชื่อเดือนภาษาไทย
                    String[] thaiMonths = {
                        "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
                    };
                    String monthYear = thaiMonths[month] + " " + dateComponents[2];
                    
                    // เพิ่มเวลาถ้ามี
                    if (dateTimeComponents.length > 1) {
                        monthYear += " " + dateTimeComponents[1];
                    }
                    holder.monthYearTime.setText(monthYear);
                    
                } catch (NumberFormatException e) {
                    Log.e("SlipAdapter", "Error parsing date: " + e.getMessage());
                    holder.day.setText("วันที่");
                    holder.monthYearTime.setText("เดือน/ปี");
                }
                
                // Set total amount
                String amount = String.format("%.2f ฿", slip.getAmount());
                holder.totalAmountPerDay.setText(amount);
            }
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

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView date;
        TextView day;
        TextView monthYearTime;
        TextView totalAmountPerDay;
        RecyclerView listItemView;

        public ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
            day = itemView.findViewById(R.id.day);
            monthYearTime = itemView.findViewById(R.id.monthYearTime);
            totalAmountPerDay = itemView.findViewById(R.id.totalAmountPerDay);
            listItemView = itemView.findViewById(R.id.list_item_view);
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

package com.demo.JotSlip;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;
import java.util.Calendar;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class SlipAdapter extends RecyclerView.Adapter<SlipAdapter.ViewHolder> {
    private List<TransferSlip> dataSet;
    private List<List<TransferSlip>> groupedSlips;
    private MyClickListener mCallback;
    private Context context;

    public SlipAdapter(List<TransferSlip> myDataSet) {
        this.dataSet = myDataSet;
        groupSlipsByDate();
    }   

    private void groupSlipsByDate() {
        groupedSlips = new ArrayList<>();
        Map<String, List<TransferSlip>> dateGroups = new HashMap<>();

        for (TransferSlip slip : dataSet) {
            String date = slip.getDateTime().split(" ")[0];
            if (!dateGroups.containsKey(date)) {
                dateGroups.put(date, new ArrayList<>());
            }
            dateGroups.get(date).add(slip);
        }

        List<String> sortedDates = new ArrayList<>(dateGroups.keySet());
        Collections.sort(sortedDates, Collections.reverseOrder());

        for (String date : sortedDates) {
            groupedSlips.add(dateGroups.get(date));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        List<TransferSlip> dailySlips = groupedSlips.get(position);
        TransferSlip firstSlip = dailySlips.get(0);

        String[] dateTimeComponents = firstSlip.getDateTime().split(" ");
        if (dateTimeComponents.length > 0) {
            String dateStr = dateTimeComponents[0];
            String[] dateComponents = dateStr.split("/");
            if (dateComponents.length >= 3) {
                holder.date.setText(dateComponents[0]);

                Calendar calendar = Calendar.getInstance();
                try {
                    int day = Integer.parseInt(dateComponents[0]);
                    int month = Integer.parseInt(dateComponents[1]) - 1;
                    int year = Integer.parseInt(dateComponents[2]) - 543;
                    calendar.set(year, month, day);

                    Calendar today = Calendar.getInstance();
                    if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                        holder.day.setText(holder.itemView.getContext().getString(R.string.today));
                    } else {
                        String[] thaiDays = holder.itemView.getContext().getResources().getStringArray(R.array.days_of_week);
                        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                        holder.day.setText(holder.itemView.getContext().getString(R.string.day_prefix) + thaiDays[dayOfWeek]);
                    }

                    String[] months = holder.itemView.getContext().getResources().getStringArray(R.array.months_array);
                    String monthYear = months[month] + " " + dateComponents[2];
                    if (dateTimeComponents.length > 1) {
                        monthYear += " " + dateTimeComponents[1];
                    }
                    holder.monthYearTime.setText(monthYear);

                    double totalIncome = 0;
                    double totalExpense = 0;
                    for (TransferSlip slip : dailySlips) {
                        if (slip.getType() == 1) { // รายรับ
                            totalIncome += slip.getAmount();
                        } else { // รายจ่าย
                            totalExpense += slip.getAmount();
                        }
                    }
                    double totalAmount = totalIncome - totalExpense;

                    // Set total amount with color
                    if (totalAmount >= 0) {
                        holder.totalAmountPerDay.setTextColor(holder.itemView.getContext()
                            .getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        holder.totalAmountPerDay.setTextColor(holder.itemView.getContext()
                            .getResources().getColor(android.R.color.holo_red_dark));
                    }
                    holder.totalAmountPerDay.setText(String.format(
                        holder.itemView.getContext().getString(R.string.amount_format_with_currency), 
                        totalAmount));

                } catch (NumberFormatException e) {
                    Log.e("SlipAdapter", "Error parsing date: " + e.getMessage());
                    holder.day.setText(holder.itemView.getContext().getString(R.string.default_date));
                    holder.monthYearTime.setText(holder.itemView.getContext().getString(R.string.default_month_year));
                }
            }
        }

        holder.listAdapter = new ListAdapter(dailySlips, false, holder.itemView.getContext());
        holder.listItemView.setAdapter(holder.listAdapter);
    }

    @Override
    public int getItemCount() {
        return groupedSlips.size();
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
        ListAdapter listAdapter;

        public ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
            day = itemView.findViewById(R.id.day);
            monthYearTime = itemView.findViewById(R.id.monthYearTime);
            totalAmountPerDay = itemView.findViewById(R.id.totalAmountPerDay);
            listItemView = itemView.findViewById(R.id.list_item_view);
            
            listItemView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
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

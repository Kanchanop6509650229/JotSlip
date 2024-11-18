package com.example.finalproject;

import static android.provider.BaseColumns._ID;
import static com.example.finalproject.Constants.*;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActivityOptions;

public class HistoryActivity extends AppCompatActivity {
    private EventsData events;
    private LineChart chart;
    private TextView monthText;
    private TextView yearText;
    private int selectedMonth;
    private int selectedYear;
    private TextView remainAmountText;
    private TextView remainAmount;
    private TextView income;
    private TextView outcome;
    private ImageButton addbtn;
    private RecyclerView recyclerView;
    private View historyNav;
    private ImageView historyIcon;
    private TextView historyText;
    private View homeNav;
    private ImageView homeIcon;
    private TextView homeText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final String[] MONTHS = new String[] { "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.history);

        // Initialize views
        chart = findViewById(R.id.chart);
        monthText = findViewById(R.id.monthText);
        yearText = findViewById(R.id.yearText);
        remainAmountText = findViewById(R.id.remainAmountText);
        remainAmount = findViewById(R.id.remainAmount);
        income = findViewById(R.id.income);
        outcome = findViewById(R.id.outcome);
        
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(true);
        
        // Initialize database
        events = new EventsData(HistoryActivity.this);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.green_500,  // สีหลัก
            R.color.blue_500,   // สีรอง
            R.color.orange_500  // สีที่สาม
        );
        
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.white);
        
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        
        swipeRefreshLayout.setSlingshotDistance(100);
        
        swipeRefreshLayout.setProgressViewOffset(false, 0, 100);
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            View contentView = findViewById(R.id.main);
            contentView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .withEndAction(() -> {
                    updateChartData();
                    
                    contentView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();
                    
                    swipeRefreshLayout.setRefreshing(false);
                })
                .start();
        });

        addbtn = findViewById(R.id.add_btn);
        addbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSlipActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        setupNavigation();
        setupChart();

        updateChartData();

        ImageButton addBtn = findViewById(R.id.add_btn);
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_button);

        addBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(scaleAnimation);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.clearAnimation();
                    v.setScaleX(1);
                    v.setScaleY(1);
                    break;
            }
            return false;
        });

        // Add after initializing views
        historyNav = findViewById(R.id.nav_history);
        historyIcon = historyNav.findViewById(android.R.id.icon);
        historyText = historyNav.findViewById(android.R.id.text1);

        homeNav = findViewById(R.id.nav_home);
        homeIcon = homeNav.findViewById(R.id.home_icon);
        homeText = homeNav.findViewById(R.id.home_text);
        homeNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);

            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, options);
        });

        // Highlight history icon and text
        homeIcon.setColorFilter(getColor(R.color.gray));
        homeText.setTextColor(getColor(R.color.gray));
    }

    private void setupNavigation() {
        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedYear = calendar.get(Calendar.YEAR) + 543;

        // หาข้อมูลที่ใกล้ปัจจุบันที่สุด
        findNearestData(selectedMonth, selectedYear);
        updateDisplayTexts();

        findViewById(R.id.prevYear).setOnClickListener(v -> {
            int newYear = findPreviousYearWithData(selectedYear);
            if (newYear != selectedYear) {
                selectedYear = newYear;
                selectedMonth = findLatestMonthWithData(selectedYear);
                updateDisplayTexts();
                updateChartData();
                Log.d("MainActivity", "Moved to: " + (selectedMonth + 1) + "/" + selectedYear);
            }
        });

        findViewById(R.id.nextYear).setOnClickListener(v -> {
            int newYear = findNextYearWithData(selectedYear);
            if (newYear != selectedYear) {
                selectedYear = newYear;
                selectedMonth = findEarliestMonthWithData(selectedYear);
                updateDisplayTexts();
                updateChartData();
                Log.d("MainActivity", "Moved to: " + (selectedMonth + 1) + "/" + selectedYear);
            }
        });

        findViewById(R.id.prevMonth).setOnClickListener(v -> {
            int newMonth = findPreviousMonthWithData(selectedYear, selectedMonth);
            if (newMonth == -1) {
                int newYear = findPreviousYearWithData(selectedYear);
                if (newYear != selectedYear) {
                    selectedYear = newYear;
                    selectedMonth = findLatestMonthWithData(selectedYear);
                }
            } else {
                selectedMonth = newMonth;
            }
            updateDisplayTexts();
            updateChartData();
            Log.d("MainActivity", "Moved to: " + (selectedMonth + 1) + "/" + selectedYear);
        });

        findViewById(R.id.nextMonth).setOnClickListener(v -> {
            int newMonth = findNextMonthWithData(selectedYear, selectedMonth);
            if (newMonth == -1) {
                int newYear = findNextYearWithData(selectedYear);
                if (newYear != selectedYear) {
                    selectedYear = newYear;
                    selectedMonth = findEarliestMonthWithData(selectedYear);
                }
            } else {
                selectedMonth = newMonth;
            }
            updateDisplayTexts();
            updateChartData();
            Log.d("MainActivity", "Moved to: " + (selectedMonth + 1) + "/" + selectedYear);
        });
    }

    private void findNearestData(int currentMonth, int currentYear) {
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID, DATE };

        // ตรวจสอบข้อมูลในเดือนปัจจุบันก่อน
        String monthStr = String.format("%02d", currentMonth + 1);
        String selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ?";
        String[] selectionArgs = { monthStr, String.valueOf(currentYear) };
        Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            // ถ้ามีข้อมูลในเดือนปัจจุบัน ใช้เดือนปัจจุบัน
            selectedMonth = currentMonth;
            selectedYear = currentYear;
            cursor.close();
            return;
        }

        // ถ้าไม่มีข้อมูลในเดือนปัจจุบัน หาเดือนที่ใกล้เคียงที่สุด
        int closestMonth = currentMonth;
        int closestYear = currentYear;
        int minDistance = Integer.MAX_VALUE;

        // ค้นหาในช่วง ±6 เดือนจากเดือนปัจจุบัน
        for (int monthOffset = -6; monthOffset <= 6; monthOffset++) {
            int targetMonth = currentMonth;
            int targetYear = currentYear;

            // ปรับเดือนและปีตาม offset
            targetMonth += monthOffset;
            while (targetMonth < 0) {
                targetMonth += 12;
                targetYear--;
            }
            while (targetMonth >= 12) {
                targetMonth -= 12;
                targetYear++;
            }

            monthStr = String.format("%02d", targetMonth + 1);
            selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ?";
            selectionArgs = new String[] { monthStr, String.valueOf(targetYear) };
            cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                int distance = Math.abs(monthOffset);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestMonth = targetMonth;
                    closestYear = targetYear;
                }
                cursor.close();
            }
        }

        selectedMonth = closestMonth;
        selectedYear = closestYear;
    }

    private int findPreviousYearWithData(int currentYear) {
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID };

        for (int year = currentYear - 1; year >= currentYear - 10; year--) {
            String selection = "substr(date, -4) = ?";
            String[] selectionArgs = { String.valueOf(year) };
            Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return year;
            }
            if (cursor != null)
                cursor.close();
        }
        return currentYear;
    }

    private int findNextYearWithData(int currentYear) {
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID };

        for (int year = currentYear + 1; year <= currentYear + 10; year++) {
            String selection = "substr(date, -4) = ?";
            String[] selectionArgs = { String.valueOf(year) };
            Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return year;
            }
            if (cursor != null)
                cursor.close();
        }
        return currentYear;
    }

    private int findPreviousMonthWithData(int year, int currentMonth) {
        String[] FROM = { _ID };
        SQLiteDatabase db = events.getReadableDatabase();

        for (int month = currentMonth - 1; month >= 0; month--) {
            String monthStr = String.format("%02d", month + 1);
            String selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ?";
            String[] selectionArgs = { monthStr, String.valueOf(year) };
            Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return month;
            }
            if (cursor != null)
                cursor.close();
        }
        return -1;
    }

    private int findNextMonthWithData(int year, int currentMonth) {
        String[] FROM = { _ID };
        SQLiteDatabase db = events.getReadableDatabase();

        for (int month = currentMonth + 1; month < 12; month++) {
            String monthStr = String.format("%02d", month + 1);
            String selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ?";
            String[] selectionArgs = { monthStr, String.valueOf(year) };
            Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return month;
            }
            if (cursor != null)
                cursor.close();
        }
        return -1;
    }

    private int findEarliestMonthWithData(int year) {
        return findNextMonthWithData(year, -1);
    }

    private int findLatestMonthWithData(int year) {
        return findPreviousMonthWithData(year, 12);
    }

    private void updateDisplayTexts() {
        TextView monthText = findViewById(R.id.monthText);
        TextView yearText = findViewById(R.id.yearText);

        String[] MONTHS = { "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม" };

        monthText.setText(MONTHS[selectedMonth]);
        yearText.setText(String.valueOf(selectedYear));
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.WHITE);

        // Customize Y-Axis (Money)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        // Customize X-Axis (Days)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%d/%d", (int) value, selectedMonth + 1);
            }
        });
    }

    private void updateChartData() {
        Cursor cursor = getEvents();
        List<TransferSlip> slipList = new ArrayList<>();
        Map<Integer, Float> incomeDayMap = new HashMap<>();
        Map<Integer, Float> expenseDayMap = new HashMap<>();
        float maxMoney = 0f;
        float incomeMoney = 0f;
        float expenseMoney = 0f;

        // Update UI with zero values by default
        income.setText("0.00 ฿");
        outcome.setText("0.00 ฿");
        remainAmount.setText("0.00 ฿");
        remainAmount.setTextColor(getColor(R.color.green_500));

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(_ID));
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                double money = cursor.getDouble(cursor.getColumnIndex(MONEY));
                String dateStr = cursor.getString(cursor.getColumnIndex(DATE));
                String timeStr = cursor.getString(cursor.getColumnIndex(TIME));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                String category = cursor.getString(cursor.getColumnIndex(CATEGORY));
                String receiver = cursor.getString(cursor.getColumnIndex(RECEIVER));
                String image = cursor.getString(cursor.getColumnIndex(IMAGE));

                try {
                    String[] dateParts = dateStr.split("/");
                    int day = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int year = Integer.parseInt(dateParts[2]);

                    if (month == selectedMonth + 1 && year == selectedYear) {
                        // Create TransferSlip object and add to list
                        TransferSlip slip = new TransferSlip(
                                id,
                                dateStr + " " + timeStr,
                                money,
                                receiver,
                                description,
                                image,
                                category,
                                type);
                        slipList.add(slip);

                        // Combine transactions of the same type on the same day
                        if (type == 1) {
                            float currentDayTotal = incomeDayMap.getOrDefault(day, 0f);
                            currentDayTotal += (float) money;
                            incomeDayMap.put(day, currentDayTotal);
                            incomeMoney += (float) money;

                            if (currentDayTotal > maxMoney) {
                                maxMoney = currentDayTotal;
                            }
                        } else {
                            float currentDayTotal = expenseDayMap.getOrDefault(day, 0f);
                            currentDayTotal += (float) money;
                            expenseDayMap.put(day, currentDayTotal);
                            expenseMoney += (float) money;

                            if (currentDayTotal > maxMoney) {
                                maxMoney = currentDayTotal;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error parsing date: " + dateStr, e);
                }
            } while (cursor.moveToNext());

            // Update UI
            income.setText(String.format("%.2f", incomeMoney) + " ฿");
            outcome.setText(String.format("%.2f", expenseMoney) + " ฿");
            remainAmount.setText(String.format("%.2f", incomeMoney - expenseMoney) + " ฿");
            if (incomeMoney - expenseMoney < 0) {
                remainAmount.setTextColor(getColor(R.color.red));
            } else {
                remainAmount.setTextColor(getColor(R.color.green_500));
            }

            // Convert maps to entry lists
            List<Entry> incomeEntries = new ArrayList<>();
            List<Entry> expenseEntries = new ArrayList<>();

            for (Map.Entry<Integer, Float> entry : incomeDayMap.entrySet()) {
                incomeEntries.add(new Entry(entry.getKey(), entry.getValue()));
            }

            for (Map.Entry<Integer, Float> entry : expenseDayMap.entrySet()) {
                expenseEntries.add(new Entry(entry.getKey(), entry.getValue()));
            }

            // Set adapter
            SlipAdapter adapter = new SlipAdapter(slipList);
            recyclerView.setAdapter(adapter);

            cursor.close();

            // Update chart
            updateChartWithData(maxMoney, incomeEntries, expenseEntries);
        }

        // If no data was found, show empty chart
        if (cursor == null || cursor.getCount() == 0) {
            List<Entry> emptyIncomeEntries = new ArrayList<>();
            List<Entry> emptyExpenseEntries = new ArrayList<>();
            updateChartWithData(1000f, emptyIncomeEntries, emptyExpenseEntries);

            // Set empty adapter
            SlipAdapter adapter = new SlipAdapter(slipList);
            recyclerView.setAdapter(adapter);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void updateChartWithData(float maxMoney, List<Entry> incomeEntries, List<Entry> expenseEntries) {
        // Sort entries by X value (day)
        Collections.sort(incomeEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
        Collections.sort(expenseEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));

        // Rest of your existing code
        float axisMaximum = maxMoney == 0f ? 1000f : maxMoney + 200f;
        chart.getAxisLeft().setAxisMaximum(axisMaximum);
        chart.getAxisLeft().setAxisMinimum(0f);

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear - 543, selectedMonth, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        chart.getXAxis().setAxisMinimum(1f);
        chart.getXAxis().setAxisMaximum(lastDay);

        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "รายรับ");
        incomeDataSet.setColor(Color.GREEN);
        incomeDataSet.setCircleColor(Color.GREEN);
        incomeDataSet.setDrawValues(true);
        incomeDataSet.setDrawFilled(true);
        incomeDataSet.setFillColor(Color.GREEN);
        incomeDataSet.setFillAlpha(50);

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "รายจ่าย");
        expenseDataSet.setColor(Color.RED);
        expenseDataSet.setCircleColor(Color.RED);
        expenseDataSet.setDrawValues(true);
        expenseDataSet.setDrawFilled(true);
        expenseDataSet.setFillColor(Color.RED);
        expenseDataSet.setFillAlpha(50);

        // Add empty state message if no data
        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            chart.setNoDataText("ไม่มีข้อมูลในเดือนนี้");
            chart.setNoDataTextColor(Color.GRAY);
        }

        LineData lineData = new LineData(incomeDataSet, expenseDataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private Cursor getEvents() {
        String[] FROM = { _ID, TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE };
        String ORDER_BY = DATE + " ASC";
        SQLiteDatabase db = events.getReadableDatabase();

        // สร้างรูปแบบวันที่สำหรับการค้นหา
        String startDay = String.format("01/%02d/%d", selectedMonth + 1, selectedYear);

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear - 543, selectedMonth, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        String endDay = String.format("%02d/%02d/%d", lastDay, selectedMonth + 1, selectedYear);

        // ใช้การเปรียบเทียบวันที่โดยตรง
        String selection = "date BETWEEN ? AND ?";
        String[] selectionArgs = { startDay, endDay };

        Log.d("MainActivity", "Querying for dates between: " + startDay + " and " + endDay);

        return db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, ORDER_BY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateChartData();
    }

    class DecimalDigitsInputFilter implements InputFilter {
        private Pattern mPattern;

        DecimalDigitsInputFilter(int digits, int digitsAfterZero) {
            mPattern = Pattern.compile("[0-9]{0," + (digits - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) +
                    "})?)||(\\.)?");
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Matcher matcher = mPattern.matcher(dest);
            if (!matcher.matches())
                return "";
            return null;
        }
    }

    // Add this utility method to help with number formatting
    private String formatNumber(float number) {
        DecimalFormat df = new DecimalFormat("###,###,###,###.##");
        return df.format(number);
    }
}
package com.example.finalproject;

import static android.provider.BaseColumns._ID;
import static com.example.finalproject.Constants.CATEGORY;
import static com.example.finalproject.Constants.DATE;
import static com.example.finalproject.Constants.DESCRIPTION;
import static com.example.finalproject.Constants.IMAGE;
import static com.example.finalproject.Constants.MONEY;
import static com.example.finalproject.Constants.RECEIVER;
import static com.example.finalproject.Constants.TABLE_NAME;
import static com.example.finalproject.Constants.TIME;
import static com.example.finalproject.Constants.TYPE;

import android.app.ActivityOptions;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryActivity extends AppCompatActivity {
    private EventsData events;
    private PieChart chart;
    private TextView monthText;
    private TextView yearText;
    private int selectedMonth;
    private int selectedYear;
    private ImageButton addbtn;
    private RecyclerView categoryRecyclerView;
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
        setContentView(R.layout.category);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.green_500, // สีหลัก
                R.color.blue_500, // สีรอง
                R.color.orange_500 // สีที่สาม
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
                        updateCategoryData();

                        contentView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();

                        swipeRefreshLayout.setRefreshing(false);
                    })
                    .start();
        });

        // Initialize views
        chart = findViewById(R.id.chart);
        monthText = findViewById(R.id.monthText);
        yearText = findViewById(R.id.yearText);

        // Initialize RecyclerView
        categoryRecyclerView = findViewById(R.id.category_list_item_view);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryRecyclerView.setNestedScrollingEnabled(true);

        // Initialize database
        events = new EventsData(CategoryActivity.this);

        addbtn = findViewById(R.id.add_btn);
        addbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSlipActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.hold);
        });

        setupNavigation();
        setupChart();

        updateChartData();
        updateCategoryData();

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
        historyNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);

            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, options);
        });
        historyIcon.setColorFilter(getColor(R.color.gray));
        historyText.setTextColor(getColor(R.color.gray));

        homeNav = findViewById(R.id.nav_home);
        homeIcon = homeNav.findViewById(R.id.home_icon);
        homeText = homeNav.findViewById(R.id.home_text);
        homeNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);

            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, options);
        });
        homeIcon.setColorFilter(getColor(R.color.gray));
        homeText.setTextColor(getColor(R.color.gray));
    }

    private void updateCategoryData() {
        ArrayList<String> categoryList = new ArrayList<>();
        ArrayList<TransferSlip> slipList = new ArrayList<>();

        Cursor cursor = getEvents();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(_ID));
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                double money = cursor.getDouble(cursor.getColumnIndex(MONEY));
                String dateStr = cursor.getString(cursor.getColumnIndex(DATE));
                String timeStr = cursor.getString(cursor.getColumnIndex(TIME));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                String receiver = cursor.getString(cursor.getColumnIndex(RECEIVER));
                String image = cursor.getString(cursor.getColumnIndex(IMAGE));
                String category = cursor.getString(cursor.getColumnIndex(CATEGORY));

                try {
                    String[] dateParts = dateStr.split("/");
                    int day = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int year = Integer.parseInt(dateParts[2]);

                    if (month == selectedMonth + 1 && year == selectedYear) {
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
                    }
                } catch (Exception e) {
                    Log.e("CategoryActivity", "Error parsing date: " + dateStr, e);
                }
            } while (cursor.moveToNext());

            CategoryAdapter adapter = new CategoryAdapter(slipList);
            categoryRecyclerView.setAdapter(adapter);

            cursor.close();
        }
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
                updateCategoryData();
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
                updateCategoryData();
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
            updateCategoryData();
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
            updateCategoryData();
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

        // คนหาในช่วง ±6 เดือนจากเดือนปัจจุบัน
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
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(61f);
        chart.setDrawCenterText(true);
        chart.setCenterText("รายจ่ายทั้งหมด");
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
    }

    private void updateChartData() {
        Cursor cursor = getEvents();
        Map<String, Float> categoryExpenses = new HashMap<>();
        float totalExpense = 0f;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(_ID));
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                float money = cursor.getFloat(cursor.getColumnIndex(MONEY));
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
                        if (type != 1) { // Assuming type != 1 signifies an expense
                            float currentValue = categoryExpenses.getOrDefault(category, 0f);
                            float newValue = currentValue + money;
                            categoryExpenses.put(category, newValue);
                            totalExpense += money;
                        }
                    }
                } catch (Exception e) {
                    Log.e("CategoryActivity", "Error parsing date: " + dateStr, e);
                }
            } while (cursor.moveToNext());
            cursor.close();

            // Update chart with category data
            updateChartWithData(categoryExpenses, totalExpense);
        } else {
            // Show empty chart
            chart.setData(null);
            chart.setCenterText("ไม่มีข้อมูลในเดือนนี้");
            chart.invalidate();
        }
    }

    private void updateChartWithData(Map<String, Float> categoryExpenses, float totalExpense) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        // Define colors for different categories
        int[] CHART_COLORS = new int[] {
                Color.rgb(64, 89, 128), Color.rgb(149, 165, 124),
                Color.rgb(217, 184, 162), Color.rgb(191, 134, 134),
                Color.rgb(179, 48, 80), Color.rgb(193, 37, 82),
                Color.rgb(255, 102, 0), Color.rgb(245, 199, 0),
                Color.rgb(106, 150, 31), Color.rgb(179, 100, 53)
        };

        int colorIndex = 0;
        for (Map.Entry<String, Float> entry : categoryExpenses.entrySet()) {
            float percentage = (entry.getValue() / totalExpense) * 100;
            if (percentage > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                colors.add(CHART_COLORS[colorIndex % CHART_COLORS.length]);
                colorIndex++;
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f%%", (value / totalExpense) * 100);
            }
        });
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        chart.setCenterText(String.format("รายจ่ายทั้งหมด\n%.2f ฿", totalExpense));
        chart.setCenterTextSize(14f);
        chart.setData(data);
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

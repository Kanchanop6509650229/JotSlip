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
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageButton addbtn;
    private RecyclerView recyclerView;
    private RecyclerView categoryRecyclerView;
    private View historyNav;
    private ImageView historyIcon;
    private TextView historyText;
    private View homeNav;
    private ImageView homeIcon;
    private TextView homeText;
    private EventsData events;
    private TextView remainAmount;
    private TextView seeAllTransaction;
    private TextView seeAllTransaction2;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View navCategory;
    private ImageView categoryIcon;
    private TextView categoryText;
    private View navSettings;
    private ImageView settingsIcon;
    private TextView settingsText;

    private static final String PREF_NAME = "AppSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_FONT_SIZE = "fontSize";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.homepage);

        events = new EventsData(this);

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
                        updateBarChartData();
                        updateCategoryData();
                        getRemainMoney();

                        contentView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();

                        swipeRefreshLayout.setRefreshing(false);
                    })
                    .start();
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.list_item_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(true);

        categoryRecyclerView = findViewById(R.id.category_list_item_view);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryRecyclerView.setNestedScrollingEnabled(true);

        addbtn = findViewById(R.id.add_btn);
        addbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSlipActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.hold);
        });

        ImageButton addBtn = findViewById(R.id.add_btn);
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_button);

        remainAmount = findViewById(R.id.totalRemain);
        getRemainMoney();

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

        navSettings = findViewById(R.id.nav_settings);
        settingsIcon = findViewById(R.id.settings_icon);
        settingsText = findViewById(R.id.settings_text);
        settingsIcon.setColorFilter(getColor(R.color.gray));
        settingsText.setTextColor(getColor(R.color.gray));

        navSettings.setOnClickListener(v -> showSettingsDialog());

        navCategory = findViewById(R.id.nav_category);
        categoryIcon = findViewById(R.id.category_icon);
        categoryText = findViewById(R.id.category_text);

        navCategory.setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryActivity.class);
            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, options);
        });

        seeAllTransaction = findViewById(R.id.seeAllText);
        seeAllTransaction.setOnClickListener(this);
        seeAllTransaction2 = findViewById(R.id.seeAllText2);
        seeAllTransaction2.setOnClickListener(this);

        // Add after initializing views
        historyNav = findViewById(R.id.nav_history);
        historyIcon = historyNav.findViewById(android.R.id.icon);
        historyText = historyNav.findViewById(android.R.id.text1);
        historyNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, options);
        });

        // ปรับแต่ง animation สำหรับ navigation

        historyNav.setOnTouchListener((v, event) -> {
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

        // ทำแบบเดียวกันสำหรับ home navigation
        homeNav = findViewById(R.id.nav_home);
        homeIcon = homeNav.findViewById(R.id.home_icon);
        homeText = homeNav.findViewById(R.id.home_text);

        homeNav.setOnTouchListener((v, event) -> {
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

        // Highlight history icon and text
        historyIcon.setColorFilter(getColor(R.color.gray));
        historyText.setTextColor(getColor(R.color.gray));

        updateCategoryData();
        setupBarChart();
        updateBarChartData();
    }

    private void getRemainMoney() {
        Cursor cursor = getEvents();
        List<TransferSlip> slipList = new ArrayList<>();
        float totalRemain = 0f;
        int count = 0;

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
                    if (type == 1) {
                        totalRemain += (float) money;
                    } else {
                        totalRemain -= (float) money;
                    }

                    if (count < 5) {
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
                        count++;
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error parsing date: " + dateStr, e);
                }
            } while (cursor.moveToNext());

            remainAmount.setText(formatNumber(totalRemain) + " ฿");
            if (totalRemain < 0) {
                remainAmount.setTextColor(getColor(R.color.red));
            } else {
                remainAmount.setTextColor(getColor(R.color.dark_green));
            }

            ListAdapter adapter = new ListAdapter(slipList, true, this);
            recyclerView.setAdapter(adapter);

            cursor.close();
        }
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

    private Cursor getEvents() {
        String[] FROM = { _ID, TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE };
        String ORDER_BY = "substr(date, -4) DESC, " + // ปี
                "substr(date, 4, 2) DESC, " + // เดือน
                "substr(date, 1, 2) DESC, " + // วัน
                TIME + " DESC"; // เวลา
        SQLiteDatabase db = events.getReadableDatabase();

        return db.query(TABLE_NAME, FROM, null, null, null, null, ORDER_BY);
    }

    @Override
    public void onClick(View v) {
        if (v == seeAllTransaction) {
            Intent intent = new Intent(this, HistoryActivity.class);

            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, options);
        } else if (v == seeAllTransaction2) {
            Intent intent = new Intent(this, CategoryActivity.class);

            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, options);
        }
    }

    private void setupBarChart() {
        BarChart barChart = findViewById(R.id.bar_chart);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);

        // Disable zooming and scaling
        barChart.setScaleEnabled(false); // ปิดการซูม
        barChart.setPinchZoom(false); // ปิดการ pinch zoom
        barChart.setDoubleTapToZoomEnabled(false); // ปิดการ double tap zoom
        barChart.setDragEnabled(false); // ปิดการลาก

        // ตั้งค่าแกน X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true); // จัดให้ label อยู่ตรงกลางของกลุ่มแท่ง
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, (int) value - 6);
                return String.format("%d/%d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1);
            }
        });

        // ตั้งค่าแกน Y ด้าซ้าย
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatNumber(value);
            }
        });

        // ปิดแกน Y ด้านขวา
        barChart.getAxisRight().setEnabled(false);

        // ตั้งค่าตำนาน (Legend)
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(true);
    }

    private void updateCategoryData() {
        ArrayList<String> categoryList = new ArrayList<>();
        ArrayList<TransferSlip> slipList = new ArrayList<>();

        Cursor cursor = getSevenDaysEvents();
        if (cursor != null) {
            Log.d("MainActivity", "Cursor is not null");
            while (cursor.moveToNext()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String columnName = cursor.getColumnName(i);
                    if (!columnName.equals(IMAGE)) { // Skip logging image data
                        Log.d("MainActivity", columnName + ": " + cursor.getString(i));
                    }
                }
            }
        } else {
            Log.d("MainActivity", "Cursor is null");
        }
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

            } while (cursor.moveToNext());

            CategoryAdapter adapter = new CategoryAdapter(slipList, true);
            categoryRecyclerView.setAdapter(adapter);

            cursor.close();
        }
    }

    private void updateBarChartData() {
        BarChart barChart = findViewById(R.id.bar_chart);
        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();

        // สร้าง Map เพื่อเก็บข้อมูลรายรับ-รายจ่ายแต่ละวัน
        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();

        // ดึงข้อมูล 7 วันล่าสุด
        Cursor cursor = getSevenDaysEvents();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                double money = cursor.getDouble(cursor.getColumnIndex(MONEY));
                String date = cursor.getString(cursor.getColumnIndex(DATE));

                if (type == 1) { // รายรับ
                    incomeMap.put(date, incomeMap.getOrDefault(date, 0.0) + money);
                } else { // รายจ่าย
                    expenseMap.put(date, expenseMap.getOrDefault(date, 0.0) + money);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // สร้างข้อมูลสำหรับกราฟแท่ง
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -6); // เริ่มจาก 6 วันย้อนหัง

        for (int i = 0; i < 7; i++) {
            String date = String.format("%02d/%02d/%d",
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR) + 543);

            float income = incomeMap.getOrDefault(date, 0.0).floatValue();
            float expense = expenseMap.getOrDefault(date, 0.0).floatValue();

            incomeEntries.add(new BarEntry(i, income));
            expenseEntries.add(new BarEntry(i, expense));

            cal.add(Calendar.DAY_OF_MONTH, 1); // เพิ่มวันทีละ 1 วัน
        }

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "รายรับ");
        incomeDataSet.setColor(Color.GREEN);
        incomeDataSet.setDrawValues(true);
        incomeDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0)
                    return ""; // ซ่อนค่า 0
                return formatNumber(value);
            }
        });

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "รายจ่าย");
        expenseDataSet.setColor(Color.RED);
        expenseDataSet.setDrawValues(true);
        expenseDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0)
                    return ""; // ซ่อนค่า 0
                return formatNumber(value);
            }
        });

        // เพิ่มระยะห่างระหว่างกลุ่มแท่ง
        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        BarData barData = new BarData(incomeDataSet, expenseDataSet);
        barData.setBarWidth(barWidth);
        barData.setValueTextSize(10f);

        barChart.setData(barData);
        barChart.groupBars(0, groupSpace, barSpace);

        // ปรับขอบขตของแกน X
        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(7);

        // แก้ไข ValueFormatter สำหรับแกน X
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -6 + (int) value); // ปรับสูตรการคำนวณวันที่
                return String.format("%d/%d",
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.MONTH) + 1);
            }
        });

        barChart.setVisibleXRangeMaximum(7);
        barChart.invalidate();
    }

    private Cursor getSevenDaysEvents() {
        String[] FROM = { _ID, TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE };
        SQLiteDatabase db = events.getReadableDatabase();

        // คำนวณวันที่ปัจจุบันและ 6 วันย้อนหลัง
        Calendar cal = Calendar.getInstance();
        String endDate = String.format("%02d/%02d/%d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR) + 543);

        cal.add(Calendar.DAY_OF_MONTH, -6);
        String startDate = String.format("%02d/%02d/%d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR) + 543);

        // แก้ไขการ query โดยใช้ฟังก์ชัน SQLite สำหรับแปลงวันที่
        String selection = "strftime('%Y%m%d', substr(date,7)||'-'||substr(date,4,2)||'-'||substr(date,1,2)) >= strftime('%Y%m%d', substr(?1,7)||'-'||substr(?1,4,2)||'-'||substr(?1,1,2)) "
                +
                "AND strftime('%Y%m%d', substr(date,7)||'-'||substr(date,4,2)||'-'||substr(date,1,2)) <= strftime('%Y%m%d', substr(?2,7)||'-'||substr(?2,4,2)||'-'||substr(?2,1,2))";
        String[] selectionArgs = { startDate, endDate };
        String orderBy = "strftime('%Y%m%d', substr(date,7)||'-'||substr(date,4,2)||'-'||substr(date,1,2)) ASC";

        return db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, orderBy);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.settings_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View resetDataCard = dialogView.findViewById(R.id.resetDataCard);

        resetDataCard.setOnClickListener(v -> {
            dialog.dismiss();
            showResetConfirmationDialog();
        });

        dialog.show();
    }

    private void showResetConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // สร้าง AlertDialog แบบกำหนดเอง
        View dialogView = getLayoutInflater().inflate(R.layout.reset_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // ทำให้พื้นหลัง dialog โปร่งใส
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();

        // ตั้งค่าการทำงานของปุ่มใน dialog
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnConfirm.setOnClickListener(v -> {
            resetAllData();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void resetAllData() {
        try {
            // ลบข้อมูลในฐานข้อมูล
            events = new EventsData(this);
            SQLiteDatabase db = events.getWritableDatabase();
            db.delete(TABLE_NAME, null, null);

            // อัพเดทการแสดงผล
            updateBarChartData();
            updateCategoryData();
            getRemainMoney();

            Toast.makeText(this, "รีเซ็ตข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "เกิดข้อผิดพลาดในการรีเซ็ตข้อมูล", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Error resetting data: " + e.getMessage());
        }
    }
}

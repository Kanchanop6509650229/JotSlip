package com.example.finalproject;

import static android.provider.BaseColumns._ID;
import static com.example.finalproject.Constants.*;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;

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
    private ImageButton exportButton;
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
        setContentView(R.layout.history);

        events = new EventsData(this);

        initializeViews();
        setupSwipeRefresh();
        setupNavigation();
        setupChart();
        setupExportButton();
        updateChartData();

        setupAddButton();
    }

    private void initializeViews() {
        chart = findViewById(R.id.chart);
        monthText = findViewById(R.id.monthText);
        yearText = findViewById(R.id.yearText);
        remainAmountText = findViewById(R.id.remainAmountText);
        remainAmount = findViewById(R.id.remainAmount);
        income = findViewById(R.id.income);
        outcome = findViewById(R.id.outcome);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(true);

        exportButton = findViewById(R.id.exportButton);

        historyNav = findViewById(R.id.nav_history);
        historyIcon = historyNav.findViewById(android.R.id.icon);
        historyText = historyNav.findViewById(android.R.id.text1);
        historyNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);

            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();

            startActivity(intent, options);
        });

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

    private boolean checkPermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    private void exportData() {
        List<TransferSlip> allTransactions = getAllTransactionsForCurrentMonth();
        double totalIncome = Double.parseDouble(income.getText().toString().replace(" ฿", ""));
        double totalExpense = Double.parseDouble(outcome.getText().toString().replace(" ฿", ""));

        // สร้าง Map เก็บข้อมูลกราฟรายวัน
        Map<String, Double> dailyBalances = new LinkedHashMap<>();

        // ดึงข้อมูลจาก LineChart
        if (chart.getLineData() != null && !chart.getLineData().getDataSets().isEmpty()) {
            ILineDataSet dataSet = chart.getLineData().getDataSets().get(0);
            for (int i = 0; i < dataSet.getEntryCount(); i++) {
                Entry entry = dataSet.getEntryForIndex(i);
                String date = String.format("%02d/%02d/%d",
                        (int) entry.getX(),
                        selectedMonth + 1,
                        selectedYear);
                dailyBalances.put(date, (double) entry.getY());
            }
        }

        DataExportUtil.exportHistoryData(
                this,
                monthText.getText().toString(),
                yearText.getText().toString(),
                totalIncome,
                totalExpense,
                allTransactions,
                chart,
                dailyBalances);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportData();
            } else {
                Toast.makeText(this,
                        "ต้องการสิทธิ์ในการเข้าถึงพื้นที่จัดเก็บเพื่อบันทึกไฟล์",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showExportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.export_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnExportCsv = dialogView.findViewById(R.id.btnExportCsv);
        Button btnExportImage = dialogView.findViewById(R.id.btnExportImage);

        btnExportCsv.setOnClickListener(v -> {
            if (checkPermissionAndExport()) {
                exportDataToCsv();
                dialog.dismiss();
            }
        });

        btnExportImage.setOnClickListener(v -> {
            if (checkPermissionAndExport()) {
                exportDataToImage();
                dialog.dismiss();
            }
        });
    }

    private void exportDataToCsv() {
        List<TransferSlip> allTransactions = getAllTransactionsForCurrentMonth();
        double totalIncome = Double.parseDouble(income.getText().toString().replace(" ฿", ""));
        double totalExpense = Double.parseDouble(outcome.getText().toString().replace(" ฿", ""));

        // คำนวณยอดคงเหลือรายวันจากรายการธุรกรรม
        Map<String, Double> dailyBalances = new LinkedHashMap<>();
        double runningBalance = 0;

        // เรียงลำดับรายการตามวันที่
        Collections.sort(allTransactions, (a, b) -> {
            String dateA = a.getDateTime().split(" ")[0];
            String dateB = b.getDateTime().split(" ")[0];
            return dateA.compareTo(dateB);
        });

        // คำนวณยอดคงเหลือสะสมรายวัน
        for (TransferSlip slip : allTransactions) {
            String date = slip.getDateTime().split(" ")[0];
            double amount = slip.getAmount();

            // เพิ่มหรือลดยอดตามประเภทรายการ
            if (slip.getType() == 1) { // รายรับ
                runningBalance += amount;
            } else { // รายจ่าย
                runningBalance -= amount;
            }

            // บันทึกยอดคงเหลือของวันนั้น
            dailyBalances.put(date, runningBalance);
        }

        DataExportUtil.exportHistoryData(
                this,
                monthText.getText().toString(),
                yearText.getText().toString(),
                totalIncome,
                totalExpense,
                allTransactions,
                null,
                dailyBalances);
    }

    private void exportDataToImage() {
        double totalIncome = Double.parseDouble(income.getText().toString().replace(" ฿", ""));
        double totalExpense = Double.parseDouble(outcome.getText().toString().replace(" ฿", ""));

        Map<String, Double> dailyBalances = new LinkedHashMap<>();
        if (chart.getLineData() != null && !chart.getLineData().getDataSets().isEmpty()) {
            ILineDataSet dataSet = chart.getLineData().getDataSets().get(0);
            for (int i = 0; i < dataSet.getEntryCount(); i++) {
                Entry entry = dataSet.getEntryForIndex(i);
                String date = String.format("%02d/%02d/%d",
                        (int) entry.getX(),
                        selectedMonth + 1,
                        selectedYear);
                dailyBalances.put(date, (double) entry.getY());
            }
        }

        Bitmap summaryImage = SummaryImageGenerator.generateHistorySummaryImage(
                this,
                monthText.getText().toString(),
                yearText.getText().toString(),
                totalIncome,
                totalExpense,
                chart,
                dailyBalances);

        // บันทึกไฟล์รูปภาพ
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = String.format("history_summary_%s_%s_%s.jpg",
                monthText.getText(), yearText.getText(), timestamp);

        // บันทึกรูปภาพลงใน Gallery
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = getContentResolver();
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            if (imageUri != null) {
                try (OutputStream out = resolver.openOutputStream(imageUri)) {
                    if (out != null) {
                        summaryImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(imageUri, values, null, null);
                }

                Toast.makeText(this,
                        "บันทึกรูปภาพไว้ในแกลเลอรี่แล้ว",
                        Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            // ในกรณีที่เกิดข้อผิดพลาด ให้ลบไฟล์ที่ค้างอยู่
            if (imageUri != null) {
                resolver.delete(imageUri, null, null);
            }
            Toast.makeText(this, "เกิดข้อผิดพลาดในการบันทึกรูปภาพ", Toast.LENGTH_SHORT).show();
            Log.e("CategoryActivity", "Error saving image to gallery: " + e.getMessage());
        }
    }

    // เปลี่ยนการเรียกใช้จากปุ่ม export
    private void setupExportButton() {
        exportButton.setOnClickListener(v -> {
            if (checkPermissionAndExport()) {
                showExportDialog();
            }
        });
    }

    private List<TransferSlip> getAllTransactionsForCurrentMonth() {
        List<TransferSlip> transactions = new ArrayList<>();
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID, TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE };

        String startDay = String.format("01/%02d/%d", selectedMonth + 1, selectedYear);

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear - 543, selectedMonth, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        String endDay = String.format("%02d/%02d/%d", lastDay, selectedMonth + 1, selectedYear);

        String selection = "date BETWEEN ? AND ?";
        String[] selectionArgs = { startDay, endDay };
        String ORDER_BY = DATE + " ASC, " + TIME + " ASC";

        Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, ORDER_BY);

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

                TransferSlip slip = new TransferSlip(
                        id,
                        dateStr + " " + timeStr,
                        money,
                        receiver,
                        description,
                        image,
                        category,
                        type);
                transactions.add(slip);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return transactions;
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.green_500,
                R.color.blue_500,
                R.color.orange_500);
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
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setDrawGridBackground(false);
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
        // Create a combined list of all transactions ordered by date
        List<Entry> combinedEntries = new ArrayList<>();
        float runningTotal = 0f;
        float minValue = 0f; // Track minimum value for y-axis
        float maxValue = 0f; // Track maximum value for y-axis

        // Create a map of daily totals
        Map<Integer, Float> dailyTotals = new HashMap<>();

        // Calculate daily net changes
        for (Entry income : incomeEntries) {
            int day = (int) income.getX();
            dailyTotals.put(day, dailyTotals.getOrDefault(day, 0f) + income.getY());
        }

        for (Entry expense : expenseEntries) {
            int day = (int) expense.getX();
            dailyTotals.put(day, dailyTotals.getOrDefault(day, 0f) - expense.getY());
        }

        // Sort days
        List<Integer> sortedDays = new ArrayList<>(dailyTotals.keySet());
        Collections.sort(sortedDays);

        // Create entries with running total
        for (Integer day : sortedDays) {
            runningTotal += dailyTotals.get(day);
            combinedEntries.add(new Entry(day, runningTotal));
            minValue = Math.min(minValue, runningTotal);
            maxValue = Math.max(maxValue, runningTotal);
        }

        // Set up the chart
        YAxis leftAxis = chart.getAxisLeft();
        float padding = Math.max(Math.abs(maxValue), Math.abs(minValue)) * 0.1f;
        leftAxis.setAxisMaximum(maxValue + padding);
        leftAxis.setAxisMinimum(minValue - padding);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setZeroLineColor(Color.GRAY);
        leftAxis.setZeroLineWidth(1f);

        // Set up X-Axis
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear - 543, selectedMonth, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(1f);
        xAxis.setAxisMaximum(lastDay);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGridLineWidth(0.5f);

        List<ILineDataSet> dataSets = new ArrayList<>();

        // Create DataSets for up and down trends - always create both even if empty
        LineDataSet upDataSet = new LineDataSet(new ArrayList<>(), "เพิ่มขึ้น");
        upDataSet.setColor(Color.GREEN);
        upDataSet.setLineWidth(2f);
        upDataSet.setDrawCircles(false);
        upDataSet.setDrawValues(false);
        upDataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSets.add(upDataSet);

        LineDataSet downDataSet = new LineDataSet(new ArrayList<>(), "ลดลง");
        downDataSet.setColor(Color.RED);
        downDataSet.setLineWidth(2f);
        downDataSet.setDrawCircles(false);
        downDataSet.setDrawValues(false);
        downDataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSets.add(downDataSet);

        // If we have data, update the datasets
        if (!combinedEntries.isEmpty()) {
            List<Entry> upSegments = new ArrayList<>();
            List<Entry> downSegments = new ArrayList<>();

            for (int i = 0; i < combinedEntries.size() - 1; i++) {
                Entry current = combinedEntries.get(i);
                Entry next = combinedEntries.get(i + 1);

                if (next.getY() >= current.getY()) {
                    upSegments.add(current);
                    upSegments.add(next);
                } else {
                    downSegments.add(current);
                    downSegments.add(next);
                }
            }

            upDataSet.setValues(upSegments);
            downDataSet.setValues(downSegments);
        }

        // Create DataSet for points (without legend)
        LineDataSet pointDataSet = new LineDataSet(combinedEntries, "");
        pointDataSet.setDrawCircles(true);
        pointDataSet.setCircleRadius(4f);
        pointDataSet.setDrawValues(true);
        pointDataSet.setValueTextSize(10f);
        pointDataSet.setColor(Color.TRANSPARENT);
        pointDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f฿", value);
            }
        });

        // Set circle colors based on trend with special handling for first point
        int[] colors = new int[combinedEntries.size()];
        for (int i = 0; i < combinedEntries.size(); i++) {
            float currentValue = combinedEntries.get(i).getY();
            if (i == 0) {
                // First point: red if negative, green if positive
                colors[i] = currentValue >= 0 ? Color.GREEN : Color.RED;
            } else {
                float previousValue = combinedEntries.get(i - 1).getY();
                colors[i] = currentValue >= previousValue ? Color.GREEN : Color.RED;
            }
        }
        pointDataSet.setCircleColors(colors);

        // Set up gradient fill
        pointDataSet.setDrawFilled(true);
        pointDataSet.setFillDrawable(new android.graphics.drawable.Drawable() {
            @Override
            public void draw(Canvas canvas) {
                if (combinedEntries.isEmpty())
                    return;

                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);

                float[] points = new float[combinedEntries.size() * 2];
                for (int i = 0; i < combinedEntries.size(); i++) {
                    Entry entry = combinedEntries.get(i);
                    points[i * 2] = entry.getX();
                    points[i * 2 + 1] = entry.getY();
                }

                // Transform points to pixels
                chart.getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(points);

                // Get zero line position in pixels
                float[] zeroLinePoints = new float[] { 0f, 0f };
                chart.getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(zeroLinePoints);
                float zeroY = zeroLinePoints[1];

                // Draw gradient fills for each segment
                for (int i = 2; i < points.length; i += 2) {
                    float previousX = points[i - 2];
                    float previousY = points[i - 1];
                    float currentX = points[i];
                    float currentY = points[i + 1];

                    // Determine if this segment is increasing or decreasing
                    boolean isIncreasing = currentY <= previousY; // Note: Y-axis is inverted in canvas

                    // Create separate paths for above and below zero line
                    Path pathAboveZero = new Path();
                    Path pathBelowZero = new Path();

                    // For the part above zero line
                    if (Math.min(previousY, currentY) < zeroY) {
                        pathAboveZero.moveTo(previousX, Math.max(previousY, zeroY));
                        pathAboveZero.lineTo(previousX, previousY);
                        pathAboveZero.lineTo(currentX, currentY);
                        pathAboveZero.lineTo(currentX, Math.max(currentY, zeroY));
                        pathAboveZero.close();
                    }

                    // For the part below zero line
                    if (Math.max(previousY, currentY) > zeroY) {
                        pathBelowZero.moveTo(previousX, Math.min(previousY, zeroY));
                        pathBelowZero.lineTo(previousX, previousY);
                        pathBelowZero.lineTo(currentX, currentY);
                        pathBelowZero.lineTo(currentX, Math.min(currentY, zeroY));
                        pathBelowZero.close();
                    }

                    // Draw fills with swapped colors
                    if (!pathAboveZero.isEmpty()) {
                        paint.setColor(isIncreasing ? Color.argb(50, 0, 255, 0) : // Red for increase
                                Color.argb(50, 255, 0, 0)); // Green for decrease
                        canvas.drawPath(pathAboveZero, paint);
                    }

                    if (!pathBelowZero.isEmpty()) {
                        paint.setColor(isIncreasing ? Color.argb(50, 0, 255, 0) : // Red for increase
                                Color.argb(50, 255, 0, 0)); // Green for decrease
                        canvas.drawPath(pathBelowZero, paint);
                    }
                }
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        });

        dataSets.add(pointDataSet);

        // Set up Legend
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setDrawInside(false);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setFormSize(10f);
        legend.setTextSize(12f);
        legend.setXEntrySpace(20f);

        // Handle empty state
        if (combinedEntries.isEmpty()) {
            chart.setNoDataText("ไม่มีข้อมูลในเดือนนี้");
            chart.setNoDataTextColor(Color.GRAY);
        }

        // Display the chart
        LineData lineData = new LineData(dataSets);
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

    private void setupAddButton() {
        addbtn = findViewById(R.id.add_btn);
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_button);

        addbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSlipActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, R.anim.hold);
        });

        addbtn.setOnTouchListener((v, event) -> {
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

    private String formatNumber(float number) {
        DecimalFormat df = new DecimalFormat("###,###,###,###.##");
        return df.format(number);
    }
}
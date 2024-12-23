package com.demo.JotSlip;

import static android.provider.BaseColumns._ID;
import static com.demo.JotSlip.Constants.CATEGORY;
import static com.demo.JotSlip.Constants.DATE;
import static com.demo.JotSlip.Constants.DESCRIPTION;
import static com.demo.JotSlip.Constants.IMAGE;
import static com.demo.JotSlip.Constants.MONEY;
import static com.demo.JotSlip.Constants.RECEIVER;
import static com.demo.JotSlip.Constants.TABLE_NAME;
import static com.demo.JotSlip.Constants.TIME;
import static com.demo.JotSlip.Constants.TYPE;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;
    private EventsData events;
    private PieChart chart;
    private TextView monthText;
    private TextView yearText;
    private int selectedMonth;
    private int selectedYear;
    private ImageButton addbtn;
    private ImageButton exportButton;
    private RecyclerView categoryRecyclerView;
    private View historyNav;
    private ImageView historyIcon;
    private TextView historyText;
    private View homeNav;
    private ImageView homeIcon;
    private TextView homeText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View navCategory;
    private ImageView categoryIcon;
    private TextView categoryText;
    private View navSettings;
    private ImageView settingsIcon;
    private TextView settingsText;
    private String[] months;
    private double totalExpense = 0.0;
    private Map<String, Double> categoryExpenses = new HashMap<>();
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        settingsManager = new SettingsManager(this);
        settingsManager.applyLanguage();
        setContentView(R.layout.category);

        months = getResources().getStringArray(R.array.months_array);
        initializeViews();
        setupSwipeRefresh();
        setupNavigation();
        setupChart();
        setupExportButton();
        setupAddButton();

        updateChartData();
        updateCategoryData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCategoryState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoryState();
        updateChartData();
        updateCategoryData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedMonth", selectedMonth);
        outState.putInt("selectedYear", selectedYear);
        outState.putFloat("scrollPosition",
                categoryRecyclerView.getLayoutManager() != null
                        ? ((LinearLayoutManager) categoryRecyclerView.getLayoutManager())
                                .findFirstVisibleItemPosition()
                        : 0);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            selectedMonth = savedInstanceState.getInt("selectedMonth");
            selectedYear = savedInstanceState.getInt("selectedYear");
            final float scrollPosition = savedInstanceState.getFloat("scrollPosition");

            categoryRecyclerView.post(() -> {
                if (categoryRecyclerView.getLayoutManager() != null) {
                    ((LinearLayoutManager) categoryRecyclerView.getLayoutManager())
                            .scrollToPosition((int) scrollPosition);
                }
            });
        }
    }

    private void saveCategoryState() {
        SharedPreferences prefs = getSharedPreferences("CategoryState", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("selectedMonth", selectedMonth);
        editor.putInt("selectedYear", selectedYear);
        if (categoryRecyclerView.getLayoutManager() != null) {
            editor.putInt("scrollPosition",
                    ((LinearLayoutManager) categoryRecyclerView.getLayoutManager())
                            .findFirstVisibleItemPosition());
        }
        editor.apply();
    }

    private void loadCategoryState() {
        SharedPreferences prefs = getSharedPreferences("CategoryState", MODE_PRIVATE);
        selectedMonth = prefs.getInt("selectedMonth", selectedMonth);
        selectedYear = prefs.getInt("selectedYear", selectedYear);
        final int scrollPosition = prefs.getInt("scrollPosition", 0);

        categoryRecyclerView.post(() -> {
            if (categoryRecyclerView.getLayoutManager() != null) {
                ((LinearLayoutManager) categoryRecyclerView.getLayoutManager())
                        .scrollToPosition(scrollPosition);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (events != null) {
            events.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showExportDialog();
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeViews() {
        chart = findViewById(R.id.chart);
        monthText = findViewById(R.id.monthText);
        yearText = findViewById(R.id.yearText);

        categoryRecyclerView = findViewById(R.id.category_list_item_view);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryRecyclerView.setNestedScrollingEnabled(true);

        exportButton = findViewById(R.id.exportButton);
        events = new EventsData(this);

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

        setupNavigationViews();
    }

    private void setupNavigationViews() {
        historyNav = findViewById(R.id.nav_history);
        historyIcon = historyNav.findViewById(android.R.id.icon);
        historyText = historyNav.findViewById(android.R.id.text1);
        setupHistoryNavigation();

        homeNav = findViewById(R.id.nav_home);
        homeIcon = homeNav.findViewById(R.id.home_icon);
        homeText = homeNav.findViewById(R.id.home_text);
        setupHomeNavigation();

        homeIcon.setColorFilter(getColor(R.color.gray));
        homeText.setTextColor(getColor(R.color.gray));
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
    }

    private void setupHistoryNavigation() {
        historyNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, options);
        });
        historyIcon.setColorFilter(getColor(R.color.gray));
        historyText.setTextColor(getColor(R.color.gray));
    }

    private void setupHomeNavigation() {
        homeNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            startActivity(intent, options);
        });
    }

    private void setupNavigation() {
        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedYear = calendar.get(Calendar.YEAR) + 543;

        findNearestData(selectedMonth, selectedYear);
        updateDisplayTexts();

        setupNavigationButtons();
    }

    private void setupNavigationButtons() {
        findViewById(R.id.prevYear).setOnClickListener(v -> {
            int newYear = findPreviousYearWithData(selectedYear);
            if (newYear != selectedYear) {
                selectedYear = newYear;
                selectedMonth = findLatestMonthWithData(selectedYear);
                updateDisplayTexts();
                updateChartData();
                updateCategoryData();
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
        });
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

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(61f);
        chart.setDrawCenterText(true);
        chart.setCenterText(getString(R.string.chart_center_text));
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
    }

    private void showExportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.export_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialogView.findViewById(R.id.btnExportCsv).setOnClickListener(v -> {
            if (checkPermissionAndExport()) {
                exportDataToCsv();
                dialog.dismiss();
            }
        });

        dialogView.findViewById(R.id.btnExportImage).setOnClickListener(v -> {
            if (checkPermissionAndExport()) {
                exportDataToImage();
                dialog.dismiss();
            }
        });
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

    private void setupExportButton() {
        exportButton.setOnClickListener(v -> {
            if (checkPermissionAndExport()) {
                showExportDialog();
            }
        });
    }

    private void exportDataToCsv() {
        updateExpenseData();
        DataExportUtil.exportCategoryData(
                this,
                monthText.getText().toString(),
                yearText.getText().toString(),
                totalExpense,
                categoryExpenses,
                null);
    }

    private void exportDataToImage() {
        // Save the original chart size
        final ViewGroup.LayoutParams originalParams = chart.getLayoutParams();
        final int originalWidth = chart.getWidth();
        final int originalHeight = chart.getHeight();

        Map<String, Double> categoryExpenses = getCategoryExpenses();
        double totalExpense = calculateTotalExpense();

        try {
            // Calculate the desired chart size
            int desiredWidth = chart.getWidth();
            int desiredHeight = 300; // or the desired height

            // Set the new chart size
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    desiredWidth,
                    desiredHeight);
            chart.setLayoutParams(params);

            Bitmap summaryImage = SummaryImageGenerator.generateCategorySummaryImage(
                    this,
                    monthText.getText().toString(),
                    yearText.getText().toString(),
                    totalExpense,
                    chart,
                    categoryExpenses);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = String.format("category_summary_%s_%s_%s.jpg",
                    monthText.getText(), yearText.getText(), timestamp);

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

                    Toast.makeText(this, getString(R.string.save_image_success), Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                if (imageUri != null) {
                    resolver.delete(imageUri, null, null);
                }
                Log.e("CategoryActivity", getString(R.string.error_saving_image_gallery, e.getMessage()));
                Toast.makeText(this, getString(R.string.save_image_error), Toast.LENGTH_SHORT).show();
            }
        } finally {
            // Restore the original chart size
            chart.setLayoutParams(originalParams);
            chart.getLayoutParams().width = originalWidth;
            chart.getLayoutParams().height = originalHeight;

            // Force a redraw
            chart.requestLayout();
            chart.invalidate();

            // Call updateChartData to update the data and display
            updateChartData();
        }
    }

    private Map<String, Double> getCategoryExpenses() {
        Map<String, Double> categoryExpenses = new HashMap<>();
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { TYPE, MONEY, DATE, CATEGORY };

        // Use direct month and year comparison from substring
        String selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ? AND type != 1";
        String[] selectionArgs = {
                String.format("%02d", selectedMonth + 1),
                String.valueOf(selectedYear)
        };

        Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                double money = cursor.getDouble(cursor.getColumnIndex(MONEY));
                String category = cursor.getString(cursor.getColumnIndex(CATEGORY));

                categoryExpenses.merge(category, money, Double::sum);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return categoryExpenses;
    }

    private double calculateTotalExpense() {
        double total = 0.0;
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { MONEY };

        // Use direct month and year comparison from substring
        String selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ? AND type != 1";
        String[] selectionArgs = {
                String.format("%02d", selectedMonth + 1),
                String.valueOf(selectedYear)
        };

        Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                total += cursor.getDouble(cursor.getColumnIndex(MONEY));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return total;
    }

    private void updateChartData() {
        Cursor cursor = getEvents();
        Map<String, Float> categoryExpenses = new HashMap<>();
        float totalExpense = 0f;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                float money = cursor.getFloat(cursor.getColumnIndex(MONEY));
                String categoryKey = cursor.getString(cursor.getColumnIndex(CATEGORY));
                String dateStr = cursor.getString(cursor.getColumnIndex(DATE));

                try {
                    String[] dateParts = dateStr.split("/");
                    int day = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int year = Integer.parseInt(dateParts[2]);

                    if (month == selectedMonth + 1 && year == selectedYear) {
                        if (type != 1) { // Not income
                            // Use category key as map key
                            float currentValue = categoryExpenses.getOrDefault(categoryKey, 0f);
                            categoryExpenses.put(categoryKey, currentValue + money);
                            totalExpense += money;
                        }
                    }
                } catch (Exception e) {
                    Log.e("CategoryActivity", getString(R.string.error_parsing_date, dateStr), e);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Update chart with category data
        if (totalExpense > 0) {
            ArrayList<PieEntry> entries = new ArrayList<>();
            ArrayList<Integer> colors = new ArrayList<>();

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
                    // Get localized category name for display
                    String localizedCategory = getString(CategoryConstants.getDisplayStringResource(entry.getKey()));
                    entries.add(new PieEntry(entry.getValue(), localizedCategory));
                    colors.add(CHART_COLORS[colorIndex % CHART_COLORS.length]);
                    colorIndex++;
                }
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            PieData data = new PieData(dataSet);
            float finalTotalExpense = totalExpense;
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%.1f%%", (value / finalTotalExpense) * 100);
                }
            });
            data.setValueTextSize(11f);
            data.setValueTextColor(Color.WHITE);

            chart.setCenterText(String.format(getString(R.string.total_expense_format), totalExpense));
            chart.setCenterTextSize(14f);
            chart.setData(data);
            chart.invalidate();
        } else {
            // Show empty chart
            chart.setData(null);
            chart.setCenterText(getString(R.string.no_data_this_month));
            chart.invalidate();
        }
    }

    private void updateChartWithData(Map<String, Float> categoryExpenses, float totalExpense) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

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

        chart.setCenterText(String.format(getString(R.string.total_expense_format), totalExpense));
        chart.setCenterTextSize(14f);
        chart.setData(data);
        chart.invalidate();
    }

    private void updateCategoryData() {
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
                    Log.e("CategoryActivity", getString(R.string.error_parsing_date, dateStr), e);
                }
            } while (cursor.moveToNext());

            CategoryAdapter adapter = new CategoryAdapter(slipList, false);
            categoryRecyclerView.setAdapter(adapter);

            cursor.close();
        }
    }

    private void updateExpenseData() {
        Cursor cursor = getEvents();
        totalExpense = 0.0;
        categoryExpenses.clear();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                if (type != 1) {
                    double amount = cursor.getDouble(cursor.getColumnIndex(MONEY));
                    String category_raw = cursor.getString(cursor.getColumnIndex(CATEGORY));
                    String category = getString(CategoryConstants.getDisplayStringResource(category_raw));

                    totalExpense += amount;
                    categoryExpenses.merge(category, amount, Double::sum);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void updateDisplayTexts() {
        monthText.setText(months[selectedMonth]);
        yearText.setText(String.valueOf(selectedYear));
    }

    private void findNearestData(int currentMonth, int currentYear) {
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID, DATE };

        String monthStr = String.format("%02d", currentMonth + 1);
        String selection = "substr(date, 4, 2) = ? AND substr(date, -4) = ?";
        String[] selectionArgs = { monthStr, String.valueOf(currentYear) };
        Cursor cursor = db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            selectedMonth = currentMonth;
            selectedYear = currentYear;
            cursor.close();
            return;
        }

        // Find closest month within 6 months range
        int closestMonth = currentMonth;
        int closestYear = currentYear;
        int minDistance = Integer.MAX_VALUE;

        for (int monthOffset = -6; monthOffset <= 6; monthOffset++) {
            int targetMonth = currentMonth;
            int targetYear = currentYear;

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
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID };

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
        SQLiteDatabase db = events.getReadableDatabase();
        String[] FROM = { _ID };

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

    private Cursor getEvents() {
        String[] FROM = { _ID, TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE };
        String ORDER_BY = DATE + " ASC";
        SQLiteDatabase db = events.getReadableDatabase();

        String startDay = String.format("01/%02d/%d", selectedMonth + 1, selectedYear);
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear - 543, selectedMonth, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        String endDay = String.format("%02d/%02d/%d", lastDay, selectedMonth + 1, selectedYear);

        String selection = "date BETWEEN ? AND ?";
        String[] selectionArgs = { startDay, endDay };

        return db.query(TABLE_NAME, FROM, selection, selectionArgs, null, null, ORDER_BY);
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

    private void showSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.settings_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set up button functionality
        View languageCard = dialogView.findViewById(R.id.languageCard);
        View resetDataCard = dialogView.findViewById(R.id.resetDataCard);

        // Handle language change
        languageCard.setOnClickListener(v -> {
            showLanguageDialog();
            dialog.dismiss();
        });

        // Handle data reset
        resetDataCard.setOnClickListener(v -> {
            dialog.dismiss();
            showResetConfirmationDialog();
        });

        dialog.show();
    }

    private void showLanguageDialog() {
        String[] languages = { getString(R.string.thai), getString(R.string.english) };
        String currentLang = settingsManager.getCurrentLanguage();
        int checkedItem = currentLang.equals("th") ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.language))
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selectedLang = (which == 0) ? "th" : "en";
                    if (!selectedLang.equals(currentLang)) {
                        settingsManager.setLanguage(selectedLang);
                        restartApp();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel_text), null)
                .show();
    }

    private void restartApp() {
        Intent intent = new Intent(this, CategoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showResetConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Create a custom AlertDialog
        View dialogView = getLayoutInflater().inflate(R.layout.reset_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Make the dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();

        // Set up button functionality in the dialog
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
            // Delete data from the database
            events = new EventsData(this);
            SQLiteDatabase db = events.getWritableDatabase();
            db.delete(TABLE_NAME, null, null);

            // Reset initial values
            Calendar calendar = Calendar.getInstance();
            selectedMonth = calendar.get(Calendar.MONTH);
            selectedYear = calendar.get(Calendar.YEAR) + 543;

            // Reset data variables
            totalExpense = 0.0;
            categoryExpenses.clear();

            // Update all displays
            updateDisplayTexts();
            updateChartData();
            updateCategoryData();
            updateExpenseData();

            // Reset the PieChart
            chart.clear();
            chart.setData(null);
            chart.setCenterText(getString(R.string.chart_center_text));
            chart.invalidate();

            // Reset the RecyclerView
            categoryRecyclerView.setAdapter(new CategoryAdapter(new ArrayList<>(), false));

            Toast.makeText(this, getString(R.string.reset_data_success), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.reset_data_error), Toast.LENGTH_SHORT).show();
            Log.e("CategoryActivity", "Error resetting data: " + e.getMessage());
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateChartData();
        updateCategoryData();
        updateDisplayTexts();
    }
}
package com.example.finalproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataExportUtil {

    public static void exportHistoryData(Context context, String month, String year,
                                       double totalIncome, double totalExpense,
                                       List<TransferSlip> transactions,
                                       LineChart lineChart,
                                       Map<String, Double> dailyBalances) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String baseFileName = String.format("history_export_%s_%s_%s", month, year, timestamp);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            // Export CSV data
            File csvFile = new File(downloadsDir, baseFileName + ".csv");
            FileWriter writer = new FileWriter(csvFile);

            // Write header and summary
            writer.append(context.getString(R.string.export_monthly_report, month, year)).append("\n\n");
            writer.append(context.getString(R.string.export_summary)).append("\n");
            writer.append(context.getString(R.string.export_total_income)).append(",")
                    .append(String.format("%.2f", totalIncome)).append("\n");
            writer.append(context.getString(R.string.export_total_expense)).append(",")
                    .append(String.format("%.2f", totalExpense)).append("\n");
            writer.append(context.getString(R.string.export_balance)).append(",")
                    .append(String.format("%.2f", totalIncome - totalExpense)).append("\n\n");

            // Write daily balance data
            writer.append(context.getString(R.string.export_daily_balance_title)).append("\n");
            writer.append(context.getString(R.string.export_daily_balance_header)).append("\n");
            for (Map.Entry<String, Double> entry : dailyBalances.entrySet()) {
                writer.append(entry.getKey()).append(",")
                        .append(String.format("%.2f", entry.getValue())).append("\n");
            }
            writer.append("\n");

            // Write transactions header
            writer.append(context.getString(R.string.export_transactions_title)).append("\n");
            writer.append(context.getString(R.string.export_transactions_header)).append("\n");

            // Write transaction data
            for (TransferSlip slip : transactions) {
                String[] dateTime = slip.getDateTime().split(" ");
                writer.append(dateTime[0]).append(","); // Date
                writer.append(dateTime[1]).append(","); // Time
                writer.append(slip.getType() == 1 ? 
                        context.getString(R.string.export_income_type) : 
                        context.getString(R.string.export_expense_type)).append(",");
                writer.append(slip.getCategory()).append(",");
                writer.append(String.format("%.2f", slip.getAmount())).append(",");
                writer.append(slip.getDescription().replace(",", ";")).append(",");
                writer.append(slip.getReceiver().replace(",", ";")).append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(context,
                    context.getString(R.string.export_success_message, downloadsDir.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("DataExportUtil", context.getString(R.string.export_error_log, "history", e.getMessage()));
            Toast.makeText(context, context.getString(R.string.export_error_message), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportCategoryData(Context context, String month, String year,
                                        double totalExpense, Map<String, Double> categoryExpenses,
                                        PieChart pieChart) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String baseFileName = String.format("category_export_%s_%s_%s", month, year, timestamp);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            // Export CSV data
            File csvFile = new File(downloadsDir, baseFileName + ".csv");
            FileWriter writer = new FileWriter(csvFile);

            // Write header
            writer.append(context.getString(R.string.export_category_report, month, year)).append("\n\n");
            writer.append(context.getString(R.string.export_total_expense_summary, totalExpense)).append("\n\n");

            // Write category breakdown
            writer.append(context.getString(R.string.export_category_header)).append("\n");
            for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                double percentage = (entry.getValue() / totalExpense) * 100;
                writer.append(entry.getKey()).append(",");
                writer.append(String.format("%.2f", entry.getValue())).append(",");
                writer.append(String.format(context.getString(R.string.export_percentage_format), percentage)).append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(context,
                    context.getString(R.string.export_success_message, downloadsDir.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("DataExportUtil", context.getString(R.string.export_error_log, "category", e.getMessage()));
            Toast.makeText(context, context.getString(R.string.export_error_message), Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveChartAsImage(com.github.mikephil.charting.charts.Chart chart, File file) throws IOException {
        // Store original dimensions
        ViewGroup.LayoutParams originalParams = chart.getLayoutParams();
        int originalWidth = chart.getWidth();
        int originalHeight = chart.getHeight();

        try {
            // Create bitmap and save
            Bitmap bitmap = chart.getChartBitmap();
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } finally {
            // Restore original dimensions
            if (originalParams != null) {
                originalParams.width = originalWidth;
                originalParams.height = originalHeight;
                chart.setLayoutParams(originalParams);
                chart.requestLayout();
            }
        }
    }
}
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
            writer.append("รายงานประจำเดือน: " + month + " " + year + "\n\n");
            writer.append("สรุปรวม:\n");
            writer.append("รายรับรวม,").append(String.format("%.2f", totalIncome)).append("\n");
            writer.append("รายจ่ายรวม,").append(String.format("%.2f", totalExpense)).append("\n");
            writer.append("คงเหลือ,").append(String.format("%.2f", totalIncome - totalExpense)).append("\n\n");

            // Write daily balance data
            writer.append("ข้อมูลกราฟยอดคงเหลือรายวัน:\n");
            writer.append("วันที่,ยอดคงเหลือ\n");
            for (Map.Entry<String, Double> entry : dailyBalances.entrySet()) {
                writer.append(entry.getKey()).append(",")
                        .append(String.format("%.2f", entry.getValue())).append("\n");
            }
            writer.append("\n");

            // Write transactions header
            writer.append("รายการธุรกรรมทั้งหมด:\n");
            writer.append("วันที่,เวลา,ประเภท,หมวดหมู่,จำนวนเงิน,รายละเอียด,ผู้รับ/จ่าย\n");

            // Write transaction data
            for (TransferSlip slip : transactions) {
                String[] dateTime = slip.getDateTime().split(" ");
                writer.append(dateTime[0]).append(","); // วันที่
                writer.append(dateTime[1]).append(","); // เวลา
                writer.append(slip.getType() == 1 ? "รายรับ" : "รายจ่าย").append(",");
                writer.append(slip.getCategory()).append(",");
                writer.append(String.format("%.2f", slip.getAmount())).append(",");
                writer.append(slip.getDescription().replace(",", ";")).append(",");
                writer.append(slip.getReceiver().replace(",", ";")).append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(context,
                    "ส่งออกข้อมูลสำเร็จ\nบันทึกไว้ที่: " + downloadsDir.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("DataExportUtil", "Error exporting history data: " + e.getMessage());
            Toast.makeText(context, "เกิดข้อผิดพลาดในการส่งออกข้อมูล", Toast.LENGTH_SHORT).show();
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
            writer.append("รายงานสรุปตามหมวดหมู่ประจำเดือน: " + month + " " + year + "\n\n");
            writer.append("รายจ่ายรวมทั้งหมด: " + String.format("%.2f", totalExpense) + "\n\n");

            // Write category breakdown
            writer.append("หมวดหมู่,จำนวนเงิน,เปอร์เซ็นต์\n");
            for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                double percentage = (entry.getValue() / totalExpense) * 100;
                writer.append(entry.getKey()).append(",");
                writer.append(String.format("%.2f", entry.getValue())).append(",");
                writer.append(String.format("%.1f%%", percentage)).append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(context,
                    "ส่งออกข้อมูลสำเร็จ\nบันทึกไว้ที่: " + downloadsDir.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("DataExportUtil", "Error exporting category data: " + e.getMessage());
            Toast.makeText(context, "เกิดข้อผิดพลาดในการส่งออกข้อมูล", Toast.LENGTH_SHORT).show();
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
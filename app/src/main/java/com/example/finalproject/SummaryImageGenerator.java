package com.example.finalproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class SummaryImageGenerator {
    private static final int IMAGE_WIDTH = 1080; // Instagram Story width
    private static final int IMAGE_HEIGHT = 1920; // Instagram Story height
    private static final int PADDING = 40;
    private static final int CARD_RADIUS = 30;
    private static final DecimalFormat formatter = new DecimalFormat("#,###,###.##");

    public static Bitmap generateHistorySummaryImage(
            Context context,
            String month,
            String year,
            double totalIncome,
            double totalExpense,
            LineChart chart,
            Map<String, Double> dailyBalances) {

        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw gradient background
        Paint backgroundPaint = new Paint();
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, IMAGE_HEIGHT,
                new int[]{
                        Color.rgb(248, 249, 250),  // Light gray at top
                        Color.rgb(240, 242, 245)   // Slightly darker at bottom
                },
                null,
                Shader.TileMode.CLAMP
        );
        backgroundPaint.setShader(gradient);
        canvas.drawRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, backgroundPaint);

        float currentY = PADDING * 2;

        // Draw app name with style
        Paint appNamePaint = new Paint();
        appNamePaint.setColor(Color.rgb(76, 175, 80)); // Green color
        appNamePaint.setTextSize(60f);
        appNamePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("JotSlip", PADDING, currentY, appNamePaint);
        currentY += 80;

        // Draw period with modern style
        Paint periodPaint = new Paint();
        periodPaint.setColor(Color.rgb(33, 37, 41));
        periodPaint.setTextSize(72f);
        periodPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String periodText = "สรุปรายรับ-รายจ่าย";
        canvas.drawText(periodText, PADDING, currentY, periodPaint);
        currentY += 60;

        Paint subPeriodPaint = new Paint();
        subPeriodPaint.setColor(Color.rgb(108, 117, 125));
        subPeriodPaint.setTextSize(48f);
        String dateText = "ประจำเดือน " + month + " " + year;
        canvas.drawText(dateText, PADDING, currentY, subPeriodPaint);
        currentY += 80;

        // Draw summary cards
        drawSummaryCard(canvas, "รายรับ", totalIncome, Color.rgb(40, 167, 69), currentY);
        currentY += 180;
        drawSummaryCard(canvas, "รายจ่าย", totalExpense, Color.rgb(220, 53, 69), currentY);
        currentY += 180;
        drawSummaryCard(canvas, "คงเหลือ", totalIncome - totalExpense,
                (totalIncome - totalExpense) >= 0 ? Color.rgb(40, 167, 69) : Color.rgb(220, 53, 69),
                currentY);
        currentY += 180;

        // Draw chart title
        Paint chartTitlePaint = new Paint();
        chartTitlePaint.setColor(Color.rgb(33, 37, 41));
        chartTitlePaint.setTextSize(48f);
        chartTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        currentY += 60; // เพิ่มระยะห่างก่อนหัวข้อกราฟ
        canvas.drawText("กราฟแสดงการเปลี่ยนแปลง", PADDING, currentY, chartTitlePaint);
        currentY += 60;

        // Draw chart
        if (chart != null) {
            int chartWidth = IMAGE_WIDTH - (PADDING * 3); // เพิ่มระยะขอบ
            int chartHeight = 480; // ลดความสูงลง
            chart.measure(
                    View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
            );
            chart.layout(0, 0, chartWidth, chartHeight);

            canvas.save();
            canvas.translate(PADDING + 10, currentY + 10); // เพิ่มระยะขอบซ้าย
            chart.draw(canvas);
            canvas.restore();
        }
        currentY += 540;

        // Draw bottom info section
        Paint infoPaint = new Paint();
        infoPaint.setColor(Color.rgb(108, 117, 125));
        infoPaint.setTextSize(32f);

        // Draw creation date on left
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        canvas.drawText("Created: " + dateStr,
                PADDING, IMAGE_HEIGHT - PADDING - 10, infoPaint);

        // Draw app signature on right
        String signature = "Generated by JotSlip";
        float signatureWidth = infoPaint.measureText(signature);
        canvas.drawText(signature,
                IMAGE_WIDTH - PADDING - signatureWidth, IMAGE_HEIGHT - PADDING - 10, infoPaint);

        return bitmap;
    }

    private static void drawSummaryCard(Canvas canvas, String title, double amount, int color, float y) {
        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setShadowLayer(10.0f, 0.0f, 2.0f, Color.argb(50, 0, 0, 0));

        // Draw card background
        RectF cardRect = new RectF(PADDING, y, IMAGE_WIDTH - PADDING, y + 150);
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardPaint);

        // Draw accent line on left side
        Paint accentPaint = new Paint();
        accentPaint.setColor(color);
        canvas.drawRoundRect(
                new RectF(PADDING, y, PADDING + 10, y + 150),
                5, 5, accentPaint);

        // Draw title
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(73, 80, 87));
        titlePaint.setTextSize(44f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(title, PADDING + 40, y + 50, titlePaint);

        // Draw amount
        Paint amountPaint = new Paint();
        amountPaint.setColor(color);
        amountPaint.setTextSize(54f);
        amountPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String amountText = formatter.format(amount) + " ฿";
        canvas.drawText(amountText, PADDING + 40, y + 110, amountPaint);
    }

    public static Bitmap generateCategorySummaryImage(
            Context context,
            String month,
            String year,
            double totalExpense,
            PieChart chart,
            Map<String, Double> categoryExpenses) {

        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background gradient
        Paint backgroundPaint = new Paint();
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, IMAGE_HEIGHT,
                new int[]{
                        Color.rgb(248, 249, 250),
                        Color.rgb(240, 242, 245)
                },
                null,
                Shader.TileMode.CLAMP
        );
        backgroundPaint.setShader(gradient);
        canvas.drawRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, backgroundPaint);

        float currentY = PADDING * 2;

        // App name
        Paint appNamePaint = new Paint();
        appNamePaint.setColor(Color.rgb(76, 175, 80));
        appNamePaint.setTextSize(60f);
        appNamePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("JotSlip", PADDING, currentY, appNamePaint);
        currentY += 80;

        // Title
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(33, 37, 41));
        titlePaint.setTextSize(72f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("สรุปค่าใช้จ่ายตามหมวดหมู่", PADDING, currentY, titlePaint);
        currentY += 60;

        // Period
        Paint periodPaint = new Paint();
        periodPaint.setColor(Color.rgb(108, 117, 125));
        periodPaint.setTextSize(48f);
        canvas.drawText("ประจำเดือน " + month + " " + year, PADDING, currentY, periodPaint);
        currentY += 80;

        // Total expense card
        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setShadowLayer(10.0f, 0.0f, 2.0f, Color.argb(50, 0, 0, 0));
        RectF totalCard = new RectF(PADDING, currentY, IMAGE_WIDTH - PADDING, currentY + 150);
        canvas.drawRoundRect(totalCard, CARD_RADIUS, CARD_RADIUS, cardPaint);

        Paint totalTitlePaint = new Paint();
        totalTitlePaint.setColor(Color.rgb(73, 80, 87));
        totalTitlePaint.setTextSize(44f);
        canvas.drawText("ค่าใช้จ่ายรวมทั้งหมด", PADDING + 40, currentY + 60, totalTitlePaint);

        Paint totalAmountPaint = new Paint();
        totalAmountPaint.setColor(Color.rgb(220, 53, 69));
        totalAmountPaint.setTextSize(54f);
        totalAmountPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String totalText = formatter.format(totalExpense) + " ฿";
        canvas.drawText(totalText, PADDING + 40, currentY + 120, totalAmountPaint);
        currentY += 180;

        // Draw chart title
        Paint chartTitlePaint = new Paint();
        chartTitlePaint.setColor(Color.rgb(33, 37, 41));
        chartTitlePaint.setTextSize(48f);
        chartTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        currentY += 40;
        canvas.drawText("สัดส่วนค่าใช้จ่ายแต่ละหมวดหมู่", PADDING, currentY, chartTitlePaint);
        currentY += 60;

        // Draw chart
        if (chart != null) {
            int chartWidth = IMAGE_WIDTH - (PADDING * 3);
            int chartHeight = 600;
            chart.measure(
                    View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
            );
            chart.layout(0, 0, chartWidth, chartHeight);

            Paint chartCardPaint = new Paint();
            chartCardPaint.setColor(Color.WHITE);
            chartCardPaint.setShadowLayer(10.0f, 0.0f, 2.0f, Color.argb(50, 0, 0, 0));
            RectF chartCard = new RectF(PADDING, currentY, IMAGE_WIDTH - PADDING, currentY + chartHeight + 20);
            canvas.drawRoundRect(chartCard, CARD_RADIUS, CARD_RADIUS, chartCardPaint);

            canvas.save();
            canvas.translate(PADDING + 10, currentY + 10);
            chart.draw(canvas);
            canvas.restore();
            currentY += chartHeight + 60;
        }

        // Draw category breakdown
        if (categoryExpenses != null && !categoryExpenses.isEmpty()) {
            for (Map.Entry<String, Double> category : categoryExpenses.entrySet()) {
                double percentage = (category.getValue() / totalExpense) * 100;
                drawCategoryCard(canvas, category.getKey(), category.getValue(), percentage, currentY);
                currentY += 100;
            }
        }

        // Bottom info
        Paint infoPaint = new Paint();
        infoPaint.setColor(Color.rgb(108, 117, 125));
        infoPaint.setTextSize(32f);

        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Created: " + dateStr, PADDING, IMAGE_HEIGHT - PADDING - 10, infoPaint);

        String signature = "Generated by JotSlip";
        float signatureWidth = infoPaint.measureText(signature);
        canvas.drawText(signature, IMAGE_WIDTH - PADDING - signatureWidth, IMAGE_HEIGHT - PADDING - 10, infoPaint);

        return bitmap;
    }

    private static void drawCategoryCard(Canvas canvas, String category, double amount, double percentage, float y) {
        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setShadowLayer(8.0f, 0.0f, 2.0f, Color.argb(40, 0, 0, 0));

        RectF cardRect = new RectF(PADDING, y, IMAGE_WIDTH - PADDING, y + 80);
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardPaint);

        Paint categoryPaint = new Paint();
        categoryPaint.setColor(Color.rgb(73, 80, 87));
        categoryPaint.setTextSize(40f);
        categoryPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(category, PADDING + 30, y + 50, categoryPaint);

        Paint amountPaint = new Paint();
        amountPaint.setColor(Color.rgb(220, 53, 69));
        amountPaint.setTextSize(40f);
        amountPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String amountText = formatter.format(amount) + " ฿ (" + String.format("%.1f", percentage) + "%)";
        float amountWidth = amountPaint.measureText(amountText);
        canvas.drawText(amountText, IMAGE_WIDTH - PADDING - amountWidth - 30, y + 50, amountPaint);
    }
}
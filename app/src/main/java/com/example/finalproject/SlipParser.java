package com.example.finalproject;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlipParser {
    public static TransferSlip parseSlip(String text) {
        String dateTime = "";
        double amount = 0.0;
        String sender = "";
        String receiver = "";

        Log.d("SlipParser", "Raw text: " + text);
        String[] lines = text.split("\n");

        // Pattern สำหรับวันที่
        Pattern datePattern = Pattern.compile("(\\d{1,2}\\s*[A-Za-z.]+\\s*\\d{2,4}\\s*\\d{1,2}:\\d{2})");

        // Pattern สำหรับจำนวนเงิน - รองรับเครื่องหมายคั่นพัน
        Pattern amountPattern = Pattern.compile("\\b(\\d{1,3}(?:,\\d{3})*\\.\\d{2})\\b");

        // Pattern สำหรับหมายเลขอ้างอิงและชื่อ
        Pattern refPattern = Pattern.compile("(?:lauñsnunns|เลขที่รายการ):\\s*([\\w]+)");
        Pattern namePattern = Pattern.compile("(?:sHaşsnssUņotõu|ชื่อผู้รับเงิน):\\s*([\\w]+)");

        // เก็บจำนวนเงินทั้งหมดที่เจอไว้ก่อน
        List<Double> foundAmounts = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            Log.d("SlipParser", "Processing line: " + line);

            // หาวันที่และเวลา
            if (dateTime.isEmpty()) {
                Matcher dateMatcher = datePattern.matcher(line);
                if (dateMatcher.find()) {
                    dateTime = dateMatcher.group(1).trim();
                }
            }

            // หาจำนวนเงิน
            Matcher amountMatcher = amountPattern.matcher(line);
            while (amountMatcher.find()) {
                String amountStr = amountMatcher.group(1).trim();
                try {
                    // ลบเครื่องหมายคั่นพันออกก่อนแปลงเป็นตัวเลข
                    double parsedAmount = Double.parseDouble(amountStr.replace(",", ""));
                    Log.d("SlipParser", "Found amount: " + parsedAmount);
                    foundAmounts.add(parsedAmount);
                } catch (NumberFormatException e) {
                    Log.e("SlipParser", "Error parsing amount: " + amountStr, e);
                }
            }

            // หาเลขอ้างอิง (sender)
            Matcher refMatcher = refPattern.matcher(line);
            if (refMatcher.find()) {
                sender = refMatcher.group(1).trim();
            }

            // หาชื่อผู้รับ
            Matcher nameMatcher = namePattern.matcher(line);
            if (nameMatcher.find()) {
                receiver = nameMatcher.group(1).trim();
            }
        }

        // เลือกจำนวนเงินที่มากที่สุด
        if (!foundAmounts.isEmpty()) {
            amount = foundAmounts.stream()
                    .mapToDouble(d -> d)
                    .max()
                    .orElse(0.0);
            Log.d("SlipParser", "Selected maximum amount: " + amount);
        }

        Log.d("SlipParser", String.format("Parsed: Date=%s, Amount=%.2f, Sender=%s, Receiver=%s",
                dateTime, amount, sender, receiver));

        return new TransferSlip(dateTime, amount, sender, receiver);
    }
}
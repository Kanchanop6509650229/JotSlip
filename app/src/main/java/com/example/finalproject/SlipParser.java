package com.example.finalproject;

import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.time.Year;

public class SlipParser {
    private static final Map<String, String> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("Jan", "01");
        MONTH_MAP.put("Feb", "02");
        MONTH_MAP.put("Mar", "03");
        MONTH_MAP.put("Apr", "04");
        MONTH_MAP.put("May", "05");
        MONTH_MAP.put("Jun", "06");
        MONTH_MAP.put("Jul", "07");
        MONTH_MAP.put("Aug", "08");
        MONTH_MAP.put("Sep", "09");
        MONTH_MAP.put("Oct", "10");
        MONTH_MAP.put("Nov", "11");
        MONTH_MAP.put("Dec", "12");
    }

    public static TransferSlip parseSlip(String text) {
        String dateTime = "";
        String time = "";
        double amount = 0.0;
        String sender = "";
        String receiver = "";

        Log.d("SlipParser", "Raw text: " + text);
        String[] lines = text.split("\n");

        // Pattern สำหรับวันที่
        Pattern datePattern = Pattern.compile("(\\d{1,2})\\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s*(\\d{2})\\s*(\\d{1,2}:\\d{2})(?:\\s*(?:AM|PM))?");

        // Pattern สำหรับจำนวนเงิน
        Pattern amountPattern = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*[.]\\d{2})\\s*(?:Baht)?");

        // Pattern สำหรับชื่อที่มี MR. หรือขึ้นต้นด้วย MR
        Pattern namePattern = Pattern.compile("(?:^|\\s+)(?:MR\\.|MRS\\.|MISS\\.|Mr\\.|Mrs\\.|Miss\\.|MR|MRS|MISS)\\s+([A-Za-z].+?)(?=\\s*$|\\s+(?:KBank|Bank|Transfer|Favorite|Banking|XXX|\\d))");

        boolean foundValidAmount = false;

        for (String line : lines) {
            line = line.trim();
            Log.d("SlipParser", "Processing line: " + line);

            // หาวันที่และเวลา
            Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find()) {
                String day = dateMatcher.group(1);
                String month = MONTH_MAP.get(dateMatcher.group(2));
                String year = dateMatcher.group(3);
                time = dateMatcher.group(4);

                // แปลง 2 หลักเป็น 4 หลัก และบวก 543 สำหรับปีพุทธศักราช
                int fullYear = 2000 + Integer.parseInt(year) + 543;

                // จัดรูปแบบวันที่
                dateTime = String.format("%02d/%s/%d",
                        Integer.parseInt(day),
                        month,
                        fullYear
                );

                Log.d("SlipParser", "Parsed date: " + dateTime);
                Log.d("SlipParser", "Parsed time: " + time);
            }

            // หาจำนวนเงิน
            if (!foundValidAmount) {
                Matcher amountMatcher = amountPattern.matcher(line);
                while (amountMatcher.find() && !foundValidAmount) {
                    String amountStr = amountMatcher.group(1).trim();
                    try {
                        String processedAmount = amountStr;
                        int lastDotIndex = processedAmount.lastIndexOf('.');
                        if (lastDotIndex != -1) {
                            String decimal = processedAmount.substring(lastDotIndex);
                            processedAmount = processedAmount.substring(0, lastDotIndex).replaceAll("[.,]", "") + decimal;
                        }

                        double parsedAmount = Double.parseDouble(processedAmount);
                        Log.d("SlipParser", "Found amount: " + parsedAmount);
                        if (parsedAmount > 0) {
                            amount = parsedAmount;
                            foundValidAmount = true;
                            Log.d("SlipParser", "Selected first valid amount: " + amount);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        Log.e("SlipParser", "Error parsing amount: " + amountStr, e);
                    }
                }
            }

            // หาชื่อ sender และ receiver
            Matcher nameMatcher = namePattern.matcher(line);
            if (nameMatcher.find()) {
                String foundName = nameMatcher.group(1).trim();
                Log.d("SlipParser", "Found name: " + foundName);

                if (sender.isEmpty()) {
                    sender = foundName;
                    Log.d("SlipParser", "Set sender: " + sender);
                } else if (receiver.isEmpty() && !foundName.equals(sender)) {
                    receiver = foundName;
                    Log.d("SlipParser", "Set receiver: " + receiver);
                }
            }
        }

        Log.d("SlipParser", String.format("Parsed: Date=%s, Time=%s, Amount=%.2f, Sender=%s, Receiver=%s",
                dateTime, time, amount, sender, receiver));

        return new TransferSlip(dateTime + " " + time, amount, sender, receiver);
    }
}
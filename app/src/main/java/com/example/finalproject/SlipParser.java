package com.example.finalproject;

import android.util.Log;

public class SlipParser {
    public static TransferSlip parseSlip(String text) {
        String dateTime = "";
        double amount = 0.0;
        String sender = "";
        String receiver = "";
        boolean foundAmount = false;
        boolean readNextAmount = false;

        // Add debug logging
        Log.d("SlipParser", "Raw text: " + text);

        // Split text into lines
        String[] lines = text.split("\n");

        for (String line : lines) {
            Log.d("SlipParser", "Processing line: " + line);

            // Extract date and time
            if (line.matches("\\d{1,2}\\s+[A-Za-z]+\\s+\\d{2}\\s+\\d{1,2}:\\d{2}\\s+[AP]M")) {
                dateTime = line.trim();
            }

            // Check if we found the Amount keyword
            if (line.contains("Amount:")) {
                readNextAmount = true;
                continue;
            }

            // Extract amount - only process the next Baht value after "Amount:"
            if (readNextAmount && !foundAmount && line.contains("Baht")) {
                Log.d("SlipParser", "Found amount line: " + line);
                String amountStr = line.replace("Baht", "").trim();
                amountStr = amountStr.replace(",", "").replace(".", "");
                Log.d("SlipParser", "Cleaned amount string: " + amountStr);
                
                try {
                    if (!amountStr.isEmpty()) {
                        // Convert to double and divide by 100 to get correct decimal place
                        amount = Double.parseDouble(amountStr) / 100;
                        Log.d("SlipParser", "Parsed amount: " + amount);
                        foundAmount = true;  // Mark that we've found our amount
                    }
                } catch (NumberFormatException e) {
                    Log.e("SlipParser", "Error parsing amount: " + amountStr, e);
                }
            }

            // Extract sender and receiver
            if (line.contains("MR.") && sender.isEmpty()) {
                sender = line.trim();
            } else if (line.contains("MR.") && !sender.isEmpty()) {
                receiver = line.trim();
            }
        }

        return new TransferSlip(dateTime, amount, sender, receiver);
    }
}

package com.example.finalproject;

public class TransferSlip {
    private String dateTime;
    private double amount;
    private String sender;
    private String receiver;

    public TransferSlip(String dateTime, double amount, String sender, String receiver) {
        this.dateTime = dateTime;
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
    }

    // Getters and setters
    public String getDateTime() { return dateTime; }
    public double getAmount() { return amount; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
}
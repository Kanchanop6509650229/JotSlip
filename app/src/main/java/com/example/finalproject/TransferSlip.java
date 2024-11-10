package com.example.finalproject;

public class TransferSlip {
    private String dateTime;
    private String type;
    private double amount;
    private String sender;
    private String receiver;
    private String description;
    private String image;
    private String category;

    // Constructor with all fields
    public TransferSlip(String dateTime, double amount, String sender, String receiver, String description, String image, String category) {
        this.dateTime = dateTime;
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
        this.description = description;
        this.image = image;
        this.category = category;
    }

    // Minimal constructor used by SlipParser
    public TransferSlip(String dateTime, double amount, String sender, String receiver) {
        this.dateTime = dateTime;
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
        this.description = "";
        this.image = "";
        this.category = "อื่นๆ"; // Default category
    }

    // Getters
    public String getDateTime() {
        return dateTime;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getCategory() {
        return category;
    }

    // Setters
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

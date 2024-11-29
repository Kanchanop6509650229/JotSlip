package com.demo.JotSlip;

public class TransferSlip {
    private long id;
    private String dateTime;
    private int type;
    private double amount;
    private String receiver;
    private String description;
    private String image;
    private String category;

    // Constructor with all fields
    public TransferSlip(long id, String dateTime, double amount, String receiver, String description, String image, String category, int type) {
        this.id = id;
        this.dateTime = dateTime;
        this.amount = amount;
        this.receiver = receiver;
        this.description = description;
        this.image = image;
        this.category = category;
        this.type = type;
    }

    // Minimal constructor used by SlipParser
    public TransferSlip(String dateTime, double amount, String receiver) {
        this.dateTime = dateTime;
        this.amount = amount;
        this.receiver = receiver;
        this.description = "";
        this.image = "";
        this.category = "อื่นๆ"; // Default category
    }

    // Getters
    public String getDateTime() {
        return dateTime;
    }

    public int getType() {
        return type;
    }

    public double getAmount() {
        return amount;
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

    public long getId() {
        return id;
    }

    // Setters
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

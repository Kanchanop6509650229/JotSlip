package com.demo.JotSlip;

public class CategoryConstants {
    // Category Keys
    public static final String CATEGORY_OTHERS = "category_others";
    public static final String CATEGORY_FOOD = "category_food";
    public static final String CATEGORY_SHOPPING = "category_shopping";
    public static final String CATEGORY_FAMILY = "category_family";
    public static final String CATEGORY_SAVINGS = "category_savings";
    public static final String CATEGORY_BILLS = "category_bills";
    public static final String CATEGORY_ENTERTAINMENT = "category_entertainment";
    public static final String CATEGORY_GIFTS = "category_gifts";
    public static final String CATEGORY_TRAVEL = "category_travel";
    public static final String CATEGORY_EDUCATION = "category_education";
    public static final String CATEGORY_HOTEL = "category_hotel";
    public static final String CATEGORY_INSURANCE = "category_insurance";
    public static final String CATEGORY_WITHDRAWAL = "category_withdrawal";
    public static final String CATEGORY_CREDIT = "category_credit";

    // Get category key from display text (for migration and backwards compatibility)
    public static String getCategoryKey(String displayText) {
        switch (displayText) {
            case "อาหาร/เครื่องดื่ม":
            case "Food/Drink":
                return CATEGORY_FOOD;
            case "ช็อปปิ้ง":
            case "Shopping":
                return CATEGORY_SHOPPING;
            case "ครอบครัว/ส่วนตัว":
            case "Family/Personal":
                return CATEGORY_FAMILY;
            case "ออมเงิน/ลงทุน":
            case "Savings/Investment":
                return CATEGORY_SAVINGS;
            case "ชำระบิล":
            case "Bill Payment":
                return CATEGORY_BILLS;
            case "บันเทิง":
            case "Entertainment":
                return CATEGORY_ENTERTAINMENT;
            case "ของขวัญ/บริจาค":
            case "Gifts/Donation":
                return CATEGORY_GIFTS;
            case "ค่าเดินทาง":
            case "Travel":
                return CATEGORY_TRAVEL;
            case "การศึกษา":
            case "Education":
                return CATEGORY_EDUCATION;
            case "โรงแรม/ท่องเที่ยว":
            case "Hotel/Tourism":
                return CATEGORY_HOTEL;
            case "ประกัน":
            case "Insurance":
                return CATEGORY_INSURANCE;
            case "ถอนเงิน":
            case "Withdrawal":
                return CATEGORY_WITHDRAWAL;
            case "สินเชื่อ/เช่าซื้อ":
            case "Credit/Hire Purchase":
                return CATEGORY_CREDIT;
            default:
                return CATEGORY_OTHERS;
        }
    }

    // Get display text from category key based on current locale
    public static int getDisplayStringResource(String categoryKey) {
        switch (categoryKey) {
            case CATEGORY_FOOD:
                return R.string.category_food;
            case CATEGORY_SHOPPING:
                return R.string.category_shopping;
            case CATEGORY_FAMILY:
                return R.string.category_family;
            case CATEGORY_SAVINGS:
                return R.string.category_savings;
            case CATEGORY_BILLS:
                return R.string.category_bills;
            case CATEGORY_ENTERTAINMENT:
                return R.string.category_entertainment;
            case CATEGORY_GIFTS:
                return R.string.category_gifts;
            case CATEGORY_TRAVEL:
                return R.string.category_travel;
            case CATEGORY_EDUCATION:
                return R.string.category_education;
            case CATEGORY_HOTEL:
                return R.string.category_hotel;
            case CATEGORY_INSURANCE:
                return R.string.category_insurance;
            case CATEGORY_WITHDRAWAL:
                return R.string.category_withdrawal;
            case CATEGORY_CREDIT:
                return R.string.category_credit;
            default:
                return R.string.category_others;
        }
    }
}
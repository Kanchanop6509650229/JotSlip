package com.example.finalproject;

import static android.provider.BaseColumns._ID;
import static com.example.finalproject.Constants.CATEGORY;
import static com.example.finalproject.Constants.DATE;
import static com.example.finalproject.Constants.DESCRIPTION;
import static com.example.finalproject.Constants.IMAGE;
import static com.example.finalproject.Constants.MONEY;
import static com.example.finalproject.Constants.RECEIVER;
import static com.example.finalproject.Constants.TABLE_NAME;
import static com.example.finalproject.Constants.TIME;
import static com.example.finalproject.Constants.TYPE;

import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageButton addbtn;
    private RecyclerView recyclerView;
    private View historyNav;
    private ImageView historyIcon;
    private TextView historyText;
    private View homeNav;
    private ImageView homeIcon;
    private TextView homeText;
    private EventsData events;
    private TextView remainAmount;
    private TextView seeAllTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.homepage);
        
        events = new EventsData(this);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.list_item_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(true);

        addbtn = findViewById(R.id.add_btn);
        addbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSlipActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        ImageButton addBtn = findViewById(R.id.add_btn);
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_button);
        
        remainAmount = findViewById(R.id.totalRemain);
        getRemainMoney();
        
        addBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(scaleAnimation);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.clearAnimation();
                    v.setScaleX(1);
                    v.setScaleY(1);
                    break;
            }
            return false;
        });

        seeAllTransaction = findViewById(R.id.seeAllText);
        seeAllTransaction.setOnClickListener(this);

        // Add after initializing views
        historyNav = findViewById(R.id.nav_history);
        historyIcon = historyNav.findViewById(android.R.id.icon);
        historyText = historyNav.findViewById(android.R.id.text1);
        historyNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            
            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            
            startActivity(intent, options);
        });

        homeNav = findViewById(R.id.nav_home);
        homeIcon = homeNav.findViewById(R.id.home_icon);
        homeText = homeNav.findViewById(R.id.home_text);
        
        // Highlight history icon and text
        historyIcon.setColorFilter(getColor(R.color.gray));
        historyText.setTextColor(getColor(R.color.gray));
    }

    private void getRemainMoney() {
        Cursor cursor = getEvents();
        List<TransferSlip> slipList = new ArrayList<>();
        float totalRemain = 0f;
        int count = 0;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(cursor.getColumnIndex(TYPE));
                double money = cursor.getDouble(cursor.getColumnIndex(MONEY));
                String dateStr = cursor.getString(cursor.getColumnIndex(DATE));
                String timeStr = cursor.getString(cursor.getColumnIndex(TIME));
                String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
                String category = cursor.getString(cursor.getColumnIndex(CATEGORY));
                String receiver = cursor.getString(cursor.getColumnIndex(RECEIVER));
                String image = cursor.getString(cursor.getColumnIndex(IMAGE));
                
                try {
                    if (type == 1) {
                        totalRemain += (float) money;
                    } else {
                        totalRemain -= (float) money;
                    }

                    if(count < 3) {
                        TransferSlip slip = new TransferSlip(
                            dateStr + " " + timeStr,
                            money,
                            "",
                            receiver,
                            description,
                            image,
                            category,
                            type
                        );
                        slipList.add(slip);
                        count++;
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error parsing date: " + dateStr, e);
                }
            } while (cursor.moveToNext());
            
            remainAmount.setText(formatNumber(totalRemain) + " ฿");
            if (totalRemain < 0){
                remainAmount.setTextColor(getColor(R.color.red));
            } else {
                remainAmount.setTextColor(getColor(R.color.green_500));
            }

            ListAdapter adapter = new ListAdapter(slipList);
            recyclerView.setAdapter(adapter);
            
            cursor.close();
        }
    }

    class DecimalDigitsInputFilter implements InputFilter {
        private Pattern mPattern;
        DecimalDigitsInputFilter(int digits, int digitsAfterZero) {
            mPattern = Pattern.compile("[0-9]{0," + (digits - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) +
                    "})?)||(\\.)?");
        }
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Matcher matcher = mPattern.matcher(dest);
            if (!matcher.matches())
                return "";
            return null;
        }
    }

    // Add this utility method to help with number formatting
    private String formatNumber(float number) {
        DecimalFormat df = new DecimalFormat("###,###,###,###.##");
        return df.format(number);
    }

    private Cursor getEvents() {
        String[] FROM = {_ID, TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE};
        String ORDER_BY = "substr(date, -4) DESC, " +  // ปี
                         "substr(date, 4, 2) DESC, " +  // เดือน
                         "substr(date, 1, 2) DESC, " +  // วัน
                         TIME + " DESC";                // เวลา
        SQLiteDatabase db = events.getReadableDatabase();

        return db.query(TABLE_NAME, FROM, null, null, null, null, ORDER_BY);
    }

    @Override
    public void onClick(View v) {
        if (v == seeAllTransaction) {
            Intent intent = new Intent(this, HistoryActivity.class);
            
            Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
            
            startActivity(intent, options);
        }
    }
}

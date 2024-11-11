package com.example.finalproject;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ImageButton addbtn;
    private RecyclerView recyclerView;
    private View historyNav;
    private ImageView historyIcon;
    private TextView historyText;
    private View homeNav;
    private ImageView homeIcon;
    private TextView homeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.homepage);
        
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
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
}

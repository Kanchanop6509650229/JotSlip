package com.example.finalproject;

import static com.example.finalproject.Constants.CATEGORY;
import static com.example.finalproject.Constants.DATE;
import static com.example.finalproject.Constants.DESCRIPTION;
import static com.example.finalproject.Constants.IMAGE;
import static com.example.finalproject.Constants.MONEY;
import static com.example.finalproject.Constants.RECEIVER;
import static com.example.finalproject.Constants.TABLE_NAME;
import static com.example.finalproject.Constants.TIME;
import static com.example.finalproject.Constants.TYPE;
import static com.example.finalproject.Constants._ID;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlipInfoActivity extends AppCompatActivity {

    DecimalFormat formatter = new DecimalFormat("#,###,###.##");
    private TextView receiverTextView;
    private EditText moneyEditText;
    private Button btnDate;
    private Button btnTime;
    private Spinner spinner;
    private Button submitBtn;
    private EventsData events;
    private EditText descriptionEditText;
    private Uri image_uri;

    private SlipProcessor slipProcessor;
    private Bitmap currentBitmap;
    private boolean isIncome = true;

    Calendar myCalendar = Calendar.getInstance();

    private ImageButton galleryButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.slip_info);

        long slipId = getIntent().getLongExtra("slip_id", -1);
        if (slipId != -1) {
            events = new EventsData(this);
            loadSlipData(slipId);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadSlipData(long id) {
        SQLiteDatabase db = events.getReadableDatabase();
        String[] columns = {TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE};
        String selection = _ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Get views
            RadioGroup radioGroup = findViewById(R.id.radio_group);
            TextView moneyText = findViewById(R.id.add_money);
            TextView typeText = findViewById(R.id.type_text);
            TextView descriptionText = findViewById(R.id.description);
            TextView dateText = findViewById(R.id.date_text);
            TextView timeText = findViewById(R.id.time_text);
            TextView receiverText = findViewById(R.id.receiver);
            ImageView galleryImage = findViewById(R.id.gallery_btn);

            // Set data
            int type = cursor.getInt(cursor.getColumnIndex(TYPE));
            radioGroup.check(type == 1 ? R.id.radio_income : R.id.radio_outcome);
            
            moneyText.setText(String.format("%.2f", cursor.getDouble(cursor.getColumnIndex(MONEY))));
            typeText.setText(cursor.getString(cursor.getColumnIndex(CATEGORY)));
            descriptionText.setText(cursor.getString(cursor.getColumnIndex(DESCRIPTION)));
            dateText.setText(cursor.getString(cursor.getColumnIndex(DATE)));
            timeText.setText(cursor.getString(cursor.getColumnIndex(TIME)));
            receiverText.setText(cursor.getString(cursor.getColumnIndex(RECEIVER)));

            // Load and set image
            String imageString = cursor.getString(cursor.getColumnIndex(IMAGE));
            if (imageString != null && !imageString.isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decode(imageString, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        galleryImage.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    Log.e("SlipInfoActivity", "Error loading image: " + e.getMessage());
                    galleryImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                galleryImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

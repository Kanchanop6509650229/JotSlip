package com.demo.JotSlip;

import static com.demo.JotSlip.Constants.CATEGORY;
import static com.demo.JotSlip.Constants.DATE;
import static com.demo.JotSlip.Constants.DESCRIPTION;
import static com.demo.JotSlip.Constants.IMAGE;
import static com.demo.JotSlip.Constants.MONEY;
import static com.demo.JotSlip.Constants.RECEIVER;
import static com.demo.JotSlip.Constants.TABLE_NAME;
import static com.demo.JotSlip.Constants.TIME;
import static com.demo.JotSlip.Constants.TYPE;
import static com.demo.JotSlip.Constants._ID;

import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
    private long slipId;

    private SlipProcessor slipProcessor;
    private Bitmap currentBitmap;
    private boolean isIncome = true;

    Calendar myCalendar = Calendar.getInstance();

    private ImageButton galleryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slip_info);

        slipId = getIntent().getLongExtra("slip_id", -1);
        if (slipId != -1) {
            events = new EventsData(this);
            loadSlipData(slipId);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.slip_details_title));
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveViewingState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSlipData(slipId);
        loadViewingState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("slipId", slipId);
        outState.putFloat("scrollPosition", getScrollPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            slipId = savedInstanceState.getLong("slipId");
            final float scrollPosition = savedInstanceState.getFloat("scrollPosition");
            scrollToPosition(scrollPosition);
        }
    }

    private void saveViewingState() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.slip_info_state), MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastViewedSlipId", slipId);
        editor.putFloat("scrollPosition", getScrollPosition());
        editor.apply();
    }

    private void loadViewingState() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.slip_info_state), MODE_PRIVATE);
        float scrollPosition = prefs.getFloat("scrollPosition", 0);
        scrollToPosition(scrollPosition);
    }

    private float getScrollPosition() {
        // หากใช้ ScrollView
        ScrollView scrollView = findViewById(R.id.info_scrollview);
        return scrollView != null ? scrollView.getScrollY() : 0;
    }

    private void scrollToPosition(float position) {
        ScrollView scrollView = findViewById(R.id.info_scrollview);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.scrollTo(0, (int) position));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (events != null) {
            events.close();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.delete));
        deleteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        deleteItem.setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) { // ID ของปุ่มลบที่เราสร้าง
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_confirmation_title))
                .setMessage(getString(R.string.delete_confirmation_message))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSlip();
                    }
                })
                .setNegativeButton(getString(R.string.cancel_text), null)
                .show();
    }

    private void deleteSlip() {
        SQLiteDatabase db = events.getWritableDatabase();
        String whereClause = _ID + " = ?";
        String[] whereArgs = { String.valueOf(slipId) };

        int deletedRows = db.delete(TABLE_NAME, whereClause, whereArgs);

        if (deletedRows > 0) {
            // ลบสำเร็จ
            finish(); // ปิดหน้าจอและกลับไปหน้าก่อนหน้า
        } else {
            // แสดงข้อความเมื่อลบไม่สำเร็จ
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.error_title))
                    .setMessage(getString(R.string.error_delete_failed))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }
    }

    private void loadSlipData(long id) {
        SQLiteDatabase db = events.getReadableDatabase();
        String[] columns = { TYPE, MONEY, DATE, TIME, DESCRIPTION, CATEGORY, RECEIVER, IMAGE };
        String selection = _ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Get views
            CardView imageCard = findViewById(R.id.image_card);
            ImageView imageView = findViewById(R.id.gallery_btn);
            RadioGroup radioGroup = findViewById(R.id.radio_group);
            TextView moneyText = findViewById(R.id.add_money);
            TextView typeText = findViewById(R.id.type_text);
            TextView descriptionText = findViewById(R.id.description);
            TextView dateText = findViewById(R.id.date_text);
            TextView timeText = findViewById(R.id.time_text);
            TextView receiverText = findViewById(R.id.receiver);

            // Set data
            int type = cursor.getInt(cursor.getColumnIndex(TYPE));
            radioGroup.check(type == 1 ? R.id.radio_income : R.id.radio_outcome);

            moneyText.setText(String.format("%.2f", cursor.getDouble(cursor.getColumnIndex(MONEY))));

            // Get category key and convert to localized text
            String categoryKey = cursor.getString(cursor.getColumnIndex(CATEGORY));
            typeText.setText(getString(CategoryConstants.getDisplayStringResource(categoryKey)));

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
                        imageCard.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageCard.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e("SlipInfoActivity", "Error loading image: " + e.getMessage());
                    imageCard.setVisibility(View.GONE);
                }
            } else {
                imageCard.setVisibility(View.GONE);
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

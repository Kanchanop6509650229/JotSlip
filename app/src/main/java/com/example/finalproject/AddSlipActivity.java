package com.example.finalproject;

import static com.example.finalproject.Constants.TABLE_NAME;
import static com.example.finalproject.Constants.TYPE;
import static com.example.finalproject.Constants.IMAGE;
import static com.example.finalproject.Constants.MONEY;
import static com.example.finalproject.Constants.CATEGORY;
import static com.example.finalproject.Constants.DESCRIPTION;
import static com.example.finalproject.Constants.DATE;
import static com.example.finalproject.Constants.TIME;
import static com.example.finalproject.Constants.RECEIVER;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddSlipActivity extends AppCompatActivity implements View.OnClickListener {

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
    private TextView removeImageButton;

    private SlipProcessor slipProcessor;
    private Bitmap currentBitmap;
    private boolean isIncome = true;

    private static final int PERMISSION_REQUEST_CODE = 100;

    Calendar myCalendar = Calendar.getInstance();

    private ImageView galleryButton;

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 ขึ้นไป ใช้ READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.READ_MEDIA_IMAGES },
                        PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            // Android 12 ลงมา ใช้ READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                        PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        }
    }

    // Validate input
    private boolean validateInput() {
        String errorMessage = null;

        // 1. Check radio button selection
        RadioGroup radioGroup = findViewById(R.id.radio_group);
        if (radioGroup.getCheckedRadioButtonId() == -1) {
            errorMessage = getString(R.string.error_required_type);
        }

        // 2. Check money input
        if (errorMessage == null) {
            String moneyStr = moneyEditText.getText().toString().trim();
            if (moneyStr.isEmpty()) {
                errorMessage = getString(R.string.error_required_amount);
            } else {
                try {
                    double money = Double.parseDouble(moneyStr);
                    if (money <= 0) {
                        errorMessage = getString(R.string.error_invalid_amount);
                    } else if (money > 99999999.99) {
                        errorMessage = getString(R.string.error_max_amount);
                    }
                } catch (NumberFormatException e) {
                    errorMessage = getString(R.string.error_invalid_amount_format);
                }
            }
        }

        // 3. Check category (spinner)
        if (errorMessage == null) {
            if (spinner.getSelectedItem() == null ||
                    spinner.getSelectedItem().toString().trim().isEmpty()) {
                errorMessage = getString(R.string.error_required_category);
            }
        }

        // 4. Check date and time
        if (errorMessage == null) {
            String dateStr = btnDate.getText().toString();
            String timeStr = btnTime.getText().toString();

            // First check if date is selected
            if (dateStr.equals(getString(R.string.date_format))) {
                errorMessage = getString(R.string.error_required_date);
            }
            // Then check if time is selected
            else if (timeStr.equals(getString(R.string.time_format))) {
                errorMessage = getString(R.string.error_required_time);
            }
            // If both are selected, validate format and future date
            else {
                try {
                    // Parse input date
                    String[] dateParts = dateStr.split("/");
                    int day = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int yearBE = Integer.parseInt(dateParts[2]);
                    int yearCE = yearBE - 543; // Convert BE to CE

                    // Parse input time
                    String[] timeParts = timeStr.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    // Validate date format
                    if (day < 1 || day > 31 || month < 1 || month > 12 ||
                            yearBE < 2500 || yearBE > 2600) {
                        errorMessage = getString(R.string.error_invalid_date_format);
                    }
                    // Validate time format
                    else if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        errorMessage = getString(R.string.error_invalid_time);
                    }
                    // Check if date and time is in the future
                    else {
                        Calendar inputDateTime = Calendar.getInstance();
                        inputDateTime.set(yearCE, month - 1, day, hour, minute, 0);
                        inputDateTime.set(Calendar.MILLISECOND, 0);

                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.MILLISECOND, 0);

                        if (inputDateTime.after(now)) {
                            errorMessage = getString(R.string.error_future_date);
                        }
                    }
                } catch (Exception e) {
                    errorMessage = getString(R.string.error_invalid_date_time);
                }
            }
        }

        // 5. Check time
        if (errorMessage == null) {
            String timeStr = btnTime.getText().toString();
            if (timeStr.equals(getString(R.string.time_format))) {
                errorMessage = getString(R.string.error_required_time);
            } else {
                try {
                    String[] timeParts = timeStr.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        errorMessage = getString(R.string.error_invalid_time);
                    }
                } catch (Exception e) {
                    errorMessage = getString(R.string.error_invalid_time);
                }
            }
        }

        // 6. Optional: Check description length if provided
        if (errorMessage == null) {
            String description = descriptionEditText.getText().toString().trim();
            if (!description.isEmpty() && description.length() > 100) {
                errorMessage = getString(R.string.error_description_length);
            }
        }

        // 7. Optional: Check receiver length if provided
        if (errorMessage == null) {
            String receiver = receiverTextView.getText().toString().trim();
            if (!receiver.isEmpty() && receiver.length() > 50) {
                errorMessage = getString(R.string.error_receiver_length);
            }
        }

        // Show error if any
        if (errorMessage != null) {
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }

        return true;
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher3 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    try {
                        Uri uri = result.getData().getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                        // ปรับขนาดรูปภาพ
                        currentBitmap = getResizedBitmap(bitmap);

                        // อัพเดทการแสดงผล
                        updateImageView();

                        // ประมวลผลสลิป
                        processSlipImage();

                    } catch (Exception e) {
                        Log.e("AddSlipActivity", "Error loading image: " + e.getMessage());
                        Toast.makeText(this, getString(R.string.load_image_error), Toast.LENGTH_SHORT).show();
                        resetImageView();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.add_transactions);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        galleryButton = findViewById(R.id.gallery_btn);
        removeImageButton = findViewById(R.id.remove_image);

        galleryButton.setOnClickListener(v -> {
            checkAndRequestPermissions();
        });

        removeImageButton.setOnClickListener(v -> {
            currentBitmap = null;
            resetImageView();
        });

        // ในเมธอด onCreate ของ AddSlipActivity
        spinner = findViewById(R.id.type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.types_array,
                android.R.layout.simple_spinner_item);

        // สร้าง custom layout สำหรับ dropdown items
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // กำหนดสีเมื่อเลือก item
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    ((TextView) view).setTextColor(getResources().getColor(R.color.black));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        descriptionEditText = findViewById(R.id.description);
        receiverTextView = findViewById(R.id.receiver);
        moneyEditText = findViewById(R.id.add_money);
        moneyEditText.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(8, 2) });

        slipProcessor = new SlipProcessor();

        btnDate = findViewById(R.id.date_btn);
        btnDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(AddSlipActivity.this, d,
                        myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnTime = findViewById(R.id.time_btn);
        btnTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new TimePickerDialog(AddSlipActivity.this, t,
                        myCalendar.get(Calendar.HOUR_OF_DAY),
                        myCalendar.get(Calendar.MINUTE), true).show();
            }
        });

        // Set click listeners for cancel buttons
        TextView moneyCancel = findViewById(R.id.money_cancel);
        TextView descriptionCancel = findViewById(R.id.description_cancel);
        TextView dateCancel = findViewById(R.id.date_cancel);
        TextView timeCancel = findViewById(R.id.time_cancel);
        TextView receiverCancel = findViewById(R.id.receiver_cancel);

        moneyCancel.setOnClickListener(this);
        descriptionCancel.setOnClickListener(this);
        dateCancel.setOnClickListener(this);
        timeCancel.setOnClickListener(this);
        receiverCancel.setOnClickListener(this);

        RadioGroup radioGroup = findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_income) {
                isIncome = true;
            } else if (checkedId == R.id.radio_outcome) {
                isIncome = false;
            }
        });

        submitBtn = findViewById(R.id.submit_btn);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!validateInput()) {
                    return;
                }

                events = new EventsData(AddSlipActivity.this);
                try {
                    // Log the values before saving
                    Log.d("SQLite_Save", "Attempting to save transaction with values:");
                    Log.d("SQLite_Save", "Type (isIncome): " + isIncome);
                    Log.d("SQLite_Save", "Money: " + moneyEditText.getText().toString());
                    Log.d("SQLite_Save", "Category: " + spinner.getSelectedItem().toString());
                    Log.d("SQLite_Save", "Description: " + descriptionEditText.getText().toString());
                    Log.d("SQLite_Save", "Date: " + btnDate.getText().toString());
                    Log.d("SQLite_Save", "Time: " + btnTime.getText().toString());
                    Log.d("SQLite_Save", "Receiver: " + receiverTextView.getText().toString());

                    addEvent();

                    // Log success
                    Log.d("SQLite_Save", getString(R.string.save_success));
                    Toast.makeText(AddSlipActivity.this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                    finish(); // Close activity after successful save

                } catch (Exception e) {
                    // Log any errors
                    Log.e("SQLite_Save", "Error saving transaction: " + e.getMessage());
                    Toast.makeText(AddSlipActivity.this, getString(R.string.error_save_transaction, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                } finally {
                    if (events != null) {
                        events.close();
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // บันทึกข้อมูลที่กำลังป้อน
        saveDraftData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // บันทึกข้อมูลสำหรับ configuration changes
        outState.putString("money", moneyEditText.getText().toString());
        outState.putString("description", descriptionEditText.getText().toString());
        outState.putString("date", btnDate.getText().toString());
        outState.putString("time", btnTime.getText().toString());
        outState.putString("receiver", receiverTextView.getText().toString());
        if (currentBitmap != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
            outState.putByteArray("image", bos.toByteArray());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            moneyEditText.setText(savedInstanceState.getString("money", ""));
            descriptionEditText.setText(savedInstanceState.getString("description", ""));
            btnDate.setText(savedInstanceState.getString("date", getString(R.string.date_format)));
            btnTime.setText(savedInstanceState.getString("time", getString(R.string.time_format)));
            receiverTextView.setText(savedInstanceState.getString("receiver", ""));
            byte[] imageBytes = savedInstanceState.getByteArray("image");
            if (imageBytes != null) {
                currentBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                updateImageView();
            }
        }
    }

    private void saveDraftData() {
        SharedPreferences prefs = getSharedPreferences("AddSlipDraft", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("money", moneyEditText.getText().toString());
        editor.putString("description", descriptionEditText.getText().toString());
        editor.putString("date", btnDate.getText().toString());
        editor.putString("time", btnTime.getText().toString());
        editor.putString("receiver", receiverTextView.getText().toString());
        editor.putBoolean("hasDraft", true);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.unsaved_changes_title))
                    .setMessage(getString(R.string.unsaved_changes_message))
                    .setPositiveButton(getString(R.string.exit), (dialog, which) -> {
                        clearDraftData();
                        finish();
                    })
                    .setNegativeButton(getString(R.string.cancel_text), null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        return !moneyEditText.getText().toString().isEmpty() ||
                !descriptionEditText.getText().toString().isEmpty() ||
                !btnDate.getText().toString().equals(getString(R.string.date_format)) ||
                !btnTime.getText().toString().equals(getString(R.string.time_format)) ||
                !receiverTextView.getText().toString().isEmpty() ||
                currentBitmap != null;
    }

    private void clearDraftData() {
        getSharedPreferences("AddSlipDraft", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void updateImageView() {
        if (currentBitmap != null) {
            galleryButton.setImageBitmap(currentBitmap);
            galleryButton.setBackgroundResource(0);
            removeImageButton.setVisibility(View.VISIBLE);
        } else {
            resetImageView();
        }
    }

    private void resetImageView() {
        galleryButton.setImageResource(R.drawable.ic_upload_image);
        galleryButton.setBackgroundResource(0);
        removeImageButton.setVisibility(View.GONE);
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {
        int maxWidth = Resources.getSystem().getDisplayMetrics().widthPixels - 64; // หักค่า padding
        int maxHeight = maxWidth; // กำหนดความสูงสูงสุดเท่ากับความกว้าง

        float scale = Math.min(
                (float) maxWidth / bitmap.getWidth(),
                (float) maxHeight / bitmap.getHeight());

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ได้รับอนุญาต
                openGallery();
            } else {
                // ถูกปฏิเสธ
                if (shouldShowRequestPermissionRationale(permissions[0])) {
                    // ผู้ใช้ปฏิเสธครั้งแรก แสดงคำอธิบายเพิ่มเติม
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.permission_title))
                            .setMessage(getString(R.string.permission_message))
                            .setPositiveButton(getString(R.string.settings), (dialog, which) -> {
                                dialog.dismiss();
                                checkAndRequestPermissions();
                            })
                            .setNegativeButton(getString(R.string.cancel_text), (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    // ผู้ใช้เลือก "Don't ask again" แสดงข้อความแนะนำให้ไปตั้งค่า
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.permission_title))
                            .setMessage(getString(R.string.permission_settings_message))
                            .setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
                                dialog.dismiss();
                                // เปิดหน้าตั้งค่าแอป
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton(getString(R.string.cancel_text), (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                }
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        Intent chooser = Intent.createChooser(intent, getString(R.string.choose_image));
        activityResultLauncher3.launch(chooser);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.smooth_fade_in, R.anim.slide_down);
    }

    private void addEvent() {
        String imageString = null;

        if (currentBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageBytes = baos.toByteArray();
                imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                Log.d("SQLite_Save", "Image encoded successfully");
            } catch (Exception e) {
                Log.e("SQLite_Save", "Error encoding image: " + e.getMessage());
            }
        }

        SQLiteDatabase db = events.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TYPE, isIncome ? 1 : 0);
        values.put(IMAGE, imageString);
        values.put(MONEY, moneyEditText.getText().toString());
        values.put(CATEGORY, spinner.getSelectedItem().toString());
        values.put(DESCRIPTION, descriptionEditText.getText().toString());
        values.put(DATE, btnDate.getText().toString());
        values.put(TIME, btnTime.getText().toString());
        values.put(RECEIVER, receiverTextView.getText().toString());

        Log.d("SQLite_Save", "Inserting values into database: " + values.toString());

        long newRowId = db.insert(TABLE_NAME, null, values);

        if (newRowId != -1) {
            Log.d("SQLite_Save", "New row inserted with ID: " + newRowId);
        } else {
            Log.e("SQLite_Save", "Failed to insert row");
        }
    }// end addEvent

    DatePickerDialog.OnDateSetListener d = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        }
    };

    TimePickerDialog.OnTimeSetListener t = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            myCalendar.set(Calendar.MINUTE, minute);
            updateTimeLabel();
        }
    };

    private void updateTimeLabel() {
        Button timeBtn = findViewById(R.id.time_btn);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeBtn.setText(timeFormat.format(myCalendar.getTime()));
    }

    private void updateDateLabel() {
        Button dateBtn = findViewById(R.id.date_btn);
        // เปลี่ยนรูปแบบวันที่ให้แสดงปี พ.ศ.
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/", Locale.getDefault());
        String date = dateFormat.format(myCalendar.getTime());
        int yearCE = myCalendar.get(Calendar.YEAR);
        int yearBE = yearCE + 543; // แปลงปี ค.ศ. เป็น พ.ศ.
        dateBtn.setText(date + yearBE);
    }

    private void processSlipImage() {
        if (currentBitmap != null) {
            slipProcessor.processSlip(currentBitmap, new SlipProcessor.OnSlipProcessedListener() {
                @Override
                public void onSlipProcessed(TransferSlip slip) {
                    runOnUiThread(() -> {
                        // แยกวันที่และเวลา
                        String[] dateTimeParts = slip.getDateTime().split(" ");
                        if (dateTimeParts.length >= 2) {
                            try {
                                // แปลงรูปแบบวันที่
                                String dateStr = dateTimeParts[0];
                                String timeStr = dateTimeParts[1];

                                // อัพเดทค่าใน Button
                                Button dateInput = findViewById(R.id.date_btn);
                                Button timeInput = findViewById(R.id.time_btn);

                                dateInput.setText(dateStr);
                                timeInput.setText(timeStr);

                            } catch (Exception e) {
                                Log.e("MainActivity", "Error formatting date: " + e.getMessage());
                            }
                        }

                        // อัพเดทค่าผู้รับโอน
                        EditText receiverInput = findViewById(R.id.receiver);
                        receiverInput.setText(slip.getReceiver());

                        // อัพเดทจำนวนเงิน
                        EditText moneyInput = findViewById(R.id.add_money);
                        moneyInput.setText(String.format("%.2f", slip.getAmount()));
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddSlipActivity.this,
                                getString(R.string.error_processing_slip) + ": " + error,
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
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
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.money_cancel) {
            EditText moneyInput = findViewById(R.id.add_money);
            moneyInput.setText("");
        } else if (id == R.id.description_cancel) {
            EditText descriptionInput = findViewById(R.id.description);
            descriptionInput.setText("");
        } else if (id == R.id.date_cancel) {
            Button dateBtn = findViewById(R.id.date_btn);
            dateBtn.setText(R.string.date_format);
        } else if (id == R.id.time_cancel) {
            Button timeBtn = findViewById(R.id.time_btn);
            timeBtn.setText(R.string.time_format);
        } else if (id == R.id.receiver_cancel) {
            EditText receiverInput = findViewById(R.id.receiver);
            receiverInput.setText("");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

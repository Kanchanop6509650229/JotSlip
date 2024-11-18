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
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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

    private SlipProcessor slipProcessor;
    private Bitmap currentBitmap;
    private boolean isIncome = true;

    Calendar myCalendar = Calendar.getInstance();

    private ImageButton galleryButton;

    ActivityResultLauncher<Intent> activityResultLauncher3 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        try {
                            Uri uri = data.getData();
                            currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            galleryButton.setImageBitmap(currentBitmap);

                            processSlipImage();

                        } catch (Exception e) {
                            Log.e("Log", "Error processing image: " + e.getMessage());
                        }
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
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                Intent chooser = Intent.createChooser(intent, "Select photo from...");
                activityResultLauncher3.launch(chooser);
            }
        });

        spinner = findViewById(R.id.type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_checked);
        spinner.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        descriptionEditText = findViewById(R.id.description);
        receiverTextView = findViewById(R.id.receiver);
        moneyEditText = findViewById(R.id.add_money);
        moneyEditText.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(8, 2)});

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
            public  void onClick(View v) {
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
                    Log.d("SQLite_Save", "Transaction saved successfully");
                    Toast.makeText(AddSlipActivity.this, "บันทึกข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    // Log any errors
                    Log.e("SQLite_Save", "Error saving transaction: " + e.getMessage());
                    Toast.makeText(AddSlipActivity.this, "เกิดข้อผิดพลาดในการบันทึก", Toast.LENGTH_SHORT).show();
                } finally {
                    events.close();
                }
            }
        });
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
    }//end addEvent

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
                                "เกิดข้อผิดพลาด: " + error,
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

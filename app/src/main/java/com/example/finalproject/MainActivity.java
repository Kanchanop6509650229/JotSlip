package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    DecimalFormat formatter = new DecimalFormat("#,###,###.##");
    private TextView dateTimeTextView;
    private TextView amountTextView;
    private TextView senderTextView;
    private TextView receiverTextView;
    private EditText moneyEditText;

    ActivityResultLauncher<Intent> activityResultLauncher3 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        try {
                            Uri uri = data.getData();
                            ImageView imageView = findViewById(R.id.imageView);
                            imageView.getLayoutParams().height = 400;
                            
                            // เก็บ bitmap และแสดงรูป
                            currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            imageView.setImageBitmap(currentBitmap);
                            
                            // เรียกใช้ processSlipImage เมื่อโหลดรูปเสร็จ
                            processSlipImage();
                            
                        } catch (Exception e) {
                            Log.e("Log", "Error processing image: " + e.getMessage());
                        }
                    }
                }
            });

    private SlipProcessor slipProcessor;
    private Bitmap currentBitmap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.add_transactions);

        Spinner spinner = findViewById(R.id.type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_checked);
        spinner.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        moneyEditText = findViewById(R.id.add_money);
        moneyEditText.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(8, 2)});

        final ImageButton btn3 = findViewById(R.id.gallery_btn);
        btn3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.createChooser(intent, "Select photo from...");
                activityResultLauncher3.launch(intent);
            }
        });

        slipProcessor = new SlipProcessor();
    }

    private void processSlipImage() {
        if (currentBitmap != null) {
            slipProcessor.processSlip(currentBitmap, new SlipProcessor.OnSlipProcessedListener() {
                @Override
                public void onSlipProcessed(TransferSlip slip) {
                    runOnUiThread(() -> {
                        // แยกวันที่และเวลา
                        String[] dateTimeParts = slip.getDateTime().split(" ");
                        if (dateTimeParts.length >= 4) {
                            // รวมส่วนวันที่ (วันที่ เดือน ปี)
                            String date = dateTimeParts[0] + " " + dateTimeParts[1] + " " + dateTimeParts[2];
                            // ส่วนเวลา
                            String time = dateTimeParts[3] + " " + dateTimeParts[4];
                            
                            // อัพเดทค่าใน TextView
                            TextView dateInput = findViewById(R.id.date_input);
                            TextView timeInput = findViewById(R.id.time_input);
                            dateInput.setText(date);
                            timeInput.setText(time);
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
                        Toast.makeText(MainActivity.this, 
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
}

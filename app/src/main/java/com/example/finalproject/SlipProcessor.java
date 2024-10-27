package com.example.finalproject;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class SlipProcessor {
    private TextRecognizer recognizer;

    public SlipProcessor() {
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void processSlip(Bitmap slipImage, OnSlipProcessedListener listener) {
        InputImage image = InputImage.fromBitmap(slipImage, 0);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    TransferSlip slip = SlipParser.parseSlip(text.getText());
                    listener.onSlipProcessed(slip);
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    public interface OnSlipProcessedListener {
        void onSlipProcessed(TransferSlip slip);
        void onError(String error);
    }
}
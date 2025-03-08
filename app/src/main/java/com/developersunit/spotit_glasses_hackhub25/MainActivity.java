package com.developersunit.spotit_glasses_hackhub25;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    //private ImageView imageView;
    private TextView tvExtractedText;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        //imageView = findViewById(R.id.imageView);
        tvExtractedText = findViewById(R.id.tvExtractedText);
        tvExtractedText.setMovementMethod(new ScrollingMovementMethod());

        // Image Picker
        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        //Glide.with(this).load(imageUri).into(imageView);
                        extractTextFromImage();
                    }
                });

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    private void extractTextFromImage() {
        if (imageUri == null) return;

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            TextRecognition.getClient(new TextRecognizerOptions.Builder().build())
                    .process(image)
                    .addOnSuccessListener(text -> displayExtractedText(text))
                    .addOnFailureListener(e -> tvExtractedText.setText("Failed to extract text!"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayExtractedText(Text text) {
        if (text.getText().isEmpty()) {
            tvExtractedText.setText("No text found in image.");
        } else {
            String fulltext = text.getText().replaceAll("\\s+", " ").trim();
            System.out.println(fulltext);
            tvExtractedText.setText(fulltext);

            makeTextClickable(tvExtractedText,fulltext);
        }
    }

    private void makeTextClickable(TextView textView, String fulltext) {
        SpannableString spannableString = new SpannableString(fulltext);

        String[] words = fulltext.split("\\s+");
        int startIndex = 0;

        for(String word : words) {
            final int wordStart = startIndex;
            final int wordEnd = startIndex + word.length();

            ClickableSpan clickableSpan =  new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Toast.makeText(MainActivity.this, "You clicked: " + word, Toast.LENGTH_SHORT).show();
                }
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(Color.WHITE);
                }
            };

              spannableString.setSpan(clickableSpan,wordStart,wordEnd,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
              startIndex = wordEnd + 1;
        }
        tvExtractedText.setText(spannableString);
        tvExtractedText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }
}



package com.developersunit.spotit_glasses_hackhub25;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String ESP32_BT_NAME = "ESP32_CAM";
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket btSocket;
    private InputStream inputStream;
    private ImageView imageView;
    private TextView statusText;
    private Button connectBtn;
    //private ImageView imageView;
    private TextView tvExtractedText;
    private final HashMap<String, String> wordCache = new HashMap<>(); // Caching meanings
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Background thread
    private Uri imageUri;
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectBtn = findViewById(R.id.connectBtn);
        //imageView = findViewById(R.id.imageView);
        tvExtractedText = findViewById(R.id.tvExtractedText);
        tvExtractedText.setMovementMethod(new ScrollingMovementMethod());
//
//        // Image Picker
//        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
//                        imageUri = result.getData().getData();
//                        //Glide.with(this).load(imageUri).into(imageView);
//                        extractTextFromImage();
//                    }
//                });
//
//        btnSelectImage.setOnClickListener(v -> {
//            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            imagePickerLauncher.launch(intent);
//        });
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            return;
        }

        checkPermissions();

        connectBtn.setOnClickListener(v -> new Thread(this::connectToESP32).start());
    }
    private void checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_REQUEST_CODE);
            }
        }
    }
    private void connectToESP32() {
        if (!bluetoothAdapter.isEnabled()) {
            runOnUiThread(() -> Toast.makeText(this, "Enable Bluetooth first!", Toast.LENGTH_SHORT).show());
            return;
        }

        BluetoothDevice esp32Device = null;
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (ESP32_BT_NAME.equals(device.getName())) {
                esp32Device = device;
                break;
            }
        }

        if (esp32Device == null) {
            runOnUiThread(() -> Toast.makeText(this, "ESP32 not found!", Toast.LENGTH_SHORT).show());
            return;
        }

        try {
            btSocket = esp32Device.createRfcommSocketToServiceRecord(SERIAL_UUID);
            btSocket.connect();
            inputStream = btSocket.getInputStream();

           // runOnUiThread(() -> statusText.setText("Connected to ESP32!"));

            new Thread(this::receiveImage).start();

        } catch (IOException e) {
            //runOnUiThread(() -> statusText.setText("Connection failed!"));
            Log.e("Bluetooth", "Connection failed", e);
            closeSocket();
        }
    }
    Bitmap bmap;
    private void receiveImage() {
        try {
            ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();

            // Step 1: Read Image Size
            String imageSizeStr = readLine();
            Log.d("Bluetooth", "Received Header: " + imageSizeStr);

            if (!imageSizeStr.startsWith("Image size: ")) {
                Log.e("Bluetooth", "Invalid image size header: " + imageSizeStr);
                return;
            }

            int imageSize;
            try {
                imageSize = Integer.parseInt(imageSizeStr.replace("Image size: ", "").trim());
            } catch (NumberFormatException e) {
                Log.e("Bluetooth", "Failed to parse image size", e);
                return;
            }

            Log.d("Bluetooth", "Expected Image Size: " + imageSize);

            // Step 2: Read Image Data (Exactly imageSize Bytes)
            byte[] buffer = new byte[1024];  // Buffer size
            int bytesRead;
            int totalBytesRead = 0;

            while (totalBytesRead < imageSize) {
                int bytesToRead = Math.min(buffer.length, imageSize - totalBytesRead);
                bytesRead = inputStream.read(buffer, 0, bytesToRead);

                if (bytesRead == -1) break;  // Stop if end of stream
                imageBuffer.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            Log.d("Bluetooth", "Total Image Bytes Received: " + totalBytesRead);

            // Step 3: Validate and Handle Extra Data
            if (totalBytesRead > imageSize) {
                Log.w("Bluetooth", "Received extra bytes! Trimming data...");
                byte[] trimmedImageData = new byte[imageSize];
                System.arraycopy(imageBuffer.toByteArray(), 0, trimmedImageData, 0, imageSize);
                imageBuffer.reset();
                imageBuffer.write(trimmedImageData);
            }

            // Step 4: Convert and Display Image
            byte[] imageData = imageBuffer.toByteArray();
            bmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

            runOnUiThread(() -> {
                if (bmap != null) {
                    //imageView.setImageBitmap(bmap);
                    //statusText.setText("Image received!");
                } else {
                    //statusText.setText("Failed to decode image!");
                    Log.e("Bluetooth", "Bitmap decoding failed!");
                }
            });

        } catch (IOException e) {
            Log.e("Bluetooth", "Image receive error", e);
        }
        extractTextFromImage();
    }


    private void extractTextFromImage() {
//        if (imageUri == null) {
//            Log.e("Debug", "imageUri is null!");
//            return;
//        }

        if (bmap == null) {
            Log.e("Debug", "Bitmap is null!");
            return;
        }

        Bitmap bitmap = bmap;
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognition.getClient(new TextRecognizerOptions.Builder().build())
                .process(image)
                .addOnSuccessListener(text -> displayExtractedText(text))
                .addOnFailureListener(e -> {
                    tvExtractedText.setText("Failed to extract text!");
                    Log.e("Debug", "Text extraction failed", e);
                });
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
//                    Toast.makeText(MainActivity.this, "You clicked: " + word, Toast.LENGTH_SHORT).show();
                    fetchMeaning(word);
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
    private void fetchMeaning(String word) {
        if (wordCache.containsKey(word)) {
            showTopDialog(wordCache.get(word)); // Use cached meaning
        } else {
            executorService.execute(() -> { // Run API call in background
                DictionaryAPIHelper.fetchMeaning(word, result -> {
                    wordCache.put(word, result); // Cache result
                    runOnUiThread(() -> showTopDialog(result)); // Update UI safely
                });
            });
        }
    }
    private void showTopDialog(String message) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.top_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.TOP);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textViewDialog = dialog.findViewById(R.id.textViewDialog);
        textViewDialog.setText(message);

        Button btnOk = dialog.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private String readLine() throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int c;
        while ((c = inputStream.read()) != -1) {
            if (c == '\n') break;
            lineBuffer.write(c);
        }
        return lineBuffer.toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSocket();
    }

    private void closeSocket() {
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Socket close error", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
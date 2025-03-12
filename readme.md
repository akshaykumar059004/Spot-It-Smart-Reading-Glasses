<h1 align="center" id="title">Spot It- Smart Reading Glasses</h1>

<p align="center"><img src="https://socialify.git.ci/akshaykumar059004/Spot-It-Smart-Reading-Glasses/image?font=KoHo&amp;language=1&amp;name=1&amp;owner=1&amp;pattern=Circuit+Board&amp;stargazers=1&amp;theme=Dark" alt="project-image"></p>

<p id="description">Spot It Glasses is a pair of smart reading glasses equipped with a built-in camera. It captures images upon touch and sends them to a mobile companion application. On the app users can select specific words to look up instantly making reading more efficient and accessible.</p>

**Demo Video and Screenshots:**


**Features**

- **Instant Word Lookup:** Allows users to quickly find the meaning of selected words from captured images.
- **Camera Integration:** Built-in camera captures text upon user input.
- **Mobile Companion App:** Seamless connectivity with a smartphone application for text processing.
- **Customizable Word Selection:** Users can choose specific words for quick reference.

**Technologies Used**

- **Hardware:** ESP32 camera board with OV2640 camera module.
- **Embedded Programming:** Developed using Embedded C for ESP32 board.
- **Mobile App:** Developed using Android Studio with Java and XML.
- **OCR Technology:** Ml Kit Library .
- **Communication:** Bluetooth for data transmission between ESP32 and the mobile app.

### Installation

**Mobile Application Setup**

1.
   1. Download or clone this repository:
   ```bash
   git clone https://github.com/yourusername/spot-it-glasses.git
   cd spot-it-glasses
   ```
   Alternatively, you can download the ZIP file and extract it.
2. Open Android Studio and select **Open an Existing Project**.
3. In the file selection window, navigate to the extracted `spot-it-glasses` folder.
4. Allow Android Studio to sync Gradle and install necessary dependencies automatically.
5. Connect your Android device via USB and enable Developer Mode.
6. Click **Run** (▶) in Android Studio to install and launch the application on your device.
7. Alternatively, you can generate an APK by navigating to **Build > Build Bundle(s) / APK(s) > Build APK(s)** and installing the APK manually.

**ESP32 Firmware Setup**

1. Connect the ESP32 board to your computer using a USB cable.
2. Open the Arduino IDE and install the necessary ESP32 board support package.
3. Select the appropriate ESP32 board from the **Tools > Board** menu.
4. Open the firmware code from the repository (`esp32_firmware` folder).
5. Select the correct port under **Tools > Port**.
6. Click the **Upload** button to flash the firmware onto the ESP32.
7. Restart the ESP32 module after flashing.
8. Pair the ESP32 with your mobile application via Bluetooth.

**Usage**

- Wear the glasses and power them on.
- Tap to capture an image of the text.
- The image is sent to the mobile companion app.
- Select specific words to look up instantly on the app.

**Future Improvements**

- Enhancing OCR accuracy for better text recognition.
- Adding support for multiple languages.
- Implementing voice feedback for word meanings.
- Expanding compatibility with additional mobile platforms.
- **Integrating with a Vision Model:** Enhancing text recognition and object detection capabilities using AI-based vision models.

## Contact

For inquiries or collaborations, reach out at <a href="mailto:akshaykumar059004@gmail.com"> or visit our project page.

---

### Disclaimer

This is a prototype project and should not be used as a medical or safety device.


  
  

<h2>🛡️ License:</h2>

This project is licensed under the Apache License 2.0

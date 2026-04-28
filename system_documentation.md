# SmartFall Dashcam System Documentation

The SmartFall Dashcam System bridges real-time wearable machine learning with computer vision. It combines an Android smartphone application (adapted from WearOS) that acts as a highly sensitive fall detector with a Python-based desktop application that acts as a continuous dashcam.

---

## 1. Android Application (WearOS Adapted)

The Android application monitors user movement in real-time and runs a TensorFlow Lite Machine Learning model to detect anomalous movements indicative of a fall.

### Core Features
*   **Continuous Monitoring:** Uses an ongoing Foreground Service (`SensorService.java`) to collect XYZ accelerometer data at high frequencies.
*   **Machine Learning Engine:** Batches sensor data into windows (`alphaQueue`, `betaQueue`) and evaluates them against an embedded LSTM or Personalized model.
*   **Dynamic UI Controls:** The `MainActivity` provides a dashboard displaying live sensor output, total inferences processed, and a **Sensitivity Threshold Slider**. Modifying this slider instantly updates the model's sensitivity without restarting the background service.
*   **Instant Signaling:** When the ML output exceeds the chosen threshold, the engine flags the data window as a potential fall (`"???"`). Before prompting the user with a UI alert, the `Database.java` class extracts the exact hardware timestamp of the sensor data and immediately fires a UDP signal to the Dashcam.

---

## 2. Python Dashcam Application

The Python script (`dashcam.py`) runs on a nearby computer equipped with a webcam. It operates identically to a vehicle dashcam, continuously recording to avoid missing unpredictable events.

### Core Features
*   **Circular Memory Buffer:** The application reads frames via OpenCV and stores them in a highly efficient, thread-safe memory queue (`collections.deque`). It maintains exactly 10 seconds of history. Old frames are continuously overwritten, meaning it uses very little RAM and avoids filling up the hard drive.
*   **Background UDP Listener:** A daemon thread constantly listens on port `5005` for incoming UDP packets, entirely independent of the main video-processing thread.
*   **Timestamped Frames:** Every single frame grabbed from the webcam is tagged with a precise system timestamp before being added to the buffer.
*   **Event Extraction:** Upon receiving a verified fall signal, the app does *not* blindly start recording. Instead, it waits for the requested post-fall duration to pass, then retroactively extracts only the specific frames from the history buffer that correspond to the exact moment of the fall.

---

## 3. Communication Protocol & Time Synchronization

To ensure the camera records the actual fall and not just the aftermath, the system utilizes a **Time-Mapped UDP Protocol**. This approach completely eliminates the need for strict NTP clock synchronization between the phone and the computer, and makes the system immune to standard Wi-Fi network delays.

### The UDP JSON Payload
When a fall is detected, the Android app sends the following JSON packet over UDP:
```json
{
   "event": "FALL_DETECTED",
   "event_timestamp": 1714221345000, 
   "send_timestamp": 1714221345800 
}
```
*   **`event_timestamp`**: The exact millisecond the *sensor reading* occurred that caused the threshold breach.
*   **`send_timestamp`**: The exact millisecond the *phone transmitted* the UDP packet.

### The Time-Mapping Algorithm (How it beats network lag)
Because the Phone and the PC might have internal clocks that are off by several seconds, and because Wi-Fi can introduce unpredictable lag, comparing timestamps directly is unreliable. 

The Python script solves this by calculating the relative offset:

1.  **Receive Time:** The PC records `receive_timestamp` the exact millisecond the packet arrives.
2.  **Calculate Offset:** The script calculates the difference between its own clock and the phone's clock (ignoring the negligible local network transit time):
    `Clock Offset = receive_timestamp - send_timestamp`
3.  **Map the Event:** The script maps the Android hardware event time into the PC's local timeline:
    `Mapped PC Time = event_timestamp + Clock Offset`
4.  **Extract:** The script now knows exactly when the fall happened relative to its own video frames. It waits until `Mapped PC Time + 3 seconds` has passed, then extracts all frames between `[Mapped PC Time - 5 seconds]` and `[Mapped PC Time + 3 seconds]`.

### Advantages
*   If the UDP packet is delayed by the router for 2 full seconds, the `Clock Offset` remains mathematically constant. The script will simply look 2 seconds deeper into its history buffer, perfectly rescuing the delayed fall footage.
*   The system requires zero configuration regarding system time zones or NTP syncing.

---

## 4. How to Use

### Prerequisites
1.  **Network Setup:** Both the Android smartphone and the PC running the Dashcam script must be connected to the same local Wi-Fi network.
2.  **Python Environment:** The PC must have Python 3 installed along with the OpenCV library.
    ```bash
    pip install opencv-python
    ```

### Step 1: Start the Dashcam (PC)
1.  Navigate to the `wear/dashcam_python/` directory in your terminal.
2.  Run the python script:
    ```bash
    python dashcam.py
    ```
    *Note: The script will automatically open your default webcam and display a live feed. It will keep the last 10 seconds of video in memory.*

### Step 2: Configure the Android App
1.  Find the IPv4 address of the PC running the Dashcam script (e.g., `10.205.203.251`).
2.  Open the SmartFall app on your phone.
3.  In the "Settings" card, locate the **Dashcam IP Address** field.
4.  Enter the PC's IP address (it defaults to `10.205.203.251`). This is saved automatically.

### Step 3: Run the System
1.  Open the SmartFall app on your phone.
2.  Toggle **Manual Control** to start monitoring.
3.  Adjust the **Sensitivity Threshold** slider if necessary.
4.  Perform a test fall (or trigger the accelerometer). 
5.  **Result:** The moment the phone detects the fall, you will see a signal appear in the Python terminal. A few seconds later, an MP4 file containing precisely the 5 seconds before the fall and the 3 seconds after the fall will be saved in the `fall_videos` directory on your PC.

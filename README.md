<div align="center">

# 📌 SmartFall – Multi-Model Fall Detection with Personalization

<img src="https://smartfall.github.io/assets/banners_logos/smartfall-banner.svg" alt="SmartFall Banner" width="800"/>

🔗 [Visit Project Website](https://smartfall.github.io/)

</div>

## 🚀 Overview

**SmartFall** is a dual-component Android application built for **real-time fall detection**, user labeling, and data logging using wearable IMU sensors and on-device machine learning.

It comprises two main components:

- 📱 **Phone App**  
  Acts as a controller to initiate and connect to the smartwatch. Minimal UI, primarily for enabling communication.

- ⌚ **Watch App (WearOS)**  
  Handles all core functions, including:
  - Continuous IMU data recording
  - Real-time fall detection using TensorFlow Lite models
  - User prompts for event labeling
  - Local storage and synchronization to Couchbase

This modular setup enables accurate, on-device fall detection with options for general or personalized models and dynamic data labeling.

## 🔧 Prerequisites

- **Java Development Kit (JDK)**: Version **18** or later  
- **Android Studio**: Version **Giraffe (2022.3.1)** or later  
- **WearOS-compatible smartwatch** and **Android phone** with ADB access enabled


## ⚙️ SmartFall Ecosystem

A detailed technical description of the SmartFall system architecture can be found in the following PDF:

📄 [smartfall_ecosystem_2025.pdf](pdfs/smartfall_ecosystem_2025.pdf)

This document includes:
1. Introduction: SmartFall System
2. Installation and Setup
3. Daily Workflow: Starting the App on Phone and Watch
4. Usage of Smartwatch with SmartFall
5. Retrieve the Sensed Data from Couchbase

> 💡 Use this document as a reference for deploying, configuring, and maintaining SmartFall across devices and sessions.


## 🗃️ Offline Data Storage (Without Couchbase)

If Couchbase is not configured or the server is unreachable, SmartFall stores sensor and label data locally on the watch as JSON files. These files are saved in the internal app storage or external directory (e.g., `/sdcard/SmartFall/`). You can retrieve them using ADB:

```bash
adb shell ls /sdcard/SmartFall/
adb pull /sdcard/SmartFall/<filename>.json
```

For internal storage, run:
```bash
adb shell run-as com.example.smartfallwatch cat files/<filename>.json > output.json
```
Each file contains timestamped IMU data, predictions, confidence scores, and user labels for offline analysis.

## 🧪 Adding a New Sensor (e.g., Gyroscope)

If your fall detection model is trained to accept **two separate input tensors** (e.g., one for accelerometer, one for gyroscope), follow these steps to integrate it into the SmartFall app:

1. **Register** both Sensors in `SensorService.java` file in `onStartCommand()`:
    ```bash
    sensorManager.registerListener(this,
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        SensorManager.SENSOR_DELAY_FASTEST);

    sensorManager.registerListener(this,
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
        SensorManager.SENSOR_DELAY_FASTEST);
    ```

2. **Buffer Both Sensor Streams Separately:** In `onSensorChanged()`, maintain two synchronized buffers (e.g., `accBuffer`, `gyroBuffer`) with timestamps:
    ```bash
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        accBuffer.add(new float[]{event.values[0], event.values[1], event.values[2]});
    }
  
    if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
        gyroBuffer.add(new float[]{event.values[0], event.values[1], event.values[2]});
    }
    ```
    Make sure both buffers are aligned to the same time window (e.g., 2 seconds with overlapping frames).

3. **Prepare Dual Input for TFLite Inference:** In your prediction module (`PredictionContext.java` or similar), convert both buffers into `ByteBuffer` or `TensorBuffer` objects:
    ```
    tflite.runForMultipleInputsOutputs(new Object[]{accTensor, gyroTensor}, outputMap);
    ```
    Ensure the model’s input shapes match the expected window dimensions (e.g., `[1, 128, 3]` for each sensor).

4. **Store and Upload Dual-Sensor Data:** Update local JSON logging and upload formatting to include both sensor streams:
    ```bash
    {
      "uuid": "abc123",
      "acc": [[...], [...], ...],
      "gyro": [[...], [...], ...],
      "label": "fall",
      "confidence": 0.92,
      "timestamp": 1719370000000
    }
    ```
5. **Update PHP and Couchbase Storage (Optional):** If uploading to Couchbase, ensure `upsertdocuments.php` accepts and parses both `acc` and `gyro` arrays.

> ⚠️ Make sure to retrain or validate the model with your dual-sensor data structure before converting it into a `.tflite` format and deploying it.

## 🔁 Replacing or Updating the Machine Learning Model

To update the machine learning model used by SmartFall, follow these steps:

1. **Train and Export Your Model:** Train your fall detection model using your preferred framework (e.g., TensorFlow) and export it as a `.tflite` file. Ensure the model:
    - Accepts the same input shape expected by the app (e.g., accelerometer-only or dual-stream with gyroscope).
    - Produces classification probabilities or labels compatible with the app’s output processing.

2. **Replace the Model File:** If you are using local inference (non-cloud), locate the configuration file in wear (e.g., `wear > java > com.example.wear > config > SmartFallConfig.java`). Update the following field to point to your model file:
    ```bash
    public static final String MODEL_NAME = "your_model.tflite";
    ```
    Place your `.tflite` file in the following directory `wear > assets`. Make sure the filename exactly matches the one set in `MODEL_NAME`.

3. **For Cloud-Based Model Download:** If you are using a cloud-based deployment, upload your `.tflite` file to the server specified in `getCloudConfig().downloadModel`.
    The watch will fetch and use this model dynamically at runtime. Ensure the server is configured to serve the correct `.tflite` file when requested.

> ✅ No Java code modification is needed beyond changing the filename in the config.

## Troubleshooting

Listing below are the common issues while running the project:
- If the phone stops transmitting data, you will see “OFF” instead of the data transmitting after clicking Activate. In this case, go to WearOS on the smartphone, please unpair the watch, and pair both the phone and watch again. Make sure you uninstall the app on your phone and watch both before repairing.
- If you can not see the watch in Android Studio, turn off the ADB debugging and turn it on again.
- When you are tracking the prediction values in the log in Android Studio. 
- Sometimes the prediction value can go high. Typically, it should be between 0 and 1. If it is increasing over time, that means the model has not been loaded properly. Try to do it again from the beginning and run it again on the smartphone.

## 📚 Citation

If you use or reference this system in your work, please cite the following paper:

Ngu, A. H., Yasmin, A., Mahmud, T., Mahmood, A., & Sheng, Q. Z. (2024).  
**Demo: P-Fall: Personalization Pipeline for Fall Detection**.  
*Proceedings of the 8th ACM/IEEE International Conference on Connected Health: Applications, Systems and Engineering Technologies (CHASE '23)*, pp. 173–174.  
[https://doi.org/10.1145/3580252.3589412](https://doi.org/10.1145/3580252.3589412)
```
@inproceedings{10.1145/3580252.3589412,
  author    = {Ngu, Anne H. and Yasmin, Awatif and Mahmud, Tarek and Mahmood, Adnan and Sheng, Quan Z.},
  title     = {Demo: P-Fall: Personalization Pipeline for Fall Detection},
  booktitle = {Proceedings of the 8th ACM/IEEE International Conference on Connected Health: Applications, Systems and Engineering Technologies},
  year      = {2024},
  pages     = {173--174},
  publisher = {Association for Computing Machinery},
  location  = {Orlando, FL, USA},
  doi       = {10.1145/3580252.3589412},
  url       = {https://doi.org/10.1145/3580252.3589412},
  keywords  = {fall detection, personalization of ML models, edge computing}
}
```


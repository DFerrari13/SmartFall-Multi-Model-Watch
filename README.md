<div align="center">

# 📌 SmartFall – Multi-Model Fall Detection with Personalization

<img src="https://smartfall.github.io/assets/banners_logos/smartfall-banner.svg" alt="SmartFall Banner" width="800"/>

🔗 [Visit Project Website](https://smartfall.github.io/)

</div>

## 📚 Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [SmartFall Ecosystem](#Smartfall-ecosystem)
- [Offline Data Storage](#offline-data-storage)
- [Adding a New Sensor](#adding-a-new-sensor)
- [Replacing or Updating the Machine Learning Model](#replacing-or-updating-the-machine-learning-model)
- [Troubleshooting](#troubleshootingabc)
- [Citation](#citation)

<a name="overview"></a>
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

<a name="prerequisites"></a>
## 🔧 Prerequisites

- **Java Development Kit (JDK)**: Version **18** or later  
- **Android Studio**: Version **Giraffe (2022.3.1)** or later  
- **WearOS-compatible smartwatch** and **Android phone** with ADB access enabled

<a name="smartfall-ecosystem"></a>
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

<a name="offline-data-storage"></a>
## 🗃️ Offline Data Storage (without Couchbase)

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

<a name="adding-a-new-sensor"></a>
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

<a name="replacing-or-updating-the-machine-learning-model"></a>
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

<a name="troubleshootingabc"></a>
## 🛠️ Troubleshooting

The following are the common issues while running the project:
- If the phone stops transmitting data, you will see “OFF” instead of the data transmitting after clicking Activate. In this case, go to WearOS on the smartphone, please unpair the watch, and pair both the phone and watch again. Make sure you uninstall the app on your phone and watch both before repairing.
- If you can not see the watch in Android Studio, turn off the ADB debugging and turn it on again.
- When you are tracking the prediction values in the log in Android Studio. 
- Sometimes the prediction value can go high. Typically, it should be between 0 and 1. If it is increasing over time, that means the model has not been loaded properly. Try to do it again from the beginning and run it again on the smartphone.

<a name="citation"></a>
## 📚 Citation

If you use or reference this system in your work, please cite the following papers:

> Taylor Mauldin, Anne H. Ngu, Vangelis Metsis, and Marc E. Canby. 2021. Ensemble Deep Learning on Wearables Using Small Datasets. ACM Trans. Comput. Healthcare 2, 1, Article 5 (January 2021), https://doi.org/10.1145/3428666
```
@article{10.1145/3428666,
  author = {Mauldin, Taylor and Ngu, Anne H. and Metsis, Vangelis and Canby, Marc E.},
  title = {Ensemble Deep Learning on Wearables Using Small Datasets},
  year = {2021},
  issue_date = {January 2021},
  publisher = {Association for Computing Machinery},
  address = {New York, NY, USA},
  volume = {2},
  number = {1},
  url = {https://doi.org/10.1145/3428666},
  doi = {10.1145/3428666},
  journal = {ACM Trans. Comput. Healthcare},
  month = December,
  articleno = {5},
  numpages = {30}
}
```

> Ngu AH, Metsis V, Coyne S, Srinivas P, Salad T, Mahmud U, Chee KH. Personalized Watch-Based Fall Detection Using a Collaborative Edge-Cloud Framework. Int J Neural Syst. 2022 Dec;32(12):2250048. doi: 10.1142/S0129065722500484. Epub 2022 Aug 15. PMID: 35972790.
```
@article{Ngu2022FallDetection,
  author    = {Ngu, Anne Hee and Metsis, Vangelis and Coyne, Shuan and Srinivas, Priyanka and Salad, Tarek and Mahmud, Uddin and Chee, Kyong Hee},
  title     = {Personalized Watch-Based Fall Detection Using a Collaborative Edge-Cloud Framework},
  journal   = {International Journal of Neural Systems},
  volume    = {32},
  number    = {12},
  pages     = {2250048},
  year      = {2022},
  doi       = {10.1142/S0129065722500484},
  url       = {https://doi.org/10.1142/S0129065722500484},
  eprint    = {https://europepmc.org/article/MED/35972790},
  publisher = {World Scientific},
  note      = {Epub 2022 Aug 15},
  pmid      = {35972790}
}
```


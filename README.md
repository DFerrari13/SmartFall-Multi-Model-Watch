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

📄 [system_architecture.pdf](pdfs/system_architecture.pdf)

This document includes:
1. Introduction: SmartFall System
2. Installation and Setup
3. Daily Workflow: Starting the App on Phone and Watch
4. Usage of Smartwatch with SmartFall
5. Retrieve the Sensed Data from Couchbase

> 💡 Use this document as a reference for deploying, configuring, and maintaining SmartFall across devices and sessions.


## Troubleshooting

Listing below the common issues while running the project:
- If the phone stops transmitting data, you will see “OFF” instead of the data transmitting after clicking Activate. In this case go to WearOS in phone, please unpair the watch, and pair both phone and watch again. Make sure you uninstall the app in phone and watch both before repairing.
- If you can not see the watch in android studio turn off the ADB debugging and turn it on again.
- When you are tracking the prediction values in the log in Android Studio. 
- Sometimes the prediction value can go high. Usually, it should be from 0-1. If it is increasing over time that means the model has not been loaded properly. Try to do it again from the beginning and run again in phone.

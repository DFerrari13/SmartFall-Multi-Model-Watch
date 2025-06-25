# 📌 SmartFall – Multi-Model Fall Detection with Personalization

## Overview

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

## ⚙️ Architecture

A detailed technical description of the SmartFall system architecture can be found in the following PDF:

📄 [system_architecture.pdf](pdfs/system_architecture.pdf)

This document includes:

1. **Architecture Overview**  
   A full system-level flow diagram showing the flow of data from the phone to the watch, sensor processing, model prediction, user interaction, and Couchbase storage.

2. **Watch App Configuration**  
   A complete table of configuration parameters in `SmartFallConfig.java`, with possible values and descriptions for each (e.g., `MODEL_TYPE`, `OFFLINE_MODEL_THRESHOLD`, `BETA_LIMIT`, etc.).

3. **Example Setup (Default)**  
   A sample snippet showing how the default SmartFall setup is defined in code, ready to be customized.

> 💡 Use this PDF as a reference while modifying system behavior, testing fall detection logic, or configuring the deployment environment.

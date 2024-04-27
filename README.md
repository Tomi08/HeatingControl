# HeatingControl

# Thermostat Control System

## Overview
This project consists of an ESP32-based thermostat device that communicates with a Kotlin Android application over a local Wi-Fi network. The thermostat device reads room temperature and humidity using a DHT11 sensor and provides a web server interface for controlling various functionalities such as setting temperature, switching heating on/off, and monitoring ambient temperature. The Android application allows users to monitor the room temperature, set the mode of operation (manual or automatic), and control the heating.

## Components
- **ESP32 Thermostat Device**: This device runs the Arduino sketch provided (`thermostat_device.ino`). It utilizes an ESP32 microcontroller, a DHT11 temperature and humidity sensor, and connects to a local Wi-Fi network. The device provides a web server interface for controlling and monitoring various functionalities.

- **Kotlin Android Application**: The Android application communicates with the ESP32 device over the local Wi-Fi network. It allows users to monitor the room temperature, set the mode of operation (manual or automatic), control the heating, and view the ambient temperature from the phone's sensors.

## Setup

### ESP32 Thermostat Device
1. Connect the ESP32 microcontroller to the DHT11 sensor and any additional components as per the circuit diagram.
2. Open the Arduino IDE and upload the `projekt.ino` sketch to the ESP32 microcontroller.
3. Modify the following variables in the sketch to match your Wi-Fi network credentials:
   - `const char* ssid`: Your Wi-Fi network SSID.
   - `const char* password`: Your Wi-Fi network password.
4. Adjust any pin assignments or configurations if necessary.
5. Compile and upload the sketch to the ESP32 device.

### Kotlin Android Application
1. Open the Android project in Android Studio.
2. Ensure that the necessary permissions are declared in the `AndroidManifest.xml` file, such as internet access permissions.
3. Modify the `serverUrl` variable in the `MainActivity.kt` file to match the IP address of your ESP32 device and the port used by the web server (default is `8083`).
4. Build and deploy the Android application to your Android device or emulator.

## Usage

### ESP32 Thermostat Device
1. Power on the ESP32 device and ensure it is connected to your Wi-Fi network.
2. Access the ESP32's web server interface by navigating to its IP address in a web browser.
3. Use the web interface to perform the following actions:
   - Set the temperature.
   - Turn heating on/off.
   - Monitor room temperature and humidity.
   - Change the mode of operation (manual or automatic).

### Kotlin Android Application
1. Launch the Android application on your device.
2. Monitor the room temperature displayed on the application.
3. Use the interface to perform the following actions:
   - Set the mode of operation (manual or automatic).
   - Control the heating.
   - View the ambient temperature from the phone's sensors.


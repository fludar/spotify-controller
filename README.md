<h1 align="center">
  Spotify Controller
  <br>
</h1>

<h4 align="center">A lightweight media controller for Windows, designed to manage Spotify playback remotely from an Android device.</h4>

<p align="center">
  <a href="#key-features">Key Features</a> •
  <a href="#build-instructions">Build Instructions</a> •
  <a href="#screenshot">Screenshot</a> •
  <a href="#credits">Credits</a> •
  <a href="#license">License</a>
</p>



## 🧮 Key Features
- Play, pause, skip to the next or previous track.
- Change audio output sources on Windows.
- Control Spotify from a companion Android app.
- See the current song playing.


## 🛠 Build Instructions

This project consists of a Python server for the desktop and a Kotlin-based Android application.

**Note:** This is heavily targeted towards Windows, as it uses PowerShell to get audio devices.

### Server (PC) Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/fludar/spotify-controller.git
    cd spotify-controller
    ```
2.  **Install PowerShell Module (Admin PowerShell):**
    For changing audio devices, you need to install a module. Run this command in an administrator PowerShell window:
    ```powershell
    Install-Module -Name AudioDeviceCmdlets -Force
    ```
3.  **Install Python Dependencies:**
    Navigate to the API directory and install the required Python packages using the `requirements.txt` file.
    ```bash
    cd api
    pip install -r requirements.txt
    ```

### Android App Setup

1.  Open the project's root folder in Android Studio.
2.  Android Studio will automatically handle the Gradle sync and download the necessary dependencies.
3.  Build the project to create an APK (`Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`).


## ▶️ Usage

1.  Run the Python server on your PC from within the `/api/` directory:
    ```bash
    python main.py
    ```
2.  Install the APK you built onto your Android device.
3.  Ensure your Android device and PC are on the same network and launch the app.

## 📸 Screenshot

![screenshot](https://raw.githubusercontent.com/fludar/spotify-controller/master/demo.gif)


## 🙏 Credits

This project was created by me.

## 📖 What did I learn?

-   Basics of Kotlin and building a networked Android application.
-   Using WebSockets for real-time, two-way communication.
-   Interfacing with the native Windows Media API via Python's `winsdk`.
-   Asynchronous programming in Python with `asyncio`.
-   Executing PowerShell scripts from Python to control system settings.
-   Client-server architecture between a mobile device and a desktop PC.

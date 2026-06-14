,# Calendar Android App

This is a Calendar Android app built with **Jetpack Compose** and **Room Database**. It was created as part of the HCI Exam project.

## Features
- **Modern UI:** Built fully with Jetpack Compose following modern design principles.
- **Component-Based Architecture:** The UI is split into reusable components (`TopBar`, `CalendarGrid`, `DayCell`, `TodaySummary`, `EventList`, `BottomNavBar`).
- **Local Storage:** Events are saved locally on your device using Android Room Database.
- **Interactive:** You can add new dummy events to the selected date by clicking the `+` button, and delete them by clicking the trash icon.

## Prerequisites
To run this project, you need:
1. **Android Studio** (Flamingo or newer recommended).
2. **Android SDK** installed (Platform 34).

## How to Build and Run (Turn it on)

### Method 1: Using Android Studio (Recommended)
1. Open **Android Studio**.
2. Click on **File > Open...**
3. Navigate to this `calendar_app` folder and click **Open**.
4. Wait for Gradle to sync the project dependencies. This may take a few minutes.
5. Connect your Android device via USB (with USB Debugging enabled) or start an Android Virtual Device (Emulator) from the Device Manager.
6. Click the green **Run 'app'** button (Play icon) in the top toolbar, or press `Shift + F10`.
7. The app will compile and launch on your device/emulator!

### Method 2: Using the Command Line
1. Open a terminal and navigate to the `calendar_app` folder:
   ```bash
   cd path/to/calendar_app
   ```
2. Make sure the Gradle wrapper is executable:
   ```bash
   chmod +x gradlew
   ```
3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install the APK on your connected device/emulator:
   ```bash
   ./gradlew installDebug
   ```
5. Open the app launcher on your device and tap on **CalendarApp** to start it.

## Architecture & Code Structure
- `MainActivity.kt`: The main entry point that assembles the screen.
- `ui/components/`: Contains all the reusable UI pieces.
- `ui/theme/`: Contains the color palette, typography, and shape definitions.
- `ui/CalendarViewModel.kt`: Manages the state and business logic.
- `data/`: Contains the Room Database setup (`Event`, `EventDao`, `AppDatabase`).

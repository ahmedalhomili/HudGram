# Hudgram for Android

Hudgram is an enhanced, open-source client based on the official [Telegram for Android](https://github.com/DrKLO/Telegram) application. It introduces advanced productivity utilities and customization features while maintaining the core performance, security, and messaging protocols of Telegram.

This repository contains the source code for building Hudgram.

## Key Features

* **Advanced Auto-Reply:** Define rule-based automated responses for private chats (Direct Messages) and group mentions, complete with logging and filtering.
* **Quick Replies Manager:** Organize pre-written message templates to reply instantly using custom options.
* **Enhanced Scheduled Messages:** Set up advanced alarms and repeat rules for scheduling messages, with separate logs for sent actions.
* **Interface Customization:** Options to toggle individual UI components, such as hiding the stories bar, disabling the instant camera, customizing search bar visibility, and adjusting header layouts.
* **Multi-Account Support:** Full configuration isolation for all custom features, allowing separate auto-reply configurations, templates, and schedule rules per logged-in account.

## Prerequisites

To build Hudgram, you will need:
* Android Studio (Koala or newer recommended)
* JDK 17 or JDK 21 (configured in your build environment)
* Android SDK Platform 35 and Build Tools `35.0.0`
* Android NDK `26.3.11579264`

## Compilation and Setup

### 1. Keystore & Credentials
Ensure your release keys are secured. This project reads release signing credentials dynamically from local properties to prevent accidental exposure:

1. Create or open `local.properties` in the root directory.
2. Add your signing configurations:
   ```properties
   RELEASE_KEY_ALIAS=your_key_alias
   RELEASE_KEY_PASSWORD=your_key_password
   RELEASE_STORE_PASSWORD=your_store_password
   ```
3. Copy your release keystore file (`.keystore` or `.jks`) to the root folder (or specify its location in the signing configuration).

### 2. Firebase Configuration
Obtain a `google-services.json` file from your Google Firebase Console for your package name, and place it in the root directory of the project.

### 3. API Credentials
Set up your custom Telegram API application keys:
* Open `BuildVars.java` (located under the messenger module) or specify `APP_ID` and `APP_HASH` values to associate your application with the Telegram API.

### 4. Build
You can compile the app using the command line or Android Studio:
* **Command Line:**
  ```bash
  ./gradlew assembleAfatRelease
  ```
* **PowerShell (Windows):** Run the automated release build script:
  ```powershell
  ./build_release.ps1
  ```

## License and Brand Compliance

* **License:** Hudgram is distributed under the GNU General Public License v2.0 or later. See `LICENSE` for details.
* **Brand Guidelines:** This project is an independent modification. It uses the Telegram API but is unofficial. We request developers building forks of this repository to comply with [Telegram's API terms](https://core.telegram.org/api/obtaining_api_id), use their own package name/API keys, and use custom branding to avoid user confusion.

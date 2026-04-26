# 📞 CallRecorder — Android App

A simple Android app that records phone calls and saves them as `.m4a` audio files.

---

## 🛠️ Setup Instructions

1. **Open in Android Studio**
   - File → Open → select the `CallRecorder` folder

2. **Sync Gradle**
   - Click "Sync Now" when prompted

3. **Run on Device**
   - Connect your Android phone via USB (enable Developer Options + USB Debugging)
   - Click ▶ Run

4. **Grant Permissions**
   - Allow Microphone, Phone State, and Storage when prompted

---

## 📁 Project Structure

```
CallRecorder/
├── app/
│   ├── src/main/
│   │   ├── java/com/callrecorder/
│   │   │   ├── MainActivity.kt
│   │   │   ├── CallRecorderService.kt
│   │   │   ├── PhoneStateReceiver.kt
│   │   │   └── RecordingsAdapter.kt
│   │   ├── res/
│   │   │   └── layout/
│   │   │       ├── activity_main.xml
│   │   │       └── item_recording.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## ⚠️ Known Limitations

- On Android 9+, only your microphone (your voice) may be captured for regular calls
- Some OEM devices (Xiaomi, Samsung) may capture both sides
- VoIP apps (WhatsApp, Telegram) use their own audio streams — not recordable without root
- Not allowed on Google Play Store due to policy restrictions

---

## 📦 Minimum Requirements

- Android Studio Hedgehog or newer
- Android SDK 26+ (Android 8.0)
- Target SDK 34

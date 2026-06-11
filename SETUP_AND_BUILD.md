# PriceTag Scanner — Setup, Build & Testing Guide

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Iguana 2023.2.1+ | developer.android.com/studio |
| JDK | 17+ | bundled with Android Studio |
| Android SDK | API 26–34 | via SDK Manager in Android Studio |
| Test device / emulator | Android 8.0+ (API 26+) | Physical device recommended for camera |

---

## Step 1 — Open the Project

1. Launch **Android Studio**
2. Choose **Open** → select the `PriceTagScanner/` root folder
3. Wait for Gradle sync to complete (~2–5 min on first run — downloads ~400 MB)
4. If Gradle sync fails:
   - **File → Invalidate Caches → Restart**
   - Ensure JDK 17 is set: **File → Project Structure → SDK Location → JDK**

---

## Step 2 — Connect Test Device

**Physical device (strongly recommended for barcode scanning):**
1. Enable Developer Options on Android device
   - Settings → About Phone → tap Build Number 7 times
2. Enable USB Debugging
   - Settings → Developer Options → USB Debugging → ON
3. Connect via USB — allow the permission prompt on device
4. Verify in Android Studio: the device appears in the toolbar dropdown

**Emulator (limited — no real camera for scanning):**
- API 28+ emulator with Google Play
- Scanning will use the emulator's virtual camera (not suitable for production testing)

---

## Step 3 — Configure the Python Server IP

Before first run, either:

**Option A — Edit default in code:**
```
app/src/main/java/com/pricetag/scanner/domain/model/AppSettings.kt
```
Change `serverIp = "192.168.1.100"` to your PC's LAN IP.

**Option B — Configure in the app at runtime:**
- Launch app → tap ⚙ Settings icon (top-right)
- Enter your Windows PC IP address (e.g. `192.168.54.104`)
- Enter port `5000` (must match `android_server_port` in Python app settings)
- Tap **Save Settings**
- The app will auto-connect

---

## Step 4 — Build & Run

### Run directly on device
```
Android Studio toolbar → select your device → ▶ Run (Shift+F10)
```

### Build debug APK
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build release APK
1. Create a keystore:
   **Build → Generate Signed Bundle / APK → APK → Create new keystore**
2. Build → Generate Signed Bundle / APK → APK → Release

---

## Step 5 — Install on Multiple Devices (ADB)

```bash
# Install to all connected devices
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install to specific device
adb -s <DEVICE_SERIAL> install -r app-debug.apk

# List connected devices
adb devices
```

---

## Step 6 — Python Server Setup (Windows PC)

Ensure the Python `android_server.py` module is running:

1. Open **Price Tag v10** application on Windows
2. Go to **Settings → Android Scanner Server**
3. Set Port to `5000`
4. Check **Enable Android Server**
5. Click **Save & Start Server**
6. Confirm: status shows `🟢 Server running on port 5000`

**Firewall rule (one-time setup):**
```cmd
# Run as Administrator in Command Prompt
netsh advfirewall firewall add rule name="PriceTag Android Server" ^
    dir=in action=allow protocol=TCP localport=5000
```

---

## LAN Connection Guide

```
┌─────────────────────┐         Wi-Fi LAN         ┌─────────────────────┐
│   Android Scanner   │ ──────────────────────────► │   Windows PC        │
│   192.168.1.x       │    TCP Port 5000            │   192.168.1.100     │
│                     │                             │   Python App v10    │
│  Sends:             │                             │                     │
│  BARCODE|TYPE|UNIT  │                             │  android_server.py  │
│  |COPIES|TIMESTAMP  │                             │  Listens on :5000   │
│                     │ ◄────────────────────────── │                     │
│  Receives:          │    OK|JOB_ID                │  Fetches price      │
│  OK / ERR / DUP     │    ERR|message              │  Fills template     │
└─────────────────────┘                             │  Prints tag         │
                                                    └─────────────────────┘
```

**Finding the PC's LAN IP:**
```cmd
ipconfig
# Look for: IPv4 Address . . . . . . . . . . 192.168.x.x
```

**Both devices must be on the same Wi-Fi network.**

---

## Testing Checklist

### Connection Tests
- [ ] App starts and shows "CONNECTING…" badge
- [ ] Python server running → badge turns "CONNECTED" (green)
- [ ] Kill Python server → badge turns "DISCONNECTED" (red) within 25s
- [ ] Restart Python server → app auto-reconnects within 30s
- [ ] Reconnect button works manually
- [ ] Heartbeat keeps connection alive after 30 seconds idle

### Scanner Tests
- [ ] SCAN button opens camera preview
- [ ] Torch (flashlight) toggle works
- [ ] EAN-13 barcode scans successfully
- [ ] Beep plays on successful scan (if enabled)
- [ ] Vibration triggers on successful scan (if enabled)
- [ ] Same barcode scanned twice within 2s → second is ignored (dup protection)
- [ ] Close button dismisses scanner without scanning
- [ ] Scanner auto-closes after successful scan

### A4 / VEG Workflow
- [ ] Select A4 → select PCS → tap SCAN → scan barcode
- [ ] Copies dialog appears → enter 3 → Confirm
- [ ] Scanned barcode appears in list
- [ ] Tap SEND → job sent → "✅ Sent! OK|JOBID" snackbar
- [ ] List clears after send
- [ ] Python PC prints the tag

### 4PCS Workflow
- [ ] Select 4PCS → select CTN → tap SCAN
- [ ] Slot 1 fills with barcode → progress shows 1/4
- [ ] Scanner reopens automatically for slot 2
- [ ] Slots 2, 3, 4 fill successively
- [ ] At 4/4 → Copies dialog auto-appears
- [ ] Confirm copies → SEND → Python prints 4-slot tag

### 4PCS Partial Send
- [ ] Scan 2 of 4 slots → tap SEND
- [ ] "Partial Print?" confirmation dialog appears
- [ ] Confirm → job sent with 2 real + 2 padded barcodes
- [ ] Cancel → returns to scan screen

### 4PCS_SAME Workflow
- [ ] Select 4PCS SAME → tap SCAN → scan one barcode
- [ ] "How many slots?" dialog shows (1, 2, 3, 4 buttons)
- [ ] Select 4 → Copies dialog → Confirm → SEND
- [ ] Payload: `BARCODE,BARCODE,BARCODE,BARCODE|4PCS_SAME|PCS|1|ts`

### Offline Queue Tests
- [ ] Disable Wi-Fi or stop Python server
- [ ] Scan and tap SEND → "📥 Saved offline" snackbar
- [ ] Pending badge counter appears in top bar (e.g. "1")
- [ ] Re-enable Wi-Fi / start Python server
- [ ] Within 15 seconds, pending job is auto-sent
- [ ] Badge clears, History shows job as SENT
- [ ] No data loss confirmed

### History Screen Tests
- [ ] History button opens job list (newest first)
- [ ] Search by barcode filters results in real-time
- [ ] Re-send button resends a FAILED/PENDING job
- [ ] Delete button removes a job
- [ ] Clear All removes all jobs
- [ ] Correct status badges (SENT / PENDING / FAILED)

### Settings Tests
- [ ] Change server IP → Save → app reconnects to new IP
- [ ] Toggle beep off → scan → no beep sound
- [ ] Toggle vibrate off → scan → no vibration
- [ ] Enable Auto Send → scan barcode → auto-sends without pressing SEND
- [ ] Settings persist after app restart

### Performance Tests
- [ ] App starts in under 2 seconds
- [ ] Barcode scan-to-send latency under 1 second (on LAN)
- [ ] No ANR (Application Not Responding) during scanning
- [ ] No UI freeze during send operation
- [ ] Battery consumption: normal (no excessive wake locks)
- [ ] App stable after 1 hour continuous use

---

## APK Generation for Distribution

```bash
# From project root
./gradlew assembleRelease

# Output:
# app/build/outputs/apk/release/app-release-unsigned.apk
# (sign with your keystore before distribution)
```

**Signing the APK:**
```bash
# Sign with jarsigner (JDK 17)
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore your-keystore.jks \
    app-release-unsigned.apk your-key-alias

# Align with zipalign
zipalign -v 4 app-release-unsigned.apk app-release.apk
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Gradle sync fails | File → Invalidate Caches; check JDK 17 in Project Structure |
| "Cannot find symbol" errors | Rebuild Project (Build → Rebuild Project) |
| Camera permission denied | App Info → Permissions → Camera → Allow |
| App can't connect to Python server | Check both devices on same Wi-Fi; verify PC IP; check Windows Firewall |
| Barcode not scanning | Ensure good lighting; hold device steady; check focus |
| Duplicate scans triggering | Increase dup window in BarcodeValidator.kt (default 2s) |
| Offline jobs not retrying | Check retry loop in MainViewModel (runs every 15s when connected) |
| Room migration crash | Increment DB version + add migration OR use fallbackToDestructiveMigration |

---

## Project File Structure

```
PriceTagScanner/
├── build.gradle.kts                    ← Root build config
├── settings.gradle.kts                 ← Module includes
├── gradle.properties                   ← Build properties
├── gradle/
│   └── libs.versions.toml              ← Dependency version catalog
├── ARCHITECTURE.md                     ← Architecture diagram + protocol docs
├── SETUP_AND_BUILD.md                  ← This file
└── app/
    ├── build.gradle.kts                ← App build config (dependencies, flavors)
    ├── proguard-rules.pro              ← Release minification rules
    └── src/main/
        ├── AndroidManifest.xml         ← Permissions, activities
        ├── res/
        │   ├── values/colors.xml       ← Brand colors
        │   ├── values/strings.xml      ← App name
        │   ├── values/themes.xml       ← Base theme
        │   └── xml/network_security_config.xml  ← Allow cleartext on LAN
        └── java/com/pricetag/scanner/
            ├── App.kt                  ← Hilt application
            ├── MainActivity.kt         ← Single activity entry point
            ├── navigation/
            │   ├── Screen.kt           ← Route constants
            │   └── AppNavigation.kt    ← NavHost + composable routes
            ├── data/
            │   ├── db/
            │   │   ├── AppDatabase.kt  ← Room database
            │   │   ├── dao/JobDao.kt   ← SQL queries
            │   │   └── entity/JobEntity.kt  ← Print job table
            │   ├── network/
            │   │   ├── SocketManager.kt     ← TCP client + heartbeat
            │   │   └── model/ConnectionState.kt
            │   ├── preferences/
            │   │   └── AppPreferences.kt    ← DataStore settings
            │   └── repository/
            │       ├── JobRepositoryImpl.kt
            │       └── SettingsRepositoryImpl.kt
            ├── domain/
            │   ├── model/
            │   │   ├── TagType.kt      ← A4, 4PCS, VEG, 4PCS_DATE, 4PCS_SAME
            │   │   ├── UnitType.kt     ← PCS, CTN, PKT, KGS
            │   │   ├── ScanJob.kt      ← In-memory job + wire format builder
            │   │   └── AppSettings.kt  ← Settings data class
            │   ├── repository/
            │   │   ├── JobRepository.kt
            │   │   └── SettingsRepository.kt
            │   └── usecase/
            │       ├── BuildPayloadUseCase.kt  ← Validate + format payload
            │       └── SendJobUseCase.kt        ← Save → send → queue fallback
            ├── di/
            │   ├── AppModule.kt        ← Repository bindings
            │   └── DatabaseModule.kt   ← Room + DAO providers
            ├── presentation/
            │   ├── main/
            │   │   ├── MainViewModel.kt  ← All scan/send/clear logic + StateFlow
            │   │   └── MainScreen.kt     ← Primary UI: selectors, slots, buttons
            │   ├── scanner/
            │   │   └── ScannerScreen.kt  ← CameraX + ML Kit full-screen scanner
            │   ├── history/
            │   │   ├── HistoryViewModel.kt
            │   │   └── HistoryScreen.kt  ← Job list, search, resend, delete
            │   ├── settings/
            │   │   ├── SettingsViewModel.kt
            │   │   └── SettingsScreen.kt ← IP, port, toggles
            │   ├── components/
            │   │   ├── ConnectionBadge.kt  ← Live connection indicator
            │   │   ├── SlotGrid.kt         ← 2×2 slot display for 4PCS
            │   │   └── BarcodeList.kt      ← Scrollable barcode list
            │   └── theme/
            │       ├── Color.kt        ← High-contrast retail palette
            │       ├── Type.kt         ← Large-text typography
            │       └── Theme.kt        ← Material 3 dark theme
            └── utils/
                ├── BarcodeValidator.kt  ← 2s duplicate scan protection
                ├── Extensions.kt        ← Time formatting
                └── SoundHapticManager.kt ← Beep + vibration
```

---

*PriceTag Scanner v1.0 — Retail barcode scan-to-print client for Android*
*Connects to Price Tag v10 Python desktop application via LAN TCP*

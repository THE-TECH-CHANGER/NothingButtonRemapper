  <div align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/4/47/Nothing_Logo.svg" alt="Nothing Logo" width="200" style="filter: invert(1);"/>
  <h1>Nothing Button Remapper</h1>
  <p><b>Unlock the full potential of your Nothing Phone's Essential Key without Root!</b></p>

  <a href="https://github.com/THE-TECH-CHANGER/NothingButtonRemapper/releases/latest">
    <img src="https://img.shields.io/github/v/release/THE-TECH-CHANGER/NothingButtonRemapper?style=for-the-badge&color=black" alt="Latest Release">
  </a>
  <a href="https://github.com/THE-TECH-CHANGER/NothingButtonRemapper/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/THE-TECH-CHANGER/NothingButtonRemapper?style=for-the-badge&color=black" alt="License">
  </a>
</div>

<br>

Welcome to **Nothing Button Remapper V2.2**! This open-source app allows you to completely customize the "Essential Key" (or any hardware button mapped to keycode `0`) on Nothing OS, completely bypassing the default behavior.

## ✨ Features (New in V2!)

- **No Root Required!** Uses Android's built-in Accessibility Service + **Shizuku**.
- **App Profiles:** Set custom button actions based on which app is currently open!
- **State Constraints:** Choose to trigger actions only when the screen is off, media is playing, WiFi is connected, and more!
- **Simulate ANY Android Key (New!):** The app now functions as an Accessibility Input Method, allowing you to inject arbitrary Android keycodes (e.g., Volume Up, Media Play/Pause, Navigation keys) directly into the OS without root!
- **Multi-Gesture Engine:** Assign different actions to Single Press, Double Press, Triple Press, and Long Press.
- **Cycle Ringer Mode:** Instantly cycle between Normal ➡️ Vibrate ➡️ Silent modes.
- **Flashlight, Camera, Screenshot:** Quick access to essential utilities.
- **Unlock Wizard:** Easily bypass the OS restriction with a built-in Shizuku wizard.
- **Nothing OS Aesthetic:** Designed with the signature dot-matrix and monochrome feel.

---

## 📥 Installation & Setup Tutorial

Because Nothing OS heavily guards the Essential Key, you need to disable the default system app that handles it. This takes 1 minute and **does not require root or void your warranty**.

### Step 1 — Download & Install the APK

1. Download `NothingButtonRemapper-v2.2.apk` from the [Releases](https://github.com/THE-TECH-CHANGER/NothingButtonRemapper/releases) page.
2. Open your **Downloads** folder and tap the APK file.

---

### ⚠️ Google Play Protect Warning — How to Bypass

When installing, you may see a popup saying **"App blocked to protect your device"**. This is **completely normal** for any sideloaded app that uses Accessibility Service. The app is 100% open-source and safe.

**To install anyway:**

1. Tap **OK** to dismiss the first popup.
2. Open the **Google Play Store** app.
3. Tap your **profile icon** (top right) → **Play Protect**.
4. Tap the **⚙️ gear icon** (top right).
5. Toggle **OFF** "Scan apps with Play Protect".
6. Go back to Downloads and tap the APK — it will now install normally.
7. *(Optional)* Turn Play Protect back on after installing.

> 💡 The warning appears because this app uses Accessibility Service for button remapping. You can verify the app is safe by reviewing the full source code on this GitHub repository.

---

### 🛑 Android 13+ "Restricted Settings" Error

If you are on Android 13 or newer, you might get an **"App was denied access"** or **"Restricted Settings"** popup when trying to enable the Accessibility Service.

**How to fix it:**
1. Open your phone's **Settings** app.
2. Go to **Apps** → **Essential Remapper** (or Nothing Button Remapper).
3. Tap the **3-dot menu** (⋮) in the top-right corner.
4. Tap **"Allow restricted settings"** and authenticate (fingerprint/PIN).
5. Now you can go back and enable the Accessibility Service normally!

---

### Step 2 — App Setup (3 Steps Inside the App)

1. Open the **Nothing Button Remapper** app.
2. Follow the 3-step setup guide on the home screen:
   - **Step 1:** Download and install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
   - **Step 2:** Start Shizuku (using Wireless Debugging — no PC needed!).
   - **Step 3:** Tap **Run** in the app to disable Nothing's default button handler.
3. Turn on the main **Enable Remapping** switch (this will prompt you to enable the Accessibility Service).
4. Customize your gestures (Single Press, Double Press, etc.) and you're done!

---

### 🔄 Upgrading from v2.0?

If you have v2.0 already installed, you must **uninstall it first** before installing v2.2 due to a signing key change.

1. Long-press the app icon → **Uninstall** (or go to Settings → Apps → Essential Remapper → Uninstall)
2. Then install the new `NothingButtonRemapper-v2.2.apk`

---

## 🚀 Future Plans

I am actively working on future updates to make this tool feel even more like a native, built-in system app on Nothing OS! Stay tuned for more features.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Connect with Me

Have feedback, questions, or just want to say hi? Connect with me!  
📸 **Instagram:** [@ft.sjhn](https://instagram.com/ft.sjhn)  
📧 **Email:** [sajhansakkir1@gmail.com](mailto:sajhansakkir1@gmail.com)

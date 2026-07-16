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

Welcome to **Nothing Button Remapper V2**! This open-source app allows you to completely customize the "Essential Key" (or any hardware button mapped to keycode `0`) on Nothing OS, completely bypassing the default behavior.

## ✨ Features (New in V2!)

- **No Root Required!** Uses Android's built-in Accessibility Service + **Shizuku**.
- **Multi-Gesture Engine:** Assign different actions to Single Press, Double Press, Triple Press, and Long Press.
- **Context-Aware Camera Shutter:** The button acts as a shutter when your camera app is open!
- **Cycle Ringer Mode:** Instantly cycle between Normal ➡️ Vibrate ➡️ Silent modes.
- **Flashlight, Camera, Screenshot:** Quick access to essential utilities.
- **In-App Onboarding:** No more PC or Termux needed! Everything is done inside the app via Shizuku.
- **Nothing OS Aesthetic:** Designed with the signature dot-matrix and monochrome feel.

---

## 🚀 Installation & Setup Tutorial

Because Nothing OS heavily guards the Essential Key, you need to disable the default system app that handles it. This takes 1 minute and **does not require root or void your warranty**.

### Quick Setup (Directly in the App)
1. Download the latest `NothingButtonRemapper-v2.0.apk` from the [Releases](https://github.com/THE-TECH-CHANGER/NothingButtonRemapper/releases) page and install it.
2. Open the **Nothing Button Remapper** app.
3. Follow the 3-step setup guide on the home screen:
   - **Step 1:** Download and install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
   - **Step 2:** Start Shizuku (using Wireless Debugging or root).
   - **Step 3:** Tap **Run** in the app to disable Nothing's default button handler.
4. Turn on the main **Enable Remapping** switch (this will prompt you to enable the Accessibility Service).
5. Customize your gestures (Single Press, Double Press, etc.) and you're done!

---

## 🛠️ Building from Source

If you want to compile the app yourself using Android Studio:

```bash
git clone https://github.com/THE-TECH-CHANGER/NothingButtonRemapper.git
cd NothingButtonRemapper
./gradlew assembleDebug
```

## 🚀 Future Plans

I am actively working on future updates to make this tool feel even more like a native, built-in system app on Nothing OS! Stay tuned for more features.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Connect with Me

Have feedback, questions, or just want to say hi? Connect with me!  
📸 **Instagram:** [@ft.sjhn](https://instagram.com/ft.sjhn)  
📧 **Email:** [sajhansakkir1@gmail.com](mailto:sajhansakkir1@gmail.com)

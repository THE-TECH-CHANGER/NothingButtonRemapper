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

Welcome to **Nothing Button Remapper**! This open-source app allows you to completely customize the "Essential Key" (or any hardware button mapped to keycode `0`) on Nothing OS, completely bypassing the default behavior.

## ✨ Features

- **No Root Required!** Uses Android's built-in Accessibility Service.
- **Cycle Ringer Mode:** Instantly cycle between Normal ➡️ Vibrate ➡️ Silent modes with a single click.
- **Flashlight Toggle:** Turn your torch on and off without looking at your screen.
- **Quick Camera:** Launch your camera instantly.
- **Quick Screenshot:** Take screenshots on the fly.
- **Clean UI:** Built with Nothing's signature dot-matrix aesthetic in mind.

---

## 🚀 Installation & Setup Tutorial

Because Nothing OS heavily guards the Essential Key, you need to disable the default system app that handles it. Don't worry, this only takes 1 minute and **does not require root or void your warranty**.

### Written Guide (Using Termux / On-Device)
You can do this entirely on your phone without a PC!

1. Download the latest `NothingButtonRemapper-v1.0.apk` from the [Releases](https://github.com/THE-TECH-CHANGER/NothingButtonRemapper/releases) page and install it.
2. Download and install **Termux** from [F-Droid](https://f-droid.org/packages/com.termux/).
3. Open the **Nothing Button Remapper** app and click **"Copy Command"**.
4. Open **Termux** and install the ADB tools by typing: `pkg install android-tools`
5. Enable **Wireless Debugging** in your phone's Developer Options and pair it with Termux using `adb pair localhost:port`. If you've never done this, watch this quick tutorial: **"Wireless Debugging with Termux"** <br> [![Wireless Debugging with Termux](https://img.youtube.com/vi/KCODAyc_6rU/hqdefault.jpg)](https://youtu.be/KCODAyc_6rU)
6. Paste the copied command into Termux and press enter:
   ```bash
   adb shell pm disable-user --user 0 com.nothing.ntessentialspace && adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder
   ```
6. Open the **Settings** app on your phone.
7. Go to **Accessibility** -> **Nothing Button Remapper** and turn it **ON**.
8. Open the app, select your action (e.g., Cycle Ringer Mode), and you're done!

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

Have feedback, questions, or just want to say hi? Connect with me on Instagram!  
📸 **[@ft.sjhn](https://instagram.com/ft.sjhn)**

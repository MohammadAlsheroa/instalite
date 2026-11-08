<p align="center">
  <img src="docs/banner.svg" alt="InstaLite banner" width="100%"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/minSdk-24-blue" alt="minSdk 24"/>
  <img src="https://img.shields.io/badge/targetSdk-36-blue" alt="targetSdk 36"/>
  <img src="https://img.shields.io/badge/size-~2%20MB-lightgrey" alt="APK size"/>
</p>

---

<img src="docs/icon.svg" alt="InstaLite icon" width="96" align="right"/>

**InstaLite** is a tiny Android app that runs the Instagram mobile website full screen, with one goal: **keep the useful parts (feed, DMs, stories, profiles) and cut off the endless Reels scroll.**

- 🚫 The **Reels tab is gone** from the bottom bar, and the Reels feed can't be opened at all.
- 💬 Reels your **friends send you in DMs still play**.
- ↩️ Try to **swipe to the next reel** and you're sent straight **back to the chat**.

## 🧩 Two versions

The project builds **two apps from the same code** (Gradle product flavors). Both can be installed side by side.

| Version | Flavor | App name | Package | What you get |
|---|---|---|---|---|
| 📸 **Full** | `full` | InstaLite | `com.mohammad.instalite` | Feed, DMs, stories, profiles, with no Reels feed (everything described below) |
| 💬 **Messages only** | `dms` | InstaLite DMs | `com.mohammad.instalite.dms` | **Only Instagram DMs.** Opens on the inbox, and every other page is blocked |

### 💬 Messages-only version (`dms`)

- 📥 Opens straight on the **DM inbox** (`/direct/inbox/`).
- 🚫 **Feed, Explore, Reels, profiles, notifications and search are blocked.** Any navigation outside `/direct/` sends you back to the inbox.
- 🙈 Nav links that leave the DMs (home, explore, reels, create, profile) are **hidden** by `dms.js`.
- 🧹 **Cleaner inbox:** the Notes tray (note bubbles above "Messages"), the useless Back arrow, the username/account switcher and the New message icon are hidden from the inbox header, so the **search bar is the first thing you see** (it stays in the same spot). Inside a chat, Back still returns to the inbox.
- 🔑 Login, 2FA and security-check pages (`/accounts/`, `/challenge/`, `/two_factor`) still work so you can sign in.
- 🎬 Posts, reels and stories **friends send you in a chat still open** (`/p/`, `/reel/`, `/stories/`). The single-reel lock from the full version still applies.

Everything else (full screen, stays logged in, uploads, external links) works the same in both versions.

## ✨ Features

| | Feature | How |
|---|---|---|
| 📱 | **Full screen, mobile layout** | Immersive mode hides status and navigation bars; a WebView loads `instagram.com` with a plain mobile Chrome user agent |
| 🚫 | **No Reels tab** | CSS + DOM script hides the nav item; taps on it are swallowed |
| 🔒 | **Reels feed blocked** | Any navigation to `/reels/` is intercepted natively and sent back |
| 🎬 | **One reel at a time** | A shared reel plays normally; its scroller is locked to that reel |
| ↩️ | **Swipe = back to chat** | Swiping up/down in the viewer closes it and returns you to the conversation |
| 🍪 | **Stays logged in** | Cookies and local storage persist between launches |
| 🖼️ | **Uploads work** | File chooser wired up for sending photos/videos |
| 🌐 | **External links** | Non-Instagram links open in your normal browser |

## 🧭 How it works

```mermaid
flowchart TD
    A[App launch] --> B[Full-screen WebView<br/>instagram.com · mobile UA]
    B --> C[inject.js runs at document start<br/>on every Instagram page]
    C --> D[Hide Reels tab<br/>and block taps on it]
    C --> E[Watch for the reel viewer<br/>a vertical scroll-snap list of videos]
    B --> F[MainActivity checks every URL change<br/>including SPA pushState]
    F -->|/reels/ feed| G[Blocked → back where you came from]
    F -->|/reel/A then /reel/B| G
```

### The DM reel lock

When you tap a reel inside a chat, Instagram doesn't change the URL. It opens an overlay that holds a vertical **scroll-snap list of reels**, with the next ones already loaded. InstaLite finds that list and locks it:

```mermaid
sequenceDiagram
    participant You
    participant Chat as DM chat
    participant Viewer as Reel overlay
    participant Lock as InstaLite lock

    You->>Chat: Tap a reel a friend sent
    Chat->>Viewer: Open overlay (URL unchanged)
    Viewer->>Lock: Scroll-snap container detected
    Lock->>Viewer: Pin to the opened reel (overflow hidden)
    You->>Viewer: Watch ▶️
    You->>Viewer: Swipe to next reel
    Viewer->>Lock: Vertical swipe / scroll detected
    Lock->>Viewer: Pause videos, press the overlay's Close button
    Viewer-->>Chat: You're back in the conversation ✅
```

If the Close button can't be found, InstaLite reloads the chat as a fallback. Either way, you never reach the second reel.

## 📂 Project structure

```
instalite/
├── app/
│   ├── build.gradle.kts           # full + dms product flavors
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── inject.js          # Hides Reels tab, locks the DM reel viewer (both versions)
│       │   └── dms.js             # Messages-only version: hides nav links that leave the DMs
│       ├── java/com/mohammad/instalite/
│       │   └── MainActivity.kt    # WebView, full screen, URL rules, uploads, DMs-only rule
│       └── res/                   # theme + adaptive app icon (vector)
├── docs/                      # README banner + icon preview
├── build.gradle.kts
└── settings.gradle.kts
```

## 🛠️ Build & install

**Requirements:** Android SDK (platform 36), JDK 17+ (the one bundled with Android Studio works).

```bash
git clone https://github.com/MohammadAlsheroa/instalite.git
cd instalite

# Windows
gradlew.bat assembleFullDebug     # full version
gradlew.bat assembleDmsDebug      # messages-only version
# macOS / Linux
./gradlew assembleFullDebug
./gradlew assembleDmsDebug
```

`gradlew assembleDebug` builds both. The APKs end up at:

| Version | APK |
|---|---|
| Full | `app/build/outputs/apk/full/debug/app-full-debug.apk` |
| Messages only | `app/build/outputs/apk/dms/debug/app-dms-debug.apk` |

### Install over Wi-Fi (no cable)

1. On the phone: **Developer options → Wireless debugging → Pair device with pairing code**.
2. On the computer:

```bash
adb pair  <phone-ip>:<pairing-port>  <pairing-code>
adb connect <phone-ip>:<connect-port>        # the port shown on the Wireless debugging screen
adb install -r app/build/outputs/apk/full/debug/app-full-debug.apk   # full version
adb install -r app/build/outputs/apk/dms/debug/app-dms-debug.apk     # messages-only version
```

To switch from the full version to the messages-only one, uninstall the old app first:

```bash
adb uninstall com.mohammad.instalite
adb install -r app/build/outputs/apk/dms/debug/app-dms-debug.apk
adb shell monkey -p com.mohammad.instalite.dms -c android.intent.category.LAUNCHER 1   # open it
```

Or just open the project in Android Studio, pick the `fullDebug` or `dmsDebug` build variant, and press ▶️.

## 🔧 When Instagram changes

Instagram updates its web app often. The reel lock finds the viewer **by behavior** (a vertical scroll-snap container holding videos), not by class names, so it should survive most updates. If something breaks:

| Symptom | Where to look |
|---|---|
| Reels tab visible again | `hideNavReels()` and the CSS selector in `inject.js` |
| Can scroll past a DM reel | `findScroller()` / `lockScroller()` in `inject.js` |
| Reels feed opens | `checkUrl()` in `MainActivity.kt` |
| Messages-only version shows a non-DM page | `DMS_ALLOWED` / `dmsAllowed()` in `MainActivity.kt` |
| Messages-only version shows feed/explore links | `dms.js` |
| Notes tray or inbox Back arrow back again | `hideNotes()` / `tidyBack()` in `dms.js` |
| Username or New message icon back in the inbox header | `tidyHeader()` in `dms.js` |

To inspect the page live, temporarily add `WebView.setWebContentsDebuggingEnabled(true)` in `MainActivity.onCreate` and open `chrome://inspect` on your computer.

## ⚠️ Disclaimer

InstaLite is an unofficial, personal-use project. It isn't affiliated with, endorsed by, or connected to Instagram or Meta. It simply displays the public Instagram website inside an Android WebView. "Instagram" is a trademark of its owner.

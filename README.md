# Immich Slideshow

An Android digital-photo-frame app that turns any Android device — including
cheap "smart frames" like Frameo — into a kiosk-style slideshow backed by your
self-hosted [Immich](https://immich.app) photo library.

Pick an album once, leave it on the wall. The app auto-advances through
photos and videos, plays them full-screen, and shows the time, weather, and
asset metadata as overlays.

## What it does

- **Albums from Immich.** Lists all albums on your server; remembers the one
  you picked. Album thumbnails are cached locally so the picker opens instantly
  on next boot.
- **Slideshow.** Photos advance every 30 s with a crossfade; videos play
  through to the end, then advance. Audio is muted by default — tap the
  bottom-right corner to unmute the current clip.
- **Ken Burns effect.** Photos slowly zoom and pan during display so still
  images feel alive. Toggleable in settings.
- **Display options.** Blurred-background fill (no black bars on portrait
  shots), optional landscape-crop, kiosk-mode system UI hiding.
- **Overlays.** Current date/time (locale-aware), weather (from
  [Open-Meteo](https://open-meteo.com), free, no API key), and per-asset
  metadata (date, place).
- **Gestures.** Horizontal swipe to jump prev/next, pinch to zoom, swipe up
  to return to the album picker.
- **Sleep schedule.** Optional daily off/on time so the frame dims its
  backlight at night and resumes in the morning.
- **Boot-to-app.** Installs as the default launcher so the frame goes
  straight into the slideshow after a power cut.
- **Localised.** English and Dutch UI.

## Requirements

- An Immich server you can reach from the frame's network.
- An Immich API key (Immich → Account → API Keys).
- An Android device running **API 23 (Android 6.0)** or newer.

The Immich server URL and API key are entered in-app on first launch; nothing
is hard-coded at build time.

## Install on a Frameo frame

Frameo frames (e.g. the PFF-1042LW, PFF-1525, etc.) run a locked-down Android
build with the stock Frameo app as the launcher. Installation is done over
ADB.

### 1. Enable ADB on the frame

Frameo doesn't expose Android's normal Developer Options menu. The usual way
in is via the frame's hidden service menu:

1. On the Frameo home screen, tap the gear → **About**.
2. Tap the build / version string **7 times** in a row. You should see a
   "Developer mode enabled" toast.
3. Go back to settings, open the newly-revealed **Developer options**, and
   enable **USB debugging** (and, if you want network ADB, **Wireless
   debugging** / **ADB over network**).

If your firmware version doesn't expose this path, search for the model + "ADB"
— the entry point varies a little between Frameo OS revisions.

### 2. Connect from your computer

Install the [Android platform-tools](https://developer.android.com/tools/releases/platform-tools)
so you have `adb` on your PATH. Then either:

**USB:** plug the frame in, accept the "Allow USB debugging?" prompt on the
frame's screen, then check:

```
adb devices
```

**Wireless:** find the frame's IP in its network settings and run:

```
adb connect <frame-ip>:5555
```

### 3. Install the APK

Grab the latest `app-release.apk` from
[Releases](https://github.com/bart-kors/immich-slideshow/releases) (or build it
yourself — see below). Then:

```
adb install -r app-release.apk
```

### 4. Set as the default launcher

So the frame opens Immich Slideshow on boot instead of the Frameo app:

```
adb shell cmd package set-home-activity com.immichframe.app/.MainActivity
```

(If that command isn't available on your firmware, press the home button on
the frame after install — Android will offer a chooser and you can pick
"Always".)

### 5. First-run configuration

Launch the app once over ADB:

```
adb shell monkey -p com.immichframe.app -c android.intent.category.LAUNCHER 1
```

The Settings screen opens automatically because no server is configured yet.
Enter:

- **Server URL** — e.g. `https://photos.example.com`
- **API key** — paste the key from Immich

After saving, the album picker appears. Tap an album to start the slideshow.

## Building from source

```
git clone https://github.com/bart-kors/immich-slideshow.git
cd immich-slideshow
./gradlew :app:assembleRelease
```

The signed-with-debug-keystore APK lands at
`app/build/outputs/apk/release/app-release.apk`.

Tooling: Kotlin 2.2, AGP 9.2, Gradle 9.5, Compose BOM 2026.05, Room 2.8,
Media3 1.10, Koin 4.2. JDK 17.

## Tech stack

- **UI:** Jetpack Compose + Material 3, HorizontalPager for the slideshow,
  Coil for image loading, Media3/ExoPlayer for video.
- **Data:** Retrofit + OkHttp against the Immich REST API; Room for the
  album thumbnail cache; DataStore for settings.
- **DI:** Koin.

## Acknowledgements

- [Immich](https://immich.app) for the photo server and API.
- [Open-Meteo](https://open-meteo.com) for free weather and geocoding.
- The "slow zoom and pan" effect is named after documentary filmmaker
  [Ken Burns](https://en.wikipedia.org/wiki/Ken_Burns_effect).

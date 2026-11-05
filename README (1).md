# Lockscreen-style display

A Kotlin Android app that displays a clock, wallpaper, optional weather, and opt-in notification previews. It is a visual companion—not an Android system lock screen.

## Security: read before use

Android's own keyguard and device credential remain the only security boundary. This app does not lock the device, does not authenticate users, and must not be used to protect access to a phone or its data. Keep a secure screen lock enabled in Android Settings. The old app-only PIN has been removed because it could be mistaken for device security.

Android may prevent an app's background service from opening an activity when the screen turns on. Automatic display is therefore best-effort and varies by OS version and device manufacturer. The persistent service notification provides a user-initiated way to open the display. Battery management, force-stop, reboot policy, and notification settings may also affect background operation.

## Permissions and data

- **Notifications:** the normal Android notification permission is used for the service's ongoing status notification. Separately, notification-listener access is optional and must be explicitly enabled in Android Settings before the app can read notification previews. Previews are hidden by default. Notification content is held in memory for display and is not saved by this app.
- **Approximate location:** optional weather uses the most recent available approximate location; it does not continuously track the device. When weather is requested, coordinates are sent to Open-Meteo over HTTPS for the forecast. The weather result and fetch time are cached in app-private preferences and can be cleared in the app.
- **Wallpaper:** the selected image is copied into app-private storage and resized for display. Removing the wallpaper deletes that app copy.
- **Internet:** used only for the optional Open-Meteo weather request.
- **Display over other apps (optional):** the app declares this permission only so that Android lets the display open by itself when the screen turns on. It is off until you allow it in Android Settings. It is not used to cover other apps, and it does not make this app a secure keyguard or system overlay. Without it, open the display from the service notification.

## Setup

1. Install the app and keep Android's secure screen lock enabled.
2. Start the display service from the app. Allow service notifications if Android asks. Optionally allow display over other apps so the screen can open by itself when the display turns on.
3. Optionally enable notification-listener access if you want notification cards on the display. Keep previews hidden unless you intentionally want their content visible.
4. Optionally grant approximate location if you want weather. Denying either optional permission leaves the rest of the app usable.
5. Choose a color or photo wallpaper. Stop the display service from the app when you no longer want it.

## Build and test

Requirements: JDK 17 and Android SDK Platform 34 with the Android 34 build tools.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. CI runs unit tests, Android lint, and the debug build. A production release still needs a release keystore, a reviewed privacy policy, device testing, and Play policy checks; do not distribute a debug APK as a release.

## Device testing checklist

Test on physical Android versions you intend to support: first run without optional permissions; allow/deny notification and location access; service start/stop; screen off/on; notification tap fallback; reboot and force-stop behavior; keyguard with PIN/biometric and with no configured device lock; notification preview privacy toggle; large wallpapers; no camera/flashlight hardware; and battery-saver/vendor background restrictions. The app cannot guarantee automatic activity launches on every device.

# TapWatch — conference contact card for Galaxy Watch

A standalone Wear OS app for Abhijith Sivaprasadan's Galaxy Watch Ultra / Wear OS watch.

## What it does

- Shows a deliberately minimal **TAP TO CONNECT** conference screen.
- Displays a QR code for the existing digital card:
  `https://abhijith-sivaprasadan.github.io/tap/?nfc=1&event=cet2026&src=watch`
- Tapping the watch screen switches to a large QR mode.
- Keeps the display awake while the app is open.
- Attempts NFC Forum Type 4 NDEF URL emulation through Android `HostApduService` when the watch exposes `android.hardware.nfc.hce`.
- If HCE is unavailable on the watch firmware, the QR path still works with no companion phone or network requirement.

## Why the NFC path is conditional

The Galaxy Watch Ultra has NFC hardware and runs Wear OS, but NFC hardware alone does **not** prove that the firmware exposes Android's `FEATURE_NFC_HOST_CARD_EMULATION` to third-party apps. TapWatch checks that feature at runtime and shows one of:

- `NFC READY`
- `NFC OFF`
- `HCE UNAVAILABLE`
- `QR READY`

This lets us test the actual watch rather than assuming Samsung exposes HCE.

## Build

Requires Android SDK 35, Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3.

```bash
gradle --no-daemon assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

## Install on the watch with ADB

Enable Developer options and Wireless debugging on the watch, then from a computer with Android platform-tools:

```bash
adb pair WATCH_IP:PAIR_PORT
adb connect WATCH_IP:ADB_PORT
adb install -r app-debug.apk
```

To inspect NFC/HCE features on the physical watch:

```bash
adb shell pm list features | grep nfc
```

On Windows PowerShell:

```powershell
adb shell pm list features | Select-String nfc
```

If `android.hardware.nfc.hce` is present, TapWatch will expose the NDEF service. If it is not present, a normal third-party Wear OS app cannot force HCE support into the firmware.

## Interaction

- Open **TapWatch** before approaching someone.
- If the top-right status says **NFC READY**, have them hold the NFC area of their phone against the watch and test the handoff.
- Tap the watch screen to switch to a large QR code at any time.
- The recipient lands on the digital card with LinkedIn, save-contact, GitHub, portfolio, email and ORCID actions.

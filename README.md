# Rulecade

**English** | [한국어](README-KOR.md)

Learn traffic rules through quick arcade games while the car is parked or charging — built for Pleos Connect.

## Setup

1. Install Android Studio. On Windows, also install Git for Windows and turn on **Windows Hypervisor Platform**.
2. **SDK Manager → SDK Update Sites**: add the Pleos Connect system image, then install it for API 34.
   - macOS (Apple Silicon): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
   - Windows / Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
3. **Device Manager**: Automotive → Pleos Connect v2.1.0, API 34, Cold boot, 4 GB+ RAM. AVD names containing `pleos` are picked up; otherwise set `PLEOS_AVD`.
4. Open the project once in Android Studio (writes `local.properties`). For command-line builds without a JDK 17+, point `JAVA_HOME` to Android Studio's `jbr`.

If the emulator renders badly on integrated graphics, set `PLEOS_GPU=angle_indirect` or `swiftshader_indirect`. See the [setup guide](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator).

## Run

macOS / Linux:

```bash
scripts/emu.sh start
scripts/emu.sh wait
scripts/emu.sh run
```

Windows (PowerShell or cmd):

```bat
scripts\emu.cmd start
scripts\emu.cmd wait
scripts\emu.cmd run
```

Review APK without building: start the emulator, then `adb install -r apk/Rulecade_Rulecade_release.apk`.

Virtual Console: `emu console` → http://127.0.0.1:8282 (`PLEOS_CONSOLE_PORT` to change).

Day/night like the car: `emu theme day|night`. Slow animations: `emu anim 10` (`emu anim 1` restores). Drive through the Virtual Console: `emu drive 40` puts the car in D at 40 km/h, and Pleos covers the app (Rulecade is not declared Distraction Optimized); `emu park` shifts back to P and the app returns where it was. Passenger seat: `emu seat on|off` (the app offers profiles). Charging: `emu charge on|off` (home offers a ten-minute round). Debug builds: `emu navi` replays a short drive's navigation events (school zone, no-parking zone, speed limit, roundabout) and the app finds their signs. App window screenshot for PRs: `emu shot file.png`.

## Stack and test environment

Pleos Connect Emulator v2.1.0 (Android 14, API 34) · Kotlin · Jetpack Compose · Pleos Vehicle SDK 2.3.2

Tested on that emulator's 2560 × 1440 px, 160 dpi display, full screen and in the 1:1 split view, in day and night themes.

## Layout

| Path | What |
| --- | --- |
| `app/` | Android app |
| `scripts/` | Emulator control (`emu.sh`, `emu.cmd`) |
| `design/` | Design tokens (`tokens.json`), the single source for the app theme |
| `apk/` | Signed release APK for review (`Rulecade_Rulecade_release.apk`) |


---

All rights reserved. The Pleos SDK and emulator image are used under the 42dot SDK License Agreement and are not redistributed here.
The Asta Sans font is bundled under the SIL Open Font License 1.1 (`app/src/main/assets/licenses/`).

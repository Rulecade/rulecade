#!/usr/bin/env bash
# Runs the Pleos Connect emulator outside Android Studio.
set -euo pipefail

case "$(uname -s)" in
  Darwin) DEFAULT_SDK="$HOME/Library/Android/sdk" ;;
  *) DEFAULT_SDK="$HOME/Android/Sdk" ;;
esac
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$DEFAULT_SDK}}"
AVD="${PLEOS_AVD:-}"
EMULATOR="$SDK/emulator/emulator"
ADB="$SDK/platform-tools/adb"
LOG="${TMPDIR:-/tmp}/pleos-emulator.log"
CONSOLE_PORT="${PLEOS_CONSOLE_PORT:-8282}"
GPU="${PLEOS_GPU:-host}"
APP="io.github.rulecade"
# App window on the Connect-S display (2560x1440, no split): x y width height
WINDOW="854 93 1678 1206"

pick_avd() {
  [ -n "$AVD" ] || AVD="$("$EMULATOR" -list-avds | grep -i pleos | head -n 1 || true)"
  if [ -z "$AVD" ]; then
    echo "No Pleos AVD found. Create one in Android Studio or set PLEOS_AVD."; exit 1
  fi
}

open_url() {
  if command -v open >/dev/null; then open "$1"; elif command -v xdg-open >/dev/null; then xdg-open "$1"; fi
}

case "${1:-}" in
  start)
    if "$ADB" devices | grep -q '^emulator-'; then
      echo "Emulator already running."; exit 0
    fi
    pick_avd
    # -no-snapshot: always cold boot (required for VHAL) and never write a RAM snapshot to disk
    nohup "$EMULATOR" -avd "$AVD" -no-snapshot -no-boot-anim -gpu "$GPU" >"$LOG" 2>&1 &
    echo "Started $AVD (pid $!). Log: $LOG"
    ;;
  wait)
    "$ADB" wait-for-device
    until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
    echo "Boot completed."
    ;;
  stop)
    "$ADB" emu kill 2>/dev/null || echo "No emulator running."
    ;;
  status)
    "$ADB" devices -l
    ;;
  console)
    # The Virtual Console listens on 8088 inside the guest; only the host side is configurable.
    "$ADB" forward "tcp:$CONSOLE_PORT" tcp:8088 >/dev/null
    echo "Virtual Console: http://127.0.0.1:$CONSOLE_PORT"
    open_url "http://127.0.0.1:$CONSOLE_PORT"
    ;;
  launch)
    "$ADB" shell am start -n "$APP/.MainActivity"
    ;;
  run)
    # Build, install, and cold-start. Pleos apps such as navigation can take the front right
    # after boot or a day/night switch, so bring the app back until it stays on top.
    (cd "$(dirname "$0")/.." && ./gradlew -q installDebug)
    "$ADB" shell am force-stop "$APP"
    for _ in 1 2 3 4 5; do
      "$ADB" shell am start -n "$APP/.MainActivity" >/dev/null 2>&1
      sleep 3
      case "$("$ADB" shell dumpsys activity activities | grep topResumedActivity)" in
        *"$APP/"*) echo "Running."; exit 0 ;;
      esac
    done
    echo "Another app keeps the front. Try 'launch' again."; exit 1
    ;;
  theme)
    case "${2:-}" in
      day|night|sensor) "$ADB" shell cmd car_service day-night-mode "$2" ;;
      *) echo "usage: $0 theme {day|night|sensor}"; exit 1 ;;
    esac
    ;;
  anim)
    # Slow motion for checking or recording animations (e.g. 10); 1 restores normal speed.
    scale="${2:-1}"
    for key in animator_duration_scale transition_animation_scale window_animation_scale; do
      "$ADB" shell settings put global "$key" "$scale"
    done
    echo "Animation scale: $scale"
    ;;
  drive|park)
    # Shifts gear through the Virtual Console: drive puts the car in D at a speed (km/h, default
    # 40), park stops it in P. In D, Pleos covers apps that are not Distraction Optimized.
    # Every request needs a fresh console session; the gear stays after the session ends.
    "$ADB" forward "tcp:$CONSOLE_PORT" tcp:8088 >/dev/null
    url="http://127.0.0.1:$CONSOLE_PORT"
    send() {
      curl -fsS -m 5 -X POST "$url/browser/session?client=emu" -o /dev/null
      curl -fsS -m 5 -X PUT "$url$1" -o /dev/null
    }
    if [ "$1" = drive ]; then
      kmh="${2:-40}"
      send "/vehicle/gear?gear=Drive"
      send "/vehicle/controls?speed=$kmh&accelerator=30&brake=0"
      echo "Gear D at $kmh km/h"
    else
      send "/vehicle/controls?speed=0&accelerator=0&brake=50"
      send "/vehicle/gear?gear=Park"
      echo "Gear P"
    fi
    ;;
  seat)
    # Passenger seat through the Virtual Console: on seats someone beside the driver, off
    # empties the seat. The app then offers profiles.
    "$ADB" forward "tcp:$CONSOLE_PORT" tcp:8088 >/dev/null
    url="http://127.0.0.1:$CONSOLE_PORT"
    value=false
    [ "${2:-on}" = on ] && value=true
    curl -fsS -m 5 -X POST "$url/browser/session?client=emu" -o /dev/null
    curl -fsS -m 5 -X PUT "$url/vehicle/seat?kind=occupied&area=passenger&value=$value" -o /dev/null
    echo "Passenger seat occupied: $value"
    ;;
  charge)
    # EV charging through the Virtual Console: on starts charging, off stops it. While the car
    # charges, home offers a ten-minute round.
    "$ADB" forward "tcp:$CONSOLE_PORT" tcp:8088 >/dev/null
    url="http://127.0.0.1:$CONSOLE_PORT"
    value=false
    [ "${2:-on}" = on ] && value=true
    curl -fsS -m 5 -X POST "$url/browser/session?client=emu" -o /dev/null
    curl -fsS -m 5 -X PUT "$url/vehicle/ev-battery?field=charge_switch&value=$value" -o /dev/null
    echo "Charging: $value"
    ;;
  navi)
    # Debug builds only: replays a short drive's navigation events (school zone, no-parking
    # zone, speed limit, roundabout) into the running app, which finds their signs.
    "$ADB" shell am broadcast --user current -n "$APP/.debug.MockDriveReceiver" >/dev/null
    echo "Replayed a drive"
    ;;
  shot)
    # PR screenshot: the app window only, without the Pleos system UI.
    out="${2:-shot.png}"
    "$ADB" exec-out screencap -p >"$out"
    read -r x y w h <<<"$WINDOW"
    if command -v sips >/dev/null; then
      sips -c "$h" "$w" --cropOffset "$y" "$x" "$out" >/dev/null
    elif command -v magick >/dev/null; then
      magick "$out" -crop "${w}x${h}+${x}+${y}" +repage "$out"
    else
      echo "No sips or ImageMagick: kept the full display."
    fi
    echo "Saved $out"
    ;;
  *)
    echo "usage: $0 {start|wait|stop|status|console|launch|run|theme|anim|drive|park|seat|charge|navi|shot}"; exit 1
    ;;
esac

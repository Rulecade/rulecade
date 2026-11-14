# Runs the Pleos Connect emulator outside Android Studio (Windows).
param(
    [Parameter(Position = 0)]
    [ValidateSet('start', 'wait', 'stop', 'status', 'console', 'launch', 'run', 'theme', 'anim', 'drive', 'park', 'seat', 'charge', 'navi', 'shot')]
    [string]$Command,
    [Parameter(Position = 1)]
    [string]$Value
)

Add-Type -AssemblyName System.Drawing

$Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
       elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
       else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$Emulator = Join-Path $Sdk 'emulator\emulator.exe'
$Adb = Join-Path $Sdk 'platform-tools\adb.exe'
$Log = Join-Path $env:TEMP 'pleos-emulator.log'
$ConsolePort = if ($env:PLEOS_CONSOLE_PORT) { $env:PLEOS_CONSOLE_PORT } else { '8282' }
$Gpu = if ($env:PLEOS_GPU) { $env:PLEOS_GPU } else { 'host' }
$App = 'io.github.rulecade'
# App window on the Connect-S display (2560x1440, no split)
$Window = [System.Drawing.Rectangle]::new(854, 93, 1678, 1206)

function Get-PleosAvd {
    if ($env:PLEOS_AVD) { return $env:PLEOS_AVD }
    $avd = & $Emulator -list-avds | Where-Object { $_ -match 'pleos' } | Select-Object -First 1
    if (-not $avd) {
        Write-Host 'No Pleos AVD found. Create one in Android Studio or set PLEOS_AVD.'
        exit 1
    }
    return $avd
}

switch ($Command) {
    'start' {
        if ((& $Adb devices) -match '^emulator-') { Write-Host 'Emulator already running.'; break }
        $avd = Get-PleosAvd
        # -no-snapshot: always cold boot (required for VHAL) and never write a RAM snapshot to disk
        $proc = Start-Process -FilePath $Emulator -PassThru -NoNewWindow `
            -ArgumentList '-avd', $avd, '-no-snapshot', '-no-boot-anim', '-gpu', $Gpu `
            -RedirectStandardOutput $Log -RedirectStandardError "$Log.err"
        Write-Host "Started $avd (pid $($proc.Id)). Log: $Log"
    }
    'wait' {
        & $Adb wait-for-device
        while ((((& $Adb shell getprop sys.boot_completed) -join '').Trim()) -ne '1') { Start-Sleep -Seconds 2 }
        Write-Host 'Boot completed.'
    }
    'stop' {
        if ((& $Adb devices) -match '^emulator-') { & $Adb emu kill } else { Write-Host 'No emulator running.' }
    }
    'status' { & $Adb devices -l }
    'console' {
        # The Virtual Console listens on 8088 inside the guest; only the host side is configurable.
        & $Adb forward "tcp:$ConsolePort" tcp:8088 | Out-Null
        $url = "http://127.0.0.1:$ConsolePort"
        Write-Host "Virtual Console: $url"
        Start-Process $url
    }
    'launch' { & $Adb shell am start -n "$App/.MainActivity" }
    'run' {
        # Build, install, and cold-start. Pleos apps such as navigation can take the front right
        # after boot or a day/night switch, so bring the app back until it stays on top.
        & (Join-Path $PSScriptRoot '..\gradlew.bat') -q -p (Join-Path $PSScriptRoot '..') installDebug
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $Adb shell am force-stop $App
        foreach ($attempt in 1..5) {
            & $Adb shell am start -n "$App/.MainActivity" 2>&1 | Out-Null
            Start-Sleep -Seconds 3
            if ((& $Adb shell dumpsys activity activities) -match "topResumedActivity.*$App/") {
                Write-Host 'Running.'
                exit 0
            }
        }
        Write-Host "Another app keeps the front. Try 'launch' again."
        exit 1
    }
    'theme' {
        if ($Value -notin 'day', 'night', 'sensor') {
            Write-Host 'usage: scripts\emu.cmd theme {day|night|sensor}'
            exit 1
        }
        & $Adb shell cmd car_service day-night-mode $Value
    }
    'anim' {
        # Slow motion for checking or recording animations (e.g. 10); 1 restores normal speed.
        $scale = if ($Value) { $Value } else { '1' }
        foreach ($key in 'animator_duration_scale', 'transition_animation_scale', 'window_animation_scale') {
            & $Adb shell settings put global $key $scale
        }
        Write-Host "Animation scale: $scale"
    }
    { $_ -in 'drive', 'park' } {
        # Shifts gear through the Virtual Console: drive puts the car in D at a speed (km/h,
        # default 40), park stops it in P. In D, Pleos covers apps that are not Distraction
        # Optimized. Every request needs a fresh console session; the gear stays after it ends.
        & $Adb forward "tcp:$ConsolePort" tcp:8088 | Out-Null
        $url = "http://127.0.0.1:$ConsolePort"
        function Send($path) {
            Invoke-WebRequest -UseBasicParsing -Method Post -TimeoutSec 5 "$url/browser/session?client=emu" | Out-Null
            Invoke-WebRequest -UseBasicParsing -Method Put -TimeoutSec 5 "$url$path" | Out-Null
        }
        if ($Command -eq 'drive') {
            $kmh = if ($Value) { [int]$Value } else { 40 }
            Send '/vehicle/gear?gear=Drive'
            Send "/vehicle/controls?speed=$kmh&accelerator=30&brake=0"
            Write-Host "Gear D at $kmh km/h"
        } else {
            Send '/vehicle/controls?speed=0&accelerator=0&brake=50'
            Send '/vehicle/gear?gear=Park'
            Write-Host 'Gear P'
        }
    }
    'seat' {
        # Passenger seat through the Virtual Console: on seats someone beside the driver, off
        # empties the seat. The app then offers profiles.
        & $Adb forward "tcp:$ConsolePort" tcp:8088 | Out-Null
        $url = "http://127.0.0.1:$ConsolePort"
        $occupied = if ($Value -eq 'off') { 'false' } else { 'true' }
        Invoke-WebRequest -UseBasicParsing -Method Post -TimeoutSec 5 "$url/browser/session?client=emu" | Out-Null
        Invoke-WebRequest -UseBasicParsing -Method Put -TimeoutSec 5 "$url/vehicle/seat?kind=occupied&area=passenger&value=$occupied" | Out-Null
        Write-Host "Passenger seat occupied: $occupied"
    }
    'charge' {
        # EV charging through the Virtual Console: on starts charging, off stops it. While the
        # car charges, home offers a ten-minute round.
        & $Adb forward "tcp:$ConsolePort" tcp:8088 | Out-Null
        $url = "http://127.0.0.1:$ConsolePort"
        $on = if ($Value -eq 'off') { 'false' } else { 'true' }
        Invoke-WebRequest -UseBasicParsing -Method Post -TimeoutSec 5 "$url/browser/session?client=emu" | Out-Null
        Invoke-WebRequest -UseBasicParsing -Method Put -TimeoutSec 5 "$url/vehicle/ev-battery?field=charge_switch&value=$on" | Out-Null
        Write-Host "Charging: $on"
    }
    'navi' {
        # Debug builds only: replays a short drive's navigation events (school zone, no-parking
        # zone, speed limit, roundabout) into the running app, which finds their signs.
        & $Adb shell am broadcast --user current -n "$App/.debug.MockDriveReceiver" | Out-Null
        Write-Host 'Replayed a drive'
    }
    'shot' {
        # PR screenshot: the app window only, without the Pleos system UI.
        $out = [System.IO.Path]::GetFullPath($(if ($Value) { $Value } else { 'shot.png' }))
        $full = "$out.full.png"
        & $Adb shell screencap -p /sdcard/rulecade-shot.png
        & $Adb pull /sdcard/rulecade-shot.png $full | Out-Null
        & $Adb shell rm /sdcard/rulecade-shot.png
        $image = [System.Drawing.Bitmap]::FromFile($full)
        $crop = $image.Clone($Window, $image.PixelFormat)
        $image.Dispose()
        $crop.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        $crop.Dispose()
        Remove-Item $full
        Write-Host "Saved $out"
    }
    default {
        Write-Host 'usage: scripts\emu.cmd {start|wait|stop|status|console|launch|run|theme|anim|drive|park|seat|charge|navi|shot}'
        exit 1
    }
}

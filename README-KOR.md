# Rulecade

[English](README.md) | **한국어**

정차·충전 중에 짧은 아케이드 게임으로 교통법규를 익히는 Pleos Connect 앱입니다.

## 설정

1. Android Studio를 설치합니다. Windows는 Git for Windows도 설치하고 **Windows 하이퍼바이저 플랫폼**을 켭니다.
2. **SDK Manager → SDK Update Sites**: Pleos Connect 시스템 이미지를 추가하고 API 34용으로 설치합니다.
   - macOS (Apple Silicon): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
   - Windows / Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
3. **Device Manager**: Automotive → Pleos Connect v2.1.0, API 34, Cold boot, RAM 4GB 이상. 이름에 `pleos`가 들어간 AVD는 자동으로 잡히고, 아니면 `PLEOS_AVD`를 지정합니다.
4. Android Studio에서 프로젝트를 한 번 엽니다(`local.properties` 생성). JDK 17 이상이 없으면 명령줄 빌드용 `JAVA_HOME`을 Android Studio의 `jbr`로 지정합니다.

내장 그래픽에서 화면이 깨지면 `PLEOS_GPU=angle_indirect` 또는 `swiftshader_indirect`로 지정합니다. [설정 가이드](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)

## 실행

macOS / Linux:

```bash
scripts/emu.sh start
scripts/emu.sh wait
scripts/emu.sh run
```

Windows (PowerShell 또는 cmd):

```bat
scripts\emu.cmd start
scripts\emu.cmd wait
scripts\emu.cmd run
```

빌드 없이 심사용 APK 설치: 에뮬레이터를 켠 뒤 `adb install -r apk/Rulecade_Rulecade_release.apk`

Virtual Console: `emu console` → http://127.0.0.1:8282 (`PLEOS_CONSOLE_PORT`로 변경)

차량처럼 주간/야간 전환: `emu theme day|night`. 애니메이션 느리게: `emu anim 10`(`emu anim 1`로 복구). 가상 콘솔로 주행: `emu drive 40`이면 기어 D, 시속 40km가 되고 Pleos가 앱을 가려요(Rulecade는 주행 중 사용 앱(DO)으로 선언하지 않음). `emu park`로 P에 두면 하던 곳으로 돌아와요. 동승석: `emu seat on|off`(앱이 프로필을 권해요). 충전: `emu charge on|off`(홈이 10분 코스를 권해요). 디버그 빌드: `emu navi`로 짧은 주행의 내비 이벤트(어린이보호구역, 주정차 금지 구역, 제한속도, 회전교차로)를 재생하면 앱이 표지판을 발견해요. PR용 앱 창 스크린샷: `emu shot file.png`.

## 스택과 테스트 환경

Pleos Connect Emulator v2.1.0 (Android 14, API 34) · Kotlin · Jetpack Compose · Pleos Vehicle SDK 2.3.2

이 에뮬레이터의 2560 × 1440 px, 160 dpi 화면에서 전체 화면과 1:1 분할 화면, 주간·야간 테마로 확인했습니다.

## 구성

| 경로 | 내용 |
| --- | --- |
| `app/` | Android 앱 |
| `scripts/` | 에뮬레이터 제어(`emu.sh`, `emu.cmd`) |
| `design/` | 디자인 토큰(`tokens.json`), 앱 테마의 단일 원본 |
| `apk/` | 서명된 심사용 릴리스 APK(`Rulecade_Rulecade_release.apk`) |


---

All rights reserved. Pleos SDK와 에뮬레이터 이미지는 42dot SDK 라이선스에 따라 사용하며 이 저장소에서 재배포하지 않습니다.
Asta Sans 폰트는 SIL Open Font License 1.1에 따라 포함합니다(`app/src/main/assets/licenses/`).

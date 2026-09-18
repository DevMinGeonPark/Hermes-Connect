# Hermes-Connect

**내 컴퓨터의 Hermes를 Android에서 사용하세요.**

[English](../../README.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [日本語](README.ja.md) · **한국어** · [Português (Brasil)](README.pt-BR.md) · [Русский](README.ru.md) · [简体中文](README.zh-CN.md)

> 이 문서는 지속적으로 관리하는 한국어 요약 안내입니다. 전체 기능 설명과 기술 기준은
> [영문 README](../../README.md)와 원본문서를 따릅니다. 번역 및 검토 상태는
> [언어별 현황](../localization-status.json)에 기록합니다.

Hermes-Connect는 [Hermes-Relay](https://github.com/Codename-11/hermes-relay)를
기반으로 한국어를 추가한 Android 앱입니다. [Hermes Agent](https://github.com/NousResearch/hermes-agent)는
사용자의 컴퓨터에서 실행되고, 앱은 그 서버에 연결해 대화와 작업을 이어갑니다.
기존 언어도 그대로 선택할 수 있습니다.

## 주요 기능

- **대화와 세션:** 스트리밍 응답, 도구 실행 표시, 대화 기록, 파일 첨부를 제공합니다.
- **에이전트 관리:** 모델과 제공업체, 키, 프로필, `SOUL.md`, 스킬을 관리합니다.
- **음성:** 서버에 설정된 음성 제공업체를 통해 말하고 응답을 들을 수 있습니다.
- **표준 연결:** 대화, 관리, 세션, 기본 음성, 수신 파일은 수정하지 않은 Hermes Dashboard/Gateway에 직접 연결합니다.
- **선택적 Relay 확장:** 터미널/TUI, 알림 연동, 데스크톱 도구, 확장 음성, Relay 세션 등을 추가합니다.
- **Sideload의 기기 제어:** Relay 페어링과 필요한 권한을 허용하면 화면 읽기, 탭, 입력 등을 사용할 수 있습니다. 이 기능은 Google Play 빌드에 포함되지 않습니다.
- **Hermes-Relay CLI/UI:** 페어링한 컴퓨터의 파일, 터미널, 검색, 스크린샷 도구를 사용자 동의에 따라 제공합니다. CLI는 베타 기능입니다.

## 설치와 연결

Android 8.0 이상과 Dashboard/Gateway를 사용할 수 있는 Hermes 설치가 필요합니다.
선택 사항인 Hermes-Relay 플러그인을 설치하려면 Python 3.11 이상이 필요합니다.

1. **사용할 앱을 확인합니다.** 이 저장소의 한국어 Hermes-Connect 빌드는
   [개발 환경 안내](../../CONTRIBUTING.md#quick-start-android)에 따라 Android Studio에서
   프로젝트를 열고 빌드할 수 있습니다. 원프로젝트의
   [Google Play](https://play.google.com/store/apps/details?id=com.axiomlabs.hermesrelay)와
   [`android-v*` 릴리스](https://github.com/Codename-11/hermes-relay/releases)는
   **Hermes-Relay 배포판**입니다. 해당 링크가 이 저장소의 한국어 Hermes-Connect 빌드를
   배포한다는 뜻은 아닙니다.
2. **Hermes를 실행하는 컴퓨터에서 대시보드를 시작합니다.**

   ```bash
   hermes dashboard
   ```

3. **Android 앱에서 연결합니다.** LAN에서 Hermes를 찾거나 대시보드 주소를 입력하고,
   안내에 따라 로그인합니다. 표준 연결에는 Relay 플러그인이나 별도의 API 서버·API 키가
   필요하지 않습니다. 휴대전화에서 서버에 접속할 수 있도록 신뢰할 수 있는 LAN,
   Tailscale 또는 HTTPS 연결을 구성하세요.
4. **추가 기능이 필요하면 Relay를 설치하고 페어링합니다.**

   ```bash
   hermes plugins install Codename-11/hermes-relay/plugin --enable
   hermes relay doctor
   hermes relay start --no-ssl
   ```

   `--no-ssl`은 신뢰할 수 있는 LAN 또는 VPN 안에서만 사용하세요.
   Dashboard/Gateway를 새로고침하거나 재시작한 뒤 대시보드의
   **Relay → Pair new device**를 열고, Android 앱의
   **설정 → Gateway → 접근 → Relay 페어링**에서 일회용 QR을 스캔합니다.

## 한국어 설정

앱에서 **Settings → Appearance → Language → 한국어**를 선택하세요.
한국어 화면에서는 **설정 → 화면 설정 → 언어 → 한국어**로 표시됩니다.
Android에서 앱별 언어 설정을 제공하는 경우 해당 설정에서도 변경할 수 있습니다.

언어 설정은 앱의 버튼, 메뉴, 안내 문구에 적용됩니다. 명령어와 프로토콜 식별자,
모델·제공업체 이름은 원래 표기를 유지하며, 서버나 에이전트가 보내는 답변과 콘텐츠를
자동으로 번역하지 않습니다. 다른 언어도 계속 사용할 수 있습니다.

## 주의와 보안

- **외부 연결:** 신뢰할 수 없는 네트워크에 암호화되지 않은 Dashboard나 Relay를 노출하지 마세요. 외부 접속은 [원격 접속 안내](https://hermes-relay.dev/docs/guide/remote-access/)에 따라 설정하세요.
- **권한과 승인:** 알림 접근과 기기 제어는 필요한 기능에만 허용하세요. 기기 제어에는 앱별 차단, 위험 작업 확인, 유휴 시간 제한, 활동 기록 등의 보호 장치가 적용됩니다. 승인할 때는 실제 요청 내용을 확인하세요.
- **모델과 음성 서비스:** 사용 데이터의 처리와 서비스 요금은 연결한 Hermes 서버 및 제공업체 설정을 따릅니다. 연결 전에 해당 설정을 확인하세요.
- **설치 파일:** 원프로젝트 Sideload APK의 무결성·서명 확인 방법과 빌드별 기능 차이는 [설치 안내](https://hermes-relay.dev/docs/guide/getting-started.html#sideload-apk)와 [배포 종류](https://hermes-relay.dev/docs/guide/release-tracks)를 참고하세요.

## 원본문서

아래 문서는 원프로젝트의 영문 안내입니다. 빠르게 바뀌는 API, 경로, 설정, 아키텍처의
상세 내용은 원본문서에서 확인하세요.

[빠른 시작](https://hermes-relay.dev/docs/guide/quick-start) ·
[설치](https://hermes-relay.dev/docs/guide/getting-started) ·
[문제 해결](https://hermes-relay.dev/docs/guide/troubleshooting) ·
[전체 문서](https://hermes-relay.dev/docs/) ·
[기여 안내](../../CONTRIBUTING.md)

## 라이선스

[MIT 라이선스](../../LICENSE)를 따릅니다. 사용·수정·배포 시 원본 저작권 고지와
라이선스 고지를 유지해야 하며, 소프트웨어는 보증 없이 제공됩니다. 정확한 조건은
라이선스 원문을 확인하세요.

# Native Android UI application

The first Android implementation carries the approved icon, profile, neutral
surface, and voice direction into production Jetpack Compose components.
The [review gallery](uiux-preview/native.html) distinguishes emulator captures
from isolated component renders.

## Implemented behavior

- Chat, Manage, and Gateways are real bottom-navigation destinations. Returning
  to Chat does not restore a secondary destination over it. An unsent draft
  survives repeated tab switches.
- The Chat header opens the existing profile switcher directly. Profiles retain
  their server selection keys, custom portraits, explicit colors, lock policy,
  and server-default behavior. Work, Personal, and Research get stable symbols;
  other profiles retain a monogram fallback.
- The primary Chat, Settings, Manage, Gateways, session, attachment, and voice
  surfaces use pinned [Ionicons](../licenses/ionicons/README.md) geometry. The
  82 semantic symbols come from 74 original MIT-licensed SVGs. Android packages
  the copyright/license notice and makes no runtime asset requests.
- The default theme uses neutral light/dark backgrounds and blue actions. New
  installs use the system font; explicitly saved fonts, custom themes, and
  custom avatars remain available. Message actions and supervised restrictions
  retain their existing behavior.
- Javis Voice Focus shows a circular signal driven by the current voice state
  and amplitude. At widths of at least 720dp and heights of at least 480dp,
  voice controls and the transcript sit alongside each other. This includes
  portrait foldable windows. State text remains accessible independently of
  the decorative signal.

The prototype's sample task/file backends are not introduced into Android.
Manage and Gateways continue to use their existing upstream contracts. This
change does not alter credentials, providers, endpoints, approval decisions,
  recording permission, or speech-engine selection.

## Verification and capture provenance

`ConnectUiScreenshotTest` renders the production voice, navigation, and profile
components with Korean resources, light/dark themes, 320dp/200% text, and a
720×840dp portrait window. Microphone and profile controls invoke local test
callbacks only. `ConnectNavigationTest` exercises the actual navigation helper
through Chat → Manage → Gateways → Chat twice while retaining a draft.
`MessageBubbleInteractionTest` checks copy, quote, long-press, reaction, and
accessible message actions. Delivery-label tests require at least 4.5:1 contrast.

Reproduce the local gate when hosted verification is unavailable:

```sh
python3 scripts/generate-ionicons.py --check
python3 scripts/android-prepush.py --both-flavors
./gradlew :app:assembleSideloadDebug :app:assembleGooglePlayDebug
```

The pre-push script applies Gradle test filters separately to each flavor.
Previously filters only followed the last task and unintentionally ran the
entire sideload suite. That broader run exposed five unrelated failures, all
reproduced on the unchanged base; details live in
[the project follow-up list](../docs/project/TODO.md#repair-five-existing-android-unit-test-failures).

Chat screenshots use the application's existing offline demo on an API 36
arm64 Pixel Fold AVD with 390×840dp and 720×840dp window overrides. Airplane mode
is enabled, Wi-Fi is disabled, and no server account is configured. The review
identity strip reserves its own space above the demo banner. Light/dark mode
changes, Chat → Manage → Gateways → Chat, and narrow/wide resizing are exercised.
Voice/profile images are Roborazzi renders of actual Compose
components with sample state, not claims of live voice or harness validation.
Physical Fold8 hinge, posture, and lifecycle certification remains in
[the device follow-up](../docs/project/TODO.md#complete-native-ui-device-certification).

The review APK uses the existing isolated `candidate` build type with an
`.candidate` application ID suffix. Generated APKs are ignored and served only
from the private review server; they are not GitHub releases or production tags.

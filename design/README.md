# Hermes-Connect UI study

Five interactive Korean design directions for reviewing the Android companion's
information hierarchy. Open `uiux-preview/index.html` through a local static
HTTP server:

```sh
python3 -m http.server 4321 --bind 127.0.0.1 --directory design/uiux-preview
```

The preview has no dependencies, API calls, model calls, microphone capture,
file upload, or live harness connection. Sample approval decisions only update
browser memory. The design selection is stored in this browser's local storage.
Direct `app.html?concept=studio` links use the available viewport width.

## Review findings

The current [chat](../assets/screenshots/02_chat.png),
[management](../assets/screenshots/06_manage.png), and
[connection](../assets/screenshots/07_connections.png) views give substantial
space to connection details, repeated capability cards, and model controls.
The study explores how to prioritize the current task and retain access to
those details through explicit controls.

| Direction | Starting view | Strength | Tradeoff |
| --- | --- | --- | --- |
| Focus | Conversation with compact approval entry | Familiar conversation flow | Task overview requires a tab change |
| Mission | Task status and pending approvals | Remote supervision of several tasks | Starting a simple chat takes another step |
| Studio | Conversation and result tabs or panes | Inspect changes alongside the conversation | Higher information density |
| Paper | Reading-focused work notes | Long responses and decision records | Less emphasis on simultaneous operations |
| Pulse | Voice and task controls | Short interactions near the bottom of the screen | Detailed review opens a separate view |

A useful starting combination is Focus's compact conversation hierarchy with
Studio's result pane at larger widths. This is a proposal, not an approved
Android redesign.

## Responsive review

- At narrow widths, Studio switches between conversation and results.
- At 720 CSS pixels, conversation and results/context can appear together.
- At 1080 CSS pixels, a session rail also appears.
- Resizing preserves the current draft and sample approval state.
- Font controls exercise 100%, 150%, and 200% text sizes.
- The gallery can simulate pending approval, connected, and disconnected states.
- Offline sending is disabled; reconnecting preserves the draft.
- Keyboard focus, labeled icon buttons, native dialogs, and reduced motion are supported.

These browser breakpoints demonstrate content decisions. They do not establish
Galaxy Z Fold8 hardware dimensions, native window size classes, hinge handling,
TalkBack behavior, or physical-device certification. Android implementation
remains Jetpack Compose and must preserve the Dashboard/Gateway transport,
authentication, session, and optional Relay permission contracts.

The adaptive layout direction follows Android's guidance on
[foldable layouts and state continuity](https://developer.android.com/develop/adaptive-apps/guides/foldables/learn-about-foldables).

## Serving for review

Serve only `uiux-preview`, which contains public sample data and static assets.
A private Tailscale Serve reverse proxy can expose the loopback HTTP server on
an unused HTTPS port. Keep existing Serve endpoints intact. Machine-specific
hostnames, service configuration, logs, and screenshots are local deployment
artifacts and are not part of this directory.

## Captured views

Chromium browser captures at a 390 × 860 CSS-pixel viewport:

| Focus | Mission | Studio | Paper | Pulse |
| --- | --- | --- | --- | --- |
| [![Focus](screenshots/focus.jpg)](screenshots/focus.jpg) | [![Mission](screenshots/mission.jpg)](screenshots/mission.jpg) | [![Studio](screenshots/studio.jpg)](screenshots/studio.jpg) | [![Paper](screenshots/paper.jpg)](screenshots/paper.jpg) | [![Pulse](screenshots/pulse.jpg)](screenshots/pulse.jpg) |

[Studio with two panes at 900 × 860 CSS pixels](screenshots/studio-expanded.jpg).
These are browser prototype captures, not Android or physical-device evidence.

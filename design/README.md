# Hermes-Connect UI study

Five interactive Korean design proposals for the Android companion. Revision 2
uses familiar Apple application patterns: content-first navigation, system
text, grouped lists, thin separators, and restrained semantic color. It replaces
the previous promotional headings, decorative cards, dark accent themes, and
voice orb.

Open the gallery through a local static HTTP server:

```sh
python3 -m http.server 4321 --bind 127.0.0.1 --directory design/uiux-preview
```

The preview has no dependencies, API calls, model calls, microphone capture,
file upload, or live harness connection. Sample approval decisions only update
browser memory. The design selection is stored in this browser's local storage.
Revision 2 uses a separate selection key so an earlier choice is not presented
as approval of the redesign. Direct `app.html?concept=studio` links use the
available viewport width.

## Design directions

| Direction | Reference structure | Main behavior | Tradeoff |
| --- | --- | --- | --- |
| Messages (`focus`) | Messages | Conversation, attachments, compact approval row | Task overview requires a tab change |
| Task list (`mission`) | Reminders | Status filters, pending work, completed rows | Conversation requires selecting a task |
| Files (`studio`) | Files and split views | Conversation alongside changed files, notes, and activity | Narrow screens switch panels |
| Notes (`paper`) | Notes | Document headings, checklist, related files | Reading results takes priority over live operations |
| Voice memos (`pulse`) | Voice Memos | Recording list and bottom recording control | Detailed approval opens a review sheet |

Messages plus Files is the proposed narrow/wide combination. This is a design
proposal, not an approved Android redesign.

The current [chat](../assets/screenshots/02_chat.png),
[management](../assets/screenshots/06_manage.png), and
[connection](../assets/screenshots/07_connections.png) screens give substantial
space to connection details, capability cards, and model controls. The revision
puts the current conversation, file, or task first. Connection and model details
remain available through explicit controls.

## Visual and interaction rules

- White and system gray surfaces; blue for navigation and actions.
- Warm accent for Notes; red for recording controls and pending-count badges.
- System font stack without bundled fonts or Apple-provided asset files.
- Short, factual labels instead of promotional copy or decorative metrics.
- Back navigation, bottom tabs, grouped rows, and native browser review sheets.
- Approval stays explicit and scoped to the sample change; no automatic grants.
- Standard connection and optional Relay details retain their existing meaning.

References: Apple's [layout](https://developer.apple.com/design/human-interface-guidelines/layout),
[typography](https://developer.apple.com/design/human-interface-guidelines/typography),
and [materials](https://developer.apple.com/design/human-interface-guidelines/materials)
guidance. The prototype interprets these principles with original HTML/CSS and
outline icons; it is not a native Apple application.

## Responsive review

- At narrow widths, Files switches between conversation and results.
- At 720 CSS pixels, conversation and results/context appear together.
- At 1080 CSS pixels, a session list also appears.
- Resizing and font changes retain the draft and selected result panel.
- Font controls exercise 100%, 150%, and 200% text sizes.
- The gallery simulates pending approval, connected, and disconnected states.
- Offline sending is disabled; reconnecting preserves the draft.
- Task filters, file inspection, session selection, approval review, and recording
  controls operate only on sample state.
- Labeled controls, keyboard focus, native dialogs, and reduced motion are supported.

These browser breakpoints do not establish Galaxy Z Fold8 hardware dimensions,
native window size classes, hinge handling, TalkBack behavior, or physical-device
certification. Android implementation remains Jetpack Compose and must preserve
the Dashboard/Gateway transport, authentication, session, and optional Relay
permission contracts. The adaptive direction follows Android's
[foldable layout and state continuity guidance](https://developer.android.com/develop/adaptive-apps/guides/foldables/learn-about-foldables).

## Serving for review

Serve only `uiux-preview`, which contains public sample data and static assets.
A private Tailscale Serve reverse proxy can expose the loopback HTTP server on
an unused HTTPS port. Keep existing Serve endpoints intact. Machine-specific
hostnames, service configuration, logs, and temporary screenshots are local
deployment artifacts and are not part of this directory.

## Captured views

Chromium browser captures at a 390 × 860 CSS-pixel viewport:

| Messages | Task list | Files | Notes | Voice memos |
| --- | --- | --- | --- | --- |
| [![Messages](screenshots/focus.jpg)](screenshots/focus.jpg) | [![Task list](screenshots/mission.jpg)](screenshots/mission.jpg) | [![Files](screenshots/studio.jpg)](screenshots/studio.jpg) | [![Notes](screenshots/paper.jpg)](screenshots/paper.jpg) | [![Voice memos](screenshots/pulse.jpg)](screenshots/pulse.jpg) |

[Files with two panes at 900 × 860 CSS pixels](screenshots/studio-expanded.jpg).
These are browser prototype captures, not Android or physical-device evidence.

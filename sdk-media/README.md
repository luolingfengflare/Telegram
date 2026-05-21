# :sdk-media

Media UI library for the Lingyun fork of Telegram for Android. Code is
copied from `:TMessagesProj` into this module, preserving original
`org.telegram.*` package names. The four files under
`org/telegram/messenger/` and `org/telegram/ui/ActionBar/Theme.java` are
minimal stubs we wrote to satisfy the copied drawables' imports without
dragging in the rest of the Telegram client.

## Phase 1 contents

| File | Origin |
|---|---|
| `org/telegram/ui/Components/AudioVisualizerDrawable.java` | verbatim copy from TMessagesProj |
| `org/telegram/ui/Components/CircleBezierDrawable.java` | verbatim copy from TMessagesProj |
| `org/telegram/messenger/AndroidUtilities.java` | stub (dp, runOnUIThread, recycleBitmaps) |
| `org/telegram/messenger/LiteMode.java` | stub (FLAG_CHAT_BACKGROUND, isEnabled always true) |
| `org/telegram/messenger/SharedConfig.java` | stub (empty) |
| `org/telegram/ui/ActionBar/Theme.java` | stub (ResourcesProvider, getColor) |

## Rules

- This module MUST NOT depend on `:TMessagesProj` or anything inside it.
- When adding new Telegram-side code, prefer verbatim copy; stubs are for
  classes that pull in massive dependency trees (controllers, TLRPC, etc.).
- Public API surface is whatever the copied/stubbed classes expose. There
  is no separate SDK-facing API in Phase 1.

## Build

```
./gradlew :sdk-media:assembleDebug
./gradlew :sdk-media:testDebugUnitTest
```

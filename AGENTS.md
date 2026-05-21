# AGENTS.md

## Cursor Cloud specific instructions

### Project overview

ChatAir is a native Android app (Java 11) for ChatGPT, Gemini, Claude, and DeepSeek. It is a fork of Telegram for Android with a custom LLM client integration. There is no backend—it is a client-only app that connects to external LLM APIs.

### Build environment requirements

- **JDK 11** (`/usr/lib/jvm/java-11-openjdk-amd64`) — set `JAVA_HOME` before running Gradle.
- **Android SDK** (`/opt/android-sdk`) — includes platforms;android-34, build-tools;34.0.0, ndk;21.4.7075529, cmake;3.10.2.4988404.
- **`local.properties`** must contain `sdk.dir=/opt/android-sdk` (gitignored, created by the update script).

### Key Gradle commands

| Task | Command |
|---|---|
| Debug APK (all ABIs) | `./gradlew :TMessagesProj_App:assembleAfatDebug` |
| Lint | `./gradlew :TMessagesProj_App:lintAfatDebug` |
| Clean | `./gradlew clean` |

### Important gotchas

- The first build compiles native C/C++ code (FFmpeg, BoringSSL, WebRTC, etc.) for 4 ABIs (armeabi-v7a, arm64-v8a, x86, x86_64). Expect ~15-20 min on a cloud VM. Subsequent incremental builds are much faster.
- The `variantFilter` in `TMessagesProj_App/build.gradle` only keeps the `afat` flavor for non-release build types. Always use `afatDebug` for development builds.
- The SDK directory at `/opt/android-sdk` must be writable by the build user (the update script handles ownership).
- Kotlin metadata version mismatch warnings during lint are from transitive dependencies and do not affect the build.
- There are **no unit or instrumented tests** in this codebase. Validation is done through build + lint + APK inspection.
- This is a client-only Android app. There are no backend services, databases, or Docker dependencies to start.

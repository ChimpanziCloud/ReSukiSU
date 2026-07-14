# ReSukiSU + GhostLock

Fork of [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU) with integrated GhostLock auto-jailbreak for OnePlus Ace 6T (locked bootloader).

## What's Different

This fork adds automatic jailbreak on boot for a specific device — no bootloader unlock, no boot image modification.

### Added Components

| File | Description |
|------|-------------|
| `GhostlockService.java` | Foreground service that runs the exploit on boot |
| `GhostlockLog.kt` | Real-time exploit log screen (M3 Expressive style) |
| `BootCompletedReceiver.java` | Modified: triggers GhostLock when KSU not loaded, skips on soft reboot |
| `HomePage.kt` | Modified: GhostLock button (visible when KSU not installed on GKI kernel) |
| `libghostlock.so` | Exploit binary (OnePlus Ace 6T specific) |
| `libksud.so` | ksud with embedded kernelsu.ko |

### Boot Flow

```
BOOT_COMPLETED
  ├─ KSU loaded or su available → skip (normal boot / soft reboot)
  └─ Neither → GhostlockService
       → setsid exploit --bootstrap
       → Write 1 (SELinux off) → mini-adb TCP 5555
       → adb shell exploit (Write 2 → root)
       → ksud late-load → KernelSU
       → su load_policy → network fix
       → dynamic manager registration
```

### One-Time Setup

```bash
adb tcpip 5555
adb push ~/.android/adbkey /data/local/tmp/a/adbkey
```

After first successful jailbreak, TCP 5555 persists via `resetprop` — subsequent boots are fully automatic.

## Target Device

| | |
|---|---|
| Device | OnePlus Ace 6T (SM8845) |
| OS | Android 16 |
| Kernel | 6.12.38-android16-5 |
| Bootloader | Locked |

This is a **device-specific build**. The exploit binary and kernel offsets only work on this exact kernel version.

## Build

```bash
# Requires: Android SDK (API 36+), NDK 29, JDK 21
cd manager
./gradlew assembleRelease
```

### Important: ksud is NOT included when building from source

The `libksud.so` binary (which contains embedded `kernelsu.ko` modules) is gitignored upstream and must be added manually before building:

1. Download `ksud-aarch64-linux-android.zip` from [ReSukiSU CI releases](https://github.com/cctv18/ReSukiSU_CI/releases)
2. Extract and place at `manager/app/src/main/jniLibs/arm64-v8a/libksud.so`
3. Then build

Without this step, the APK will not contain ksud and KernelSU cannot be loaded. The exploit will still achieve root, but `su` will not persist.

> The pre-built APK in this fork's releases (if available) already includes both `libksud.so` and `libghostlock.so`.

## Exploit Source

See [ghostlock-ace6t](https://github.com/JoinChang/ghostlock-ace6t) for the exploit source code and technical details.

## Upstream

Based on [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU) — a KernelSU-based root solution for Android.

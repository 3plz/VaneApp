# WickedApp

WickedApp is a lightweight, modern launcher for Minecraft: Java Edition built with Kotlin and Compose Multiplatform by **3plz**. It is designed to provide a responsive, native desktop experience with a clean Material Design interface, fast startup times, and isolated game instance management.

---

## Features

- **Native Desktop Interface**: Built with Compose Multiplatform and hardware-accelerated Skia rendering for low resource usage and smooth framerates.
- **Secure Microsoft Authentication**: Official OAuth 2.0 / Xbox Live authentication flow with local loopback verification. User credentials and passwords are never handled or stored by the application.
- **Instance Isolation**: Separate directories for each installation, configuration, and save data to prevent mod or version conflicts.
- **Content Management**: Built-in workflows for installing and organizing mods, resource packs, and shader packs.
- **Custom JVM Configuration**: Fine-grained control over Java runtimes, garbage collection flags, and memory allocation per instance.
- **Internationalization**: Multi-language support with runtime switching.

---

## Tech Stack

- **Language**: Kotlin 2.1+
- **UI Framework**: Compose Multiplatform Desktop (Skiko)
- **Theming**: Material Design 3 / MaterialKolor
- **Asynchronous Engine**: Kotlin Coroutines & Flow
- **Serialization**: `kotlinx.serialization`
- **Build Tool**: Gradle (Kotlin DSL) with Version Catalogs

---

## Prerequisites

- **JDK**: Java 21 or higher
- **OS**: Windows 10/11, macOS 12+, or modern Linux distributions

---

## Building from Source

Clone the repository:

```bash
git clone https://github.com/3plz/WickedApp.git
cd WickedApp
```

Run in development mode:

```bash
# Windows
.\gradlew.bat run

# Linux / macOS
./gradlew run
```

Package installer / distribution:

```bash
# Windows (creates .msi and .exe packages in build/compose/binaries/main)
.\gradlew.bat packageDistributionForCurrentOS

# Linux / macOS
./gradlew packageDistributionForCurrentOS
```

---

## Architecture Overview

The codebase is structured around clean modular boundaries:

```text
src/main/kotlin/app/
├── domain/            # Core business models, session entities, and auth contracts
│   ├── auth/          # Microsoft OAuth, Xbox Live, and Mojang service clients
│   └── model/         # Domain state definitions (AuthState, UserSession)
├── ui/                # Reactive Compose UI components
│   ├── components/    # Reusable widgets, custom title bars, controls
│   └── login/         # Authentication window and layout elements
├── i18n/              # Localization tables and language providers
├── theme/             # Material 3 palettes, typography, and styling
└── di/                # Lightweight dependency injection container
```

---

## Security & Privacy

WickedApp authenticates users directly through Microsoft's Identity Platform via OAuth 2.0. The application:
- Never requests, collects, or stores Microsoft passwords.
- Only retains standard OAuth refresh tokens locally on the user's machine within secure OS-level storage.
- Communicates exclusively with official endpoints (`login.microsoftonline.com`, `user.auth.xboxlive.com`, and `api.minecraftservices.com`).

---

## Author

Developed and maintained by **3plz** ([GitHub](https://github.com/3plz)).

---

## License

Copyright (C) 2026 3plz.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

See [LICENSE](LICENSE) for the full license text.

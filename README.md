# SortIt

SortIt is a one-click Windows tool that organizes photos and videos into date-based folders. It includes its own Java runtime, so Java does not need to be installed separately.

## What it does

- Selects files directly in a source folder using a name pattern such as `*.*` or `PXL_*.*`.
- Determines dates from EXIF/metadata, a supported file-name pattern, or file creation time.
- Creates destination folders from `YYYY`, `YY`, `MM`, and `DD` tokens.
- Copies files, copies only files not already present, moves files, or moves files while keeping an archive copy.
- Keeps result logs for 24 hours.
- Supports English and Russian.

## Install and update

Download `SortIt-Setup-<version>-x64.exe` from the GitHub release and run it. The installer is per-user and does not require administrator rights. It installs the application under:

```
%LOCALAPPDATA%\Programs\PashaApps\SortIt
```

Use the same Setup EXE for updates. The installer preserves the installation location and shortcut choice. Its optional desktop shortcut is disabled by default; the Start menu shortcut is always created.

The first standard installer update migrates settings from the previous SortIt MSI installation before removing that MSI.

## User data

Uninstalling SortIt removes only program files and shortcuts. Your settings and logs remain available at:

```
Settings: %APPDATA%\PashaApps\SortIt\sortit.json
Logs:     %LOCALAPPDATA%\PashaApps\SortIt
```

## Build a Windows release

Requirements: Windows, JDK 21, and Inno Setup 6. The JDK must contain `jpackage`.

```powershell
.\gradlew.bat clean buildWindowsInstaller
```

The command runs tests, creates an embedded-runtime application image, builds the installer, and writes exactly these release assets to `build/release/<version>/`:

- `SortIt-Setup-<version>-x64.exe`
- `SortIt-Setup-<version>-x64.exe.sha256`
- `appfleet-manifest.json`

The version, stable AppFleet identity, publisher, and repository URL are defined once in `gradle.properties`.

## Usage

1. Choose the source folder.
2. Open Settings and choose the destination, date source, operation mode, patterns, and result-log preference.
3. Click `SortIt`.

The source folder is not scanned recursively.

## License

MIT

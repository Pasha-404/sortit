# Windows Installer Smoke Test

Run these checks on a disposable Windows user profile before publishing the first AppFleet installer release.

## Fresh install

1. Run `SortIt-Setup-<version>-x64.exe`.
2. Confirm the default path is `%LOCALAPPDATA%\Programs\PashaApps\SortIt` and the desktop shortcut is optional.
3. Confirm the Start menu shortcut starts SortIt without a separate Java installation.
4. Check `HKCU\Software\PashaApps\f238fccc-4f33-429d-b476-7cd286adb376` contains the expected version, installation path, executable, and process name.

## Update from MSI

1. Install the published v1.4.0 MSI and save a non-default SortIt setting.
2. Run the standard Setup EXE.
3. Confirm the old MSI no longer appears in Installed apps.
4. Confirm the saved setting is present in `%APPDATA%\PashaApps\SortIt\sortit.json` and SortIt starts normally.

## Silent install and uninstall

1. Run `SortIt-Setup-<version>-x64.exe /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS`.
2. Uninstall SortIt and confirm `%APPDATA%\PashaApps\SortIt` and `%LOCALAPPDATA%\PashaApps\SortIt` remain untouched.

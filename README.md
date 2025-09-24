# SortIt — simple photo/video sorter for Windows

**One-click** tool to organize photos and videos into date-based folders.  
No install. Settings in `sortit.json`. Works on Windows (embedded runtime in the app image).

## ✨ Features
- **Source** folder + **pattern** (`*.*`, `IMG_*.*`, `PXL_*.*`)
- Date source: **EXIF/metadata**, **file name**, or **file creation time**
- Destination folders by template: `YYYY`, `YY`, `MM`, `DD` with separators `- _ .`
- **Copy** or **Move**
- No registry; settings in `sortit.json` next to the exe
- Language switcher (RU/EN)

> Note for videos: MP4/MOV usually contain creation time in metadata.  
> **M2TS/AVCHD** often don’t — use “file name” or “creation time”.

## ⬇ Download
Grab the latest ZIP from **[Releases](../../releases)**:
- Unzip `SortIt-1.0.0.zip`
- Run `SortIt/SortIt.exe`

No Java required – app image includes an embedded runtime.

## How to use
1. Select **Source** and **Destination** folders.
2. Set a **file name pattern** (e.g., `PXL_*.*`) and a **folder template** (e.g., `YYYYMMDD`).
3. Choose date source: **EXIF/metadata**, **file name**, or **creation time**.
4. Click **SortIt**.  
   Optional: show result log after processing.

## Settings (`sortit.json`)
This file is created next to `SortIt.exe` on first run. Example:
```json
{
  "lang": "en",
  "sourceDir": "",
  "filenameTemplate": "*.*",
  "dateSource": "METADATA",  // METADATA | FILENAME | CREATED
  "copyMode": true,
  "destDir": "",
  "destTemplate": "YYYYMMDD",
  "showResults": false,
  "windowX": 120,
  "windowY": 120
}

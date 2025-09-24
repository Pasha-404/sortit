# SortIt — simple photo/video sorter for Windows

One-click tool to organize photos/videos into date-based folders.  
No install. Settings in `sortit.json`. Works on Windows (embedded runtime included).

## ✨ Features
- Source folder + file name pattern (`*.*`, `IMG_*.*`, `PXL_*.*`)
- Date source: **EXIF/metadata**, **file name**, or **file creation time**
- Destination folders by template: `YYYY`, `YY`, `MM`, `DD` (separators allowed: `- _ .`)
- Copy or Move
- No registry; settings saved next to the EXE
- Language switcher (RU/EN)

## ⬇ Download
1. Go to **Releases** and download `SortIt-1.0.0.zip`.  
2. Unzip → run `SortIt/SortIt.exe`.  
Java is **not** required (runtime included).

## How to use
1. Choose **Source** and **Destination** folders.
2. Set **File name pattern** (e.g., `PXL_*.*`) and **Folder template** (e.g., `YYYYMMDD`).
3. Pick date source: **EXIF/metadata**, **file name**, or **creation time**.
4. Click **▶ SortIt**. Optionally show the result log after processing.

## Settings (`sortit.json`)
Created on first run next to `SortIt.exe`. Example:

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

## Known limitations
- **AVCHD/M2TS** often lack embedded creation time → use “file name” or “creation time”.
- Windows SmartScreen may warn about unknown publisher (unsigned open-source build). Click **More info → Run anyway**.

## Troubleshooting
- **Nothing found** → check the pattern (try `*.*`) and make sure files are directly in the source folder (no recursion).
- **No metadata found** → switch date source to **file name** or **creation time**.
- **Conflicting names** → if the target already contains a file with the same name, the operation may fail; check the log.

## Roadmap
- Optional AVCHD (.CPI) date reading
- More built-in languages
- Conflict handling options (rename/skip)
- Installer build (EXE/MSI) as an alternative download

## Feedback
Open an **Issue** for bugs/ideas.  
Screenshots and sample files are welcome!

## License
MIT

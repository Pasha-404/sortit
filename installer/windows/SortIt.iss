#ifndef AppVersion
  #error AppVersion must be supplied by Gradle.
#endif
#ifndef AppName
  #error AppName must be supplied by Gradle.
#endif
#ifndef TechnicalName
  #error TechnicalName must be supplied by Gradle.
#endif
#ifndef AppId
  #error AppId must be supplied by Gradle.
#endif
#ifndef Publisher
  #error Publisher must be supplied by Gradle.
#endif
#ifndef RepositoryUrl
  #error RepositoryUrl must be supplied by Gradle.
#endif
#ifndef SourceAppImage
  #error SourceAppImage must be supplied by Gradle.
#endif
#ifndef OutputDir
  #error OutputDir must be supplied by Gradle.
#endif

; These are the exact ProductCodes of every published jpackage/WiX MSI through v1.4.0.
; Do not replace this allow-list with a name-based search: it must never uninstall another product.

[Setup]
AppId={{{#AppId}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#Publisher}
AppPublisherURL={#RepositoryUrl}
UninstallDisplayName={#AppName}
UninstallDisplayIcon={app}\{#TechnicalName}.exe
DefaultDirName={localappdata}\Programs\PashaApps\{#TechnicalName}
DefaultGroupName={#AppName}
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
UsePreviousAppDir=yes
UsePreviousGroup=yes
UsePreviousTasks=yes
CloseApplications=yes
RestartApplications=no
Compression=lzma2
SolidCompression=yes
OutputDir={#OutputDir}
OutputBaseFilename={#TechnicalName}-Setup-{#AppVersion}-x64
WizardStyle=modern

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked

[Files]
Source: "{#SourceAppImage}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[InstallDelete]
Type: files; Name: "{app}\{#TechnicalName}.exe"
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"
Type: filesandordirs; Name: "{app}\icons"

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#TechnicalName}.exe"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#TechnicalName}.exe"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "SchemaVersion"; ValueData: "1"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "AppId"; ValueData: "{#AppId}"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "Name"; ValueData: "{#AppName}"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "TechnicalName"; ValueData: "{#TechnicalName}"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "Version"; ValueData: "{#AppVersion}"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "InstallLocation"; ValueData: "{app}"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "Executable"; ValueData: "{app}\{#TechnicalName}.exe"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "ProcessName"; ValueData: "{#TechnicalName}.exe"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "RepositoryUrl"; ValueData: "{#RepositoryUrl}"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "InstallerType"; ValueData: "inno"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "InstalledBy"; ValueData: "installer"
Root: HKCU; Subkey: "Software\PashaApps\{#AppId}"; ValueType: string; ValueName: "Publisher"; ValueData: "{#Publisher}"

[Code]
const
  ERROR_SUCCESS = 0;
  ERROR_SUCCESS_REBOOT_REQUIRED = 3010;
  ERROR_UNKNOWN_PRODUCT = 1605;

function LegacyMsiProductCode(const Index: Integer): String;
begin
  case Index of
    0: Result := '{CC35F5C4-14E0-3A88-BD89-1E78FC339B12}';
    1: Result := '{2132FB8F-E1DF-314C-9F58-CAE6E3E13DB7}';
    2: Result := '{58044431-45A5-34E3-ACED-E2EE2432F82C}';
    3: Result := '{E09631DE-4952-3464-ABD9-D03F342DE3C3}';
    4: Result := '{9E24BC8A-C9BC-3D62-B420-C3AD31443E53}';
    5: Result := '{9728B74F-E785-3D72-8E99-EB3FB5809BC2}';
    6: Result := '{7ADDEF19-402A-3C24-A59D-9D3727A2320D}';
    7: Result := '{333D8DE8-0870-3982-8C49-377568D3BC9F}';
    8: Result := '{970C9171-C164-3824-8C46-4BF136BC2250}';
  else
    Result := '';
  end;
end;

function LegacyUninstallKey(const ProductCode: String): String;
begin
  Result := 'Software\Microsoft\Windows\CurrentVersion\Uninstall\' + ProductCode;
end;

function IsLegacyMsiProductInstalled(const ProductCode: String): Boolean;
var
  Key: String;
begin
  Key := LegacyUninstallKey(ProductCode);
  Result := RegKeyExists(HKCU, Key) or RegKeyExists(HKCU32, Key) or
    RegKeyExists(HKLM, Key) or RegKeyExists(HKLM32, Key);
end;

function FindLegacyMsiProductCode: String;
var
  Index: Integer;
  ProductCode: String;
begin
  for Index := 0 to 8 do begin
    ProductCode := LegacyMsiProductCode(Index);
    if IsLegacyMsiProductInstalled(ProductCode) then begin
      Result := ProductCode;
      Exit;
    end;
  end;
  Result := '';
end;

function ReadLegacyInstallLocation(const ProductCode: String): String;
var
  Key: String;
begin
  Key := LegacyUninstallKey(ProductCode);
  if RegQueryStringValue(HKCU, Key, 'InstallLocation', Result) then
    Exit;
  if RegQueryStringValue(HKCU32, Key, 'InstallLocation', Result) then
    Exit;
  if RegQueryStringValue(HKLM, Key, 'InstallLocation', Result) then
    Exit;
  if RegQueryStringValue(HKLM32, Key, 'InstallLocation', Result) then
    Exit;
  Result := '';
end;

function MigrateLegacySettings(const LegacyInstallLocation: String): Boolean;
var
  LegacyConfig: String;
  NewConfigDirectory: String;
  NewConfig: String;
begin
  LegacyConfig := AddBackslash(LegacyInstallLocation) + 'sortit.json';
  NewConfigDirectory := ExpandConstant('{userappdata}\PashaApps\{#TechnicalName}');
  NewConfig := AddBackslash(NewConfigDirectory) + 'sortit.json';

  if not FileExists(LegacyConfig) or FileExists(NewConfig) then begin
    Result := True;
    Exit;
  end;

  Result := ForceDirectories(NewConfigDirectory) and CopyFile(LegacyConfig, NewConfig, False);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ProductCode: String;
  LegacyInstallLocation: String;
  ExitCode: Integer;
begin
  Result := '';
  ProductCode := FindLegacyMsiProductCode;
  if ProductCode = '' then
    Exit;

  LegacyInstallLocation := ReadLegacyInstallLocation(ProductCode);
  if LegacyInstallLocation = '' then begin
    Result := 'An older SortIt MSI installation was found, but its install location could not be read. ' +
      'Cancel the update and remove the old SortIt version through Windows Settings, then run this installer again.';
    Exit;
  end;

  if not MigrateLegacySettings(LegacyInstallLocation) then begin
    Result := 'Could not migrate SortIt settings from the older installation. ' +
      'Cancel the update and make a copy of sortit.json before retrying.';
    Exit;
  end;

  if not Exec(ExpandConstant('{sys}\msiexec.exe'), '/x "' + ProductCode + '" /qn /norestart', '', SW_HIDE,
      ewWaitUntilTerminated, ExitCode) then begin
    Result := 'Could not start removal of the older SortIt MSI installation. Close SortIt and try again.';
    Exit;
  end;

  if ExitCode = ERROR_SUCCESS_REBOOT_REQUIRED then begin
    NeedsRestart := True;
    Exit;
  end;

  if (ExitCode <> ERROR_SUCCESS) and (ExitCode <> ERROR_UNKNOWN_PRODUCT) then
    Result := 'The older SortIt MSI installation could not be removed (error ' + IntToStr(ExitCode) + '). ' +
      'Close SortIt and retry, or remove the old version through Windows Settings.';
end;

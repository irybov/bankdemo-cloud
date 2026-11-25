@echo off
setlocal

set "CONFIG_FILE=configs\application.properties"

if not exist "%CONFIG_FILE%" (
    echo Error: %CONFIG_FILE% not found!
    exit /b 1
)

:: 1. Generate a cryptographically secure 256-bit (32-byte) Base64 key
for /f "delims=" %%i in ('powershell -NoProfile -Command "$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)"') do set "NEW_SECRET=%%i"

:: 2. Encrypt the key via Config Server's /encrypt endpoint
for /f "delims=" %%c in ('curl.exe -s -f -u config:config -H "Content-Type: text/plain" http://localhost:8888/encrypt --data-raw "%NEW_SECRET%"') do set "CIPHER=%%c"

if "%CIPHER%"=="" (
    echo Error: Failed to encrypt secret. Is Config Server running on http://localhost:8888?
    exit /b 1
)

:: 3. Update token.secret in configs\application.properties (preserving UTF-8 without BOM and all other lines)
powershell -NoProfile -Command ^
    "$path = '%CONFIG_FILE%';" ^
    "$lines = Get-Content -Path $path -Encoding UTF8;" ^
    "$updated = $false;" ^
    "$newLines = foreach ($line in $lines) {" ^
    "    if ($line -match '^\s*token\.secret\s*=') { $updated = $true; 'token.secret={cipher}%CIPHER%' } else { $line }" ^
    "};" ^
    "if (-not $updated) { $newLines += 'token.secret={cipher}%CIPHER%' };" ^
    "[System.IO.File]::WriteAllLines((Resolve-Path $path), $newLines, (New-Object System.Text.UTF8Encoding $false))"

echo Successfully updated token.secret in %CONFIG_FILE%.
echo.

:: 4. Trigger /actuator/refresh on all running services
for %%s in (%SERVICES%) do (
    echo Refreshing %%s/actuator/refresh ...
    curl.exe -s -f -X POST "%%s/actuator/refresh"
    if errorlevel 1 (
        echo [WARN] Could not refresh %%s ^(service unreachable or refresh endpoint disabled^)
    ) else (
        echo.
    )
)

endlocal
@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
set "ENV_FILE=%ROOT_DIR%\.env"

if not exist "%ENV_FILE%" (
    echo Missing .env file. Create one with DB_URL, DB_USERNAME, and DB_PASSWORD before packaging.
    exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%A"=="" (
        if not "%%A:~0,1%"=="#" set "%%A=%%B"
    )
)

if "%DB_URL%"=="" (
    echo DB_URL is required in .env.
    exit /b 1
)

if "%DB_USERNAME%"=="" (
    echo DB_USERNAME is required in .env.
    exit /b 1
)

if "%DB_PASSWORD%"=="" (
    echo DB_PASSWORD is required in .env.
    exit /b 1
)

for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "BUILD_ID=%%I"

set "APP_NAME=RMR Inventory"
set "INPUT_DIR=%TEMP%\rmr-jpackage-input-%BUILD_ID%"
set "DEST_DIR=%ROOT_DIR%\dist\windows-%BUILD_ID%"
set "ZIP_FILE=%ROOT_DIR%\dist\RMR-Inventory-windows-%BUILD_ID%.zip"

cd /d "%ROOT_DIR%"

echo Building desktop app...
call mvnw.cmd -pl rmr package -DskipTests
if errorlevel 1 exit /b 1

echo Preparing packaging input...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
mkdir "%ROOT_DIR%\dist" 2>nul
copy "rmr\target\inventory-desktop-0.0.1-SNAPSHOT.jar" "%INPUT_DIR%\" >nul
xcopy "rmr\target\libs" "%INPUT_DIR%\libs\" /E /I /Y >nul

echo Creating Windows app folder...
if exist "%DEST_DIR%" rmdir /s /q "%DEST_DIR%"
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --input "%INPUT_DIR%" ^
  --main-jar inventory-desktop-0.0.1-SNAPSHOT.jar ^
  --dest "%DEST_DIR%" ^
  --java-options "-DDB_URL=%DB_URL%" ^
  --java-options "-DDB_USERNAME=%DB_USERNAME%" ^
  --java-options "-DDB_PASSWORD=%DB_PASSWORD%"
if errorlevel 1 exit /b 1

echo Creating distributable zip...
if exist "%ZIP_FILE%" del "%ZIP_FILE%"
powershell -NoProfile -Command "Compress-Archive -Path '%DEST_DIR%\%APP_NAME%' -DestinationPath '%ZIP_FILE%'"
if errorlevel 1 exit /b 1

echo.
echo Windows app folder: %DEST_DIR%\%APP_NAME%
echo Exe file:           %DEST_DIR%\%APP_NAME%\%APP_NAME%.exe
echo Zip file:           %ZIP_FILE%

endlocal

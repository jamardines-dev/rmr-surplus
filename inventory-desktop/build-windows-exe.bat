@echo off
echo ========================================================
echo Building Spring Boot executable JAR...
echo ========================================================
call ..\mvnw.cmd clean package -DskipTests

echo ========================================================
echo Staging JAR for packaging...
echo ========================================================
if exist "staging" rmdir /s /q "staging"
mkdir staging
copy target\inventory-desktop-0.0.1-SNAPSHOT.jar staging\

echo ========================================================
echo Packaging into Windows App using jpackage...
echo ========================================================
if exist "windows-app" rmdir /s /q "windows-app"
jpackage --type app-image --name "InventoryDesktop" --input staging --main-jar inventory-desktop-0.0.1-SNAPSHOT.jar --dest windows-app

echo ========================================================
echo Done! Your Windows App is ready in the 'windows-app\InventoryDesktop' folder.
echo You can run the InventoryDesktop.exe from there.
echo ========================================================
pause

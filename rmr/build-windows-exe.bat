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
xcopy target\libs staging\libs\ /E /I

echo ========================================================
echo Packaging into Windows App using jpackage...
echo ========================================================
if exist "windows-app" rmdir /s /q "windows-app"
jpackage --type app-image --name "rmr desktop" --input staging --main-jar inventory-desktop-0.0.1-SNAPSHOT.jar --dest windows-app --win-console

echo ========================================================
echo Done! Your Windows App is ready in the 'windows-app\rmr desktop' folder.
echo You can run the rmr desktop.exe from there.
echo ========================================================
pause

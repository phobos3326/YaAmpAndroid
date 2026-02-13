@echo off
REM Yaamp Android - Quick Fix Script (Windows)
REM This script fixes Gradle compatibility issues

echo.
echo ============================================
echo   Yaamp Android - Quick Fix
echo ============================================
echo.

REM Step 1: Clean old builds
echo [Step 1] Cleaning old builds...
if exist .gradle rmdir /s /q .gradle
if exist app\build rmdir /s /q app\build
if exist build rmdir /s /q build
if exist gradle\wrapper\gradle-wrapper.jar del /f gradle\wrapper\gradle-wrapper.jar
echo [OK] Cleaned
echo.

REM Step 2: Update gradle-wrapper.properties
echo [Step 2] Updating gradle-wrapper.properties...
if not exist gradle\wrapper mkdir gradle\wrapper
(
echo distributionBase=GRADLE_USER_HOME
echo distributionPath=wrapper/dists
echo distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
echo networkTimeout=10000
echo validateDistributionUrl=true
echo zipStoreBase=GRADLE_USER_HOME
echo zipStorePath=wrapper/dists
) > gradle\wrapper\gradle-wrapper.properties
echo [OK] Updated gradle-wrapper.properties
echo.

REM Step 3: Update build.gradle.kts (root)
echo [Step 3] Updating root build.gradle.kts...
(
echo // Top-level build file
echo plugins {
echo     id("com.android.application"^) version "8.5.2" apply false
echo     id("org.jetbrains.kotlin.android"^) version "1.9.24" apply false
echo     id("com.google.devtools.ksp"^) version "1.9.24-1.0.20" apply false
echo }
echo.
echo tasks.register("clean", Delete::class^) {
echo     delete(rootProject.layout.buildDirectory^)
echo }
) > build.gradle.kts
echo [OK] Updated build.gradle.kts
echo.

REM Step 4: Update Compose compiler version
echo [Step 4] Updating Compose compiler version...
if exist app\build.gradle.kts (
    powershell -Command "(gc app\build.gradle.kts) -replace 'kotlinCompilerExtensionVersion = \"1.5.4\"', 'kotlinCompilerExtensionVersion = \"1.5.14\"' | Out-File -encoding ASCII app\build.gradle.kts"
    echo [OK] Updated app\build.gradle.kts
) else (
    echo [ERROR] app\build.gradle.kts not found
)
echo.

REM Step 5: Information
echo [Step 5] Next steps...
echo.
echo Now you need to:
echo 1. Open the project in Android Studio
echo 2. Android Studio will download gradle-wrapper.jar automatically
echo 3. Or run: gradle wrapper --gradle-version 8.5
echo.
echo [OK] Fix completed!
echo.
echo If you still have issues, check TROUBLESHOOTING.md
echo.
pause

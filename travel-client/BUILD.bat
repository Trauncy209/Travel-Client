@echo off
echo ========================================
echo    Travel Client - Build Setup
echo ========================================
echo.

:: Check for Java
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found!
    echo.
    echo Please install JDK 21 from: https://adoptium.net/
    echo Select "Temurin 21 LTS" then Windows x64 .msi
    echo.
    pause
    exit /b 1
)

echo [OK] Java found
echo.

:: Download gradle-wrapper.jar if missing
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] Downloading Gradle Wrapper...
    
    :: Create directory if needed
    if not exist "gradle\wrapper" mkdir gradle\wrapper
    
    :: Use PowerShell to download
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
    
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        echo [ERROR] Failed to download gradle-wrapper.jar
        echo.
        echo Try manually downloading from:
        echo https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar
        echo And place it in: gradle\wrapper\gradle-wrapper.jar
        pause
        exit /b 1
    )
    
    echo [OK] Gradle Wrapper downloaded
) else (
    echo [OK] Gradle Wrapper found
)

echo.
echo ========================================
echo    Building Travel Client...
echo ========================================
echo.
echo This may take a few minutes on first run.
echo.

:: Run the build
call gradlew.bat build

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed! Check errors above.
    pause
    exit /b 1
)

echo.
echo ========================================
echo    BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your mod is at:
echo   build\libs\travel-client-1.0.0.jar
echo.
echo Copy it to your .minecraft\mods folder
echo (with Fabric Loader and Fabric API installed)
echo.
pause

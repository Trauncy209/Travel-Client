# Travel Client Build Script
# Right-click this file -> "Run with PowerShell"

Write-Host "========================================"
Write-Host "   Travel Client - Build Setup"
Write-Host "========================================"
Write-Host ""

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "[OK] Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Java not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install JDK 21 from: https://adoptium.net/"
    Write-Host "Select 'Temurin 21 LTS' then Windows x64 .msi"
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""

# Download gradle-wrapper.jar if missing
$wrapperPath = "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperPath)) {
    Write-Host "[INFO] Downloading Gradle Wrapper..." -ForegroundColor Yellow
    
    # Create directory
    New-Item -ItemType Directory -Force -Path "gradle\wrapper" | Out-Null
    
    # Download
    $wrapperUrl = "https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperPath -UseBasicParsing
        Write-Host "[OK] Gradle Wrapper downloaded" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Failed to download gradle-wrapper.jar" -ForegroundColor Red
        Write-Host "Try downloading manually from:"
        Write-Host $wrapperUrl
        Read-Host "Press Enter to exit"
        exit 1
    }
} else {
    Write-Host "[OK] Gradle Wrapper found" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================"
Write-Host "   Building Travel Client..."
Write-Host "========================================"
Write-Host ""
Write-Host "This may take a few minutes on first run."
Write-Host ""

# Run build
& .\gradlew.bat build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "   BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "========================================"
    Write-Host ""
    Write-Host "Your mod is at:"
    Write-Host "  build\libs\travel-client-1.0.0.jar" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Copy it to your .minecraft\mods folder"
    Write-Host "(with Fabric Loader and Fabric API installed)"
} else {
    Write-Host ""
    Write-Host "[ERROR] Build failed! Check errors above." -ForegroundColor Red
}

Write-Host ""
Read-Host "Press Enter to exit"

@echo off
REM Script để chạy Color Guessing Game Server trên Windows

echo === Color Guessing Game Server ===
echo Checking requirements...

REM Kiểm tra Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java không được tìm thấy. Vui lòng cài đặt Java 17+
    pause
    exit /b 1
)

REM Kiểm tra Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven không được tìm thấy. Vui lòng cài đặt Maven 3.6+
    pause
    exit /b 1
)

echo ✓ Java và Maven đã được cài đặt

REM Build project nếu chưa có target folder
if not exist "target" (
    echo Building project...
    mvn clean compile
    if %errorlevel% neq 0 (
        echo ERROR: Build failed
        pause
        exit /b 1
    )
)

REM Lấy port từ argument hoặc dùng default
set PORT=%1
if "%PORT%"=="" set PORT=8888

echo Starting server on port %PORT%...
echo Press Ctrl+C to stop server
echo ================================

REM Chạy server
mvn exec:java -Dexec.mainClass="com.ncs.server.GameServer" -Dexec.args="%PORT%"

pause

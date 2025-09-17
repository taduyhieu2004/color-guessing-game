@echo off
REM Script để chạy Color Guessing Game Client trên Windows

echo === Color Guessing Game Client ===
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

echo Starting Swing client application...
echo ===============================

REM Chạy client Swing
mvn -q -DskipTests exec:java -Dexec.mainClass="com.ncs.client.SwingClientApplication"

if %errorlevel% neq 0 (
    echo ERROR: Failed to start client
    pause
)

pause

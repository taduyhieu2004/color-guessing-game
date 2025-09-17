#!/bin/bash

# Script để chạy Color Guessing Game Client

echo "=== Color Guessing Game Client ==="
echo "Checking requirements..."

# Kiểm tra Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java không được tìm thấy. Vui lòng cài đặt Java 17+"
    exit 1
fi

# Kiểm tra Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven không được tìm thấy. Vui lòng cài đặt Maven 3.6+"
    exit 1
fi

echo "✓ Java version: $(java -version 2>&1 | head -n 1)"
echo "✓ Maven version: $(mvn -version 2>&1 | head -n 1)"

# Build project nếu chưa có target folder
if [ ! -d "target" ]; then
    echo "Building project..."
    mvn clean compile
    if [ $? -ne 0 ]; then
        echo "ERROR: Build failed"
        exit 1
    fi
fi

echo "Starting Swing client application..."
echo "==============================="

# Chạy client Swing
mvn -q -DskipTests exec:java -Dexec.mainClass="com.ncs.client.SwingClientApplication"

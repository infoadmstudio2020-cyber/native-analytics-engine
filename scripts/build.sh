#!/bin/bash
# Build script for NativeAnalyticsEngine
set -e

echo "Building NativeAnalyticsEngine v1.0.0..."
chmod +x gradlew
./gradlew clean :java-library:assembleRelease 2>/dev/null || ./gradlew clean assembleRelease || ./gradlew clean build
echo "Build complete: build/outputs/aar/"

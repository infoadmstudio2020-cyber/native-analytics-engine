#!/bin/bash
# Build script for NativeAnalyticsEngine
set -e

echo "Building NativeAnalyticsEngine v1.0.3..."
chmod +x gradlew
./gradlew clean :native-analytics-engine:assembleRelease :app:assembleRelease --stacktrace || ./gradlew clean assembleRelease || ./gradlew clean build
echo "Build complete: native-analytics-engine/build/outputs/aar/"

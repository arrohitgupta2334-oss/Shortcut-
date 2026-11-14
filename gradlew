#!/usr/bin/env sh
# Minimal wrapper fallback or dummy execution redirect to gradle if available
echo "Running Gradle build..."
gradle assembleDebug

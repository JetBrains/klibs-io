#!/bin/bash
#
# This script recursively removes all Gradle-related build directories:
# - ".gradle" directories (Gradle's cache and temporary files)
# - "build" directories (compilation output and build artifacts)
#
# Use this script when you need to:
# - Perform a clean build from scratch
# - Troubleshoot build-related issues
#
# Note: This will remove ALL .gradle and build directories from the current
# directory and its subdirectories. Make sure you're in the correct directory
# before running this script.
#

find . \( -name ".gradle" -o -name "build" \) -type d -exec rm -rf {} \;

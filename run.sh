#!/bin/bash

# ===============================
# Apex Estates - Build & Run
# Compiles ALL .java files, then runs AppEntry
# ===============================

LIB_DIR="./lib"
CP=".:$LIB_DIR/*"

echo "Compiling all Java files..."
javac -cp "$CP" *.java

if [ $? -ne 0 ]; then
    echo "Compilation failed ❌"
    exit 1
fi

echo "Compilation successful ✅"
echo "Running AppEntry..."
java -cp "$CP" AppEntry
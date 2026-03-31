#!/bin/bash

# ===============================
# Simple Java Runner with JARs
# Usage: ./main.sh File.java
# ===============================

# Check input
if [ $# -eq 0 ]; then
    echo "Usage: ./main.sh <JavaFile.java>"
    exit 1
fi

FILE=$1

# Extract class name (remove .java)
CLASS=$(basename "$FILE" .java)

# JAR path (your lib folder)
LIB_DIR="./lib"

# Classpath (VERY IMPORTANT)
CP=".:$LIB_DIR/*"

echo "Compiling $FILE..."
javac -cp "$CP" "$FILE"

if [ $? -ne 0 ]; then
    echo "Compilation failed ❌"
    exit 1
fi

echo "Running $CLASS..."
java -cp "$CP" "$CLASS"
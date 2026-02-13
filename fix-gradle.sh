#!/bin/bash

# Yaamp Android - Quick Fix Script
# This script fixes Gradle compatibility issues

echo "🔧 Yaamp Android - Quick Fix"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Clean old builds
echo -e "${YELLOW}Step 1: Cleaning old builds...${NC}"
rm -rf .gradle
rm -rf app/build
rm -rf build
rm -rf gradle/wrapper/gradle-wrapper.jar
echo -e "${GREEN}✓ Cleaned${NC}"
echo ""

# Step 2: Update gradle-wrapper.properties
echo -e "${YELLOW}Step 2: Updating gradle-wrapper.properties...${NC}"
mkdir -p gradle/wrapper
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
echo -e "${GREEN}✓ Updated gradle-wrapper.properties${NC}"
echo ""

# Step 3: Update build.gradle.kts (root)
echo -e "${YELLOW}Step 3: Updating root build.gradle.kts...${NC}"
cat > build.gradle.kts << 'EOF'
// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
EOF
echo -e "${GREEN}✓ Updated build.gradle.kts${NC}"
echo ""

# Step 4: Update Compose compiler version in app/build.gradle.kts
echo -e "${YELLOW}Step 4: Updating Compose compiler version...${NC}"
if [ -f "app/build.gradle.kts" ]; then
    # Use sed to replace the kotlinCompilerExtensionVersion
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' 's/kotlinCompilerExtensionVersion = "1.5.4"/kotlinCompilerExtensionVersion = "1.5.14"/' app/build.gradle.kts
    else
        # Linux
        sed -i 's/kotlinCompilerExtensionVersion = "1.5.4"/kotlinCompilerExtensionVersion = "1.5.14"/' app/build.gradle.kts
    fi
    echo -e "${GREEN}✓ Updated app/build.gradle.kts${NC}"
else
    echo -e "${RED}✗ app/build.gradle.kts not found${NC}"
fi
echo ""

# Step 5: Information
echo -e "${YELLOW}Step 5: Next steps...${NC}"
echo "Now you need to:"
echo "1. Open the project in Android Studio"
echo "2. Android Studio will download gradle-wrapper.jar automatically"
echo "3. Or run: gradle wrapper --gradle-version 8.5"
echo ""
echo -e "${GREEN}✓ Fix completed!${NC}"
echo ""
echo "If you still have issues, check TROUBLESHOOTING.md"
echo ""

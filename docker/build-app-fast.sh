#!/bin/bash
# Fast Docker build for development
# Builds JAR locally, then creates Docker image
# Same as build-app.sh but with optimized settings

set -e

# Configuration
APP_IMAGE_NAME="qodo/command-sdk"
APP_IMAGE_TAG=${APP_IMAGE_TAG:-"latest"}

# Determine the correct path based on current directory
if [ -f "Dockerfile" ]; then
    # Running from docker/ directory
    DOCKERFILE="Dockerfile"
    PROJECT_ROOT=".."
    BUILD_CONTEXT=".."
elif [ -f "docker/Dockerfile" ]; then
    # Running from project root
    DOCKERFILE="docker/Dockerfile"
    PROJECT_ROOT="."
    BUILD_CONTEXT="."
else
    echo -e "${RED}ERROR: Cannot find Dockerfile${NC}"
    echo "Please run this script from either:"
    echo "  - Project root: ./docker/build-app-fast.sh"
    echo "  - Docker directory: cd docker && ./build-app-fast.sh"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Fast Application Build${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Build JAR locally
echo -e "${YELLOW}Step 1/2: Building JAR locally...${NC}"
JAR_START=$(date +%s)

# Run gradlew from project root
(cd "$PROJECT_ROOT" && ./gradlew clean build -x test)

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Gradle build failed!${NC}"
    exit 1
fi

JAR_END=$(date +%s)
JAR_DURATION=$((JAR_END - JAR_START))
echo -e "${GREEN}✓ JAR built in ${JAR_DURATION} seconds${NC}"
echo ""

# Verify JAR exists
JAR_PATH="$PROJECT_ROOT/build/libs/command-sdk.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}ERROR: JAR not found at $JAR_PATH${NC}"
    exit 1
fi

# Step 2: Build Docker image
echo -e "${YELLOW}Step 2/2: Building Docker image...${NC}"
DOCKER_START=$(date +%s)

export DOCKER_BUILDKIT=1

docker build \
    --file "$DOCKERFILE" \
    --tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" \
    --tag "${APP_IMAGE_NAME}:fast-dev" \
    "$BUILD_CONTEXT"

BUILD_EXIT_CODE=$?
DOCKER_END=$(date +%s)
DOCKER_DURATION=$((DOCKER_END - DOCKER_START))

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}ERROR: Docker build failed!${NC}"
    exit 1
fi

TOTAL_DURATION=$((JAR_DURATION + DOCKER_DURATION))

echo ""
echo -e "${GREEN}✓ Docker image built in ${DOCKER_DURATION} seconds${NC}"
echo ""

# Show image details
echo -e "${BLUE}Image Details:${NC}"
docker images "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Fast Build Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Image: ${GREEN}${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "JAR Build Time: ${GREEN}${JAR_DURATION} seconds${NC}"
echo -e "Docker Build Time: ${GREEN}${DOCKER_DURATION} seconds${NC}"
echo -e "Total Time: ${GREEN}${TOTAL_DURATION} seconds${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Run the application: ${BLUE}docker run -p 8081:8081 ${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "  2. Or use docker-compose: ${BLUE}docker compose up${NC}"
echo ""

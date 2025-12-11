#!/bin/bash
# Build application Docker image
# Builds JAR locally first, then creates Docker image

set -e

# Configuration
REGISTRY=${DOCKER_REGISTRY:-""}
APP_IMAGE_NAME="qodo/qodo-app"
APP_IMAGE_TAG=${APP_IMAGE_TAG:-"latest"}

# Determine the correct path based on current directory
if [ -f "Dockerfile" ]; then
    DOCKERFILE="Dockerfile"
    BUILD_CONTEXT=".."
    PROJECT_ROOT=".."
elif [ -f "docker/Dockerfile" ]; then
    DOCKERFILE="docker/Dockerfile"
    BUILD_CONTEXT="."
    PROJECT_ROOT="."
else
    echo -e "${RED}ERROR: Cannot find Dockerfile${NC}"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building Application Docker Image${NC}"
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
JAR_PATH="$PROJECT_ROOT/build/libs/qodo-app.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}ERROR: JAR not found at $JAR_PATH${NC}"
    exit 1
fi

# Step 2: Build Docker image
echo -e "${YELLOW}Step 2/2: Building Docker image...${NC}"
echo -e "${YELLOW}Image: ${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "${YELLOW}Dockerfile: ${DOCKERFILE}${NC}"
echo -e "${YELLOW}Build context: ${BUILD_CONTEXT}${NC}"
echo ""

START_TIME=$(date +%s)

# Enable BuildKit for better caching
export DOCKER_BUILDKIT=1

# Build Docker image
docker build \
    --file "$DOCKERFILE" \
    --tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --progress=plain \
    "$BUILD_CONTEXT"

BUILD_EXIT_CODE=$?
END_TIME=$(date +%s)
BUILD_DURATION=$((END_TIME - START_TIME))

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}ERROR: Docker image build failed!${NC}"
    exit 1
fi

TOTAL_DURATION=$((JAR_DURATION + BUILD_DURATION))

echo ""
echo -e "${GREEN}✓ Docker image built in ${BUILD_DURATION} seconds${NC}"
echo ""

# Show image details
echo -e "${BLUE}Image Details:${NC}"
docker images "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

# Tag with version if specified
if [ -n "$VERSION" ]; then
    echo -e "${YELLOW}Tagging with version: ${VERSION}${NC}"
    docker tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" "${APP_IMAGE_NAME}:${VERSION}"
fi

# Push to registry if specified
if [ -n "$REGISTRY" ]; then
    echo ""
    echo -e "${YELLOW}Pushing application image to registry: ${REGISTRY}${NC}"
    
    FULL_IMAGE_NAME="${REGISTRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"
    docker tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" "$FULL_IMAGE_NAME"
    docker push "$FULL_IMAGE_NAME"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Application image pushed successfully to ${FULL_IMAGE_NAME}${NC}"
    else
        echo -e "${RED}ERROR: Failed to push application image to registry${NC}"
        exit 1
    fi
    
    # Push version tag if specified
    if [ -n "$VERSION" ]; then
        FULL_VERSION_IMAGE="${REGISTRY}/${APP_IMAGE_NAME}:${VERSION}"
        docker tag "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}" "$FULL_VERSION_IMAGE"
        docker push "$FULL_VERSION_IMAGE"
    fi
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Application Image Build Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Image: ${GREEN}${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "JAR Build Time: ${GREEN}${JAR_DURATION} seconds${NC}"
echo -e "Docker Build Time: ${GREEN}${BUILD_DURATION} seconds${NC}"
echo -e "Total Time: ${GREEN}${TOTAL_DURATION} seconds${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Run the application: ${BLUE}docker run -p 8081:8081 ${APP_IMAGE_NAME}:${APP_IMAGE_TAG}${NC}"
echo -e "  2. Or use docker compose: ${BLUE}docker compose up${NC}"
echo ""

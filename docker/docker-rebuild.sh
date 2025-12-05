#!/bin/bash

# Docker Complete Rebuild Script
# Builds JAR locally first, then performs complete Docker rebuild
#
# Usage:
#   ./docker-rebuild.sh              # Use ActiveMQ (default)
#   ./docker-rebuild.sh activemq     # Use ActiveMQ explicitly
#   ./docker-rebuild.sh local        # Use local queue (no ActiveMQ)

set -e  # Exit on any error

# Parse command line argument
MODE="${1:-activemq}"

# Set the docker-compose file based on mode
if [ "$MODE" = "local" ]; then
    COMPOSE_FILE="docker-compose.local.yml"
    MODE_NAME="LOCAL QUEUE"
    MODE_DESC="(no ActiveMQ)"
elif [ "$MODE" = "activemq" ]; then
    COMPOSE_FILE="docker-compose.yml"
    MODE_NAME="ACTIVEMQ"
    MODE_DESC="(with ActiveMQ)"
else
    echo "❌ Invalid mode: $MODE"
    echo ""
    echo "Usage:"
    echo "  ./docker-rebuild.sh              # Use ActiveMQ (default)"
    echo "  ./docker-rebuild.sh activemq     # Use ActiveMQ explicitly"
    echo "  ./docker-rebuild.sh local        # Use local queue (no ActiveMQ)"
    exit 1
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================="
echo "Docker Complete Clean & Rebuild"
echo "Mode: $MODE_NAME $MODE_DESC"
echo "Using: $COMPOSE_FILE"
echo "========================================="
echo ""

# Determine project root
if [ -f "../gradlew" ]; then
    PROJECT_ROOT=".."
elif [ -f "./gradlew" ]; then
    PROJECT_ROOT="."
else
    echo -e "${RED}❌ Error: Cannot find gradlew${NC}"
    exit 1
fi

echo -e "${YELLOW}⚠️  WARNING: This will remove all containers, images, and build cache${NC}"
echo -e "${YELLOW}⚠️  Press Ctrl+C within 5 seconds to cancel...${NC}"
echo ""
sleep 5

echo "========================================="
echo "Step 0: Building JAR locally"
echo "========================================="
echo -e "${BLUE}Building JAR with Gradle...${NC}"
(cd "$PROJECT_ROOT" && ./gradlew clean build -x test)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ JAR built successfully${NC}"
else
    echo -e "${RED}❌ JAR build failed${NC}"
    exit 1
fi
echo ""

echo "========================================="
echo "Step 1: Stopping all containers"
echo "========================================="
docker-compose -f $COMPOSE_FILE down
echo -e "${GREEN}✅ Containers stopped${NC}"
echo ""

echo "========================================="
echo "Step 2: Removing project containers"
echo "========================================="
docker-compose -f $COMPOSE_FILE down --remove-orphans
echo -e "${GREEN}✅ Orphaned containers removed${NC}"
echo ""

echo "========================================="
echo "Step 3: Removing command-sdk image only"
echo "========================================="
# Remove only the command-sdk image, preserve Grafana and Prometheus
docker rmi -f qodo/command-sdk:latest 2>/dev/null || echo "command-sdk image not found (will be built fresh)"
docker rmi -f qodo/command-sdk:fast-dev 2>/dev/null || echo "fast-dev image not found"
echo -e "${GREEN}✅ command-sdk images removed${NC}"
echo ""

echo "========================================="
echo "Step 4: Removing dangling images"
echo "========================================="
docker image prune -f
echo -e "${GREEN}✅ Dangling images removed${NC}"
echo ""

echo "========================================="
echo "Step 5: Clearing build cache"
echo "========================================="
docker builder prune -af
echo -e "${GREEN}✅ Build cache cleared${NC}"
echo ""

echo "========================================="
echo "Step 6: Removing unused volumes"
echo "========================================="
docker volume prune -f
echo -e "${GREEN}✅ Unused volumes removed${NC}"
echo ""

echo "========================================="
echo "Step 7: Removing unused networks"
echo "========================================="
docker network prune -f
echo -e "${GREEN}✅ Unused networks removed${NC}"
echo ""

echo "========================================="
echo "Current Docker Status:"
echo "========================================="
echo "Images:"
docker images | grep -E "command-sdk|activemq|prometheus|grafana" || echo "No project images found"
echo ""
echo "Containers:"
docker ps -a | grep -E "command-sdk|activemq|prometheus|grafana" || echo "No project containers found"
echo ""
echo "Volumes:"
docker volume ls | grep -E "command-sdk|prometheus|grafana" || echo "No project volumes found"
echo ""

echo "========================================="
echo "Step 8: Rebuilding command-sdk from scratch"
echo "========================================="
# Build only command-sdk with --no-cache
docker-compose -f $COMPOSE_FILE build --no-cache --pull command-sdk
echo -e "${GREEN}✅ command-sdk image rebuilt from scratch (no cache)${NC}"
echo ""

echo "========================================="
echo "Step 9: Starting services"
echo "========================================="
docker-compose -f $COMPOSE_FILE up -d
echo -e "${GREEN}✅ Services started${NC}"
echo ""

echo "========================================="
echo "Step 10: Waiting for services to be healthy"
echo "========================================="
echo "Waiting 10 seconds for services to initialize..."
sleep 10

echo ""
echo "Service Status:"
docker-compose -f $COMPOSE_FILE ps
echo ""

echo "========================================="
echo "Step 11: Checking logs"
echo "========================================="
echo "Last 20 lines of command-sdk logs:"
docker-compose -f $COMPOSE_FILE logs --tail=20 command-sdk
echo ""

echo "========================================="
echo -e "${GREEN}✅ Complete rebuild finished!${NC}"
echo "Mode: $MODE_NAME $MODE_DESC"
echo "========================================="
echo ""
echo "Useful commands:"
echo "  View logs:           docker-compose -f $COMPOSE_FILE logs -f"
echo "  View specific logs:  docker-compose -f $COMPOSE_FILE logs -f command-sdk"
echo "  Check status:        docker-compose -f $COMPOSE_FILE ps"
echo "  Stop services:       docker-compose -f $COMPOSE_FILE down"
echo "  Restart service:     docker-compose -f $COMPOSE_FILE restart command-sdk"
echo ""
if [ "$MODE" = "local" ]; then
    echo "Local Queue Mode:"
    echo "  Health check:        curl http://localhost:8081/actuator/health | jq '.components.localQueue'"
    echo "  Queue metrics:       curl http://localhost:8081/actuator/metrics/local.queue.size"
    echo "  Switch to ActiveMQ:  ./docker-rebuild.sh activemq"
else
    echo "ActiveMQ Mode:"
    echo "  ActiveMQ Console:    http://localhost:8161/admin (admin/admin)"
    echo "  Health check:        curl http://localhost:8081/actuator/health"
    echo "  Switch to Local:     ./docker-rebuild.sh local"
fi
echo ""

read -p "Show logs now? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker-compose -f $COMPOSE_FILE logs -f
fi

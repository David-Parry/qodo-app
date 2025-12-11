#!/bin/bash

# Docker Compose Startup Script for Qodo Command Application
# Builds JAR locally first, then starts Docker services

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Qodo Command - Docker Deployment Setup                  ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Error: Docker is not installed"
    echo "   Please install Docker from https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Error: Docker Compose is not installed"
    echo "   Please install Docker Compose from https://docs.docker.com/compose/install/"
    exit 1
fi

echo "✅ Docker and Docker Compose are installed"
echo ""

# Determine project root
if [ -f "../gradlew" ]; then
    PROJECT_ROOT=".."
elif [ -f "./gradlew" ]; then
    PROJECT_ROOT="."
else
    echo "❌ Error: Cannot find gradlew"
    exit 1
fi

# Set the docker-compose file path
if [ -f "docker-compose.yml" ]; then
    COMPOSE_FILE="docker-compose.yml"
elif [ -f "../docker-compose.yml" ]; then
    COMPOSE_FILE="../docker-compose.yml"
else
    echo "❌ Error: Cannot find docker-compose.yml"
    exit 1
fi

# Check if Spring Boot jar exists
JAR_FILE="$PROJECT_ROOT/build/libs/qodo-app.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "⚠️  Spring Boot jar not found at $JAR_FILE"
    echo "🔨 Building Spring Boot application..."
    echo ""
    
    # Build the jar
    (cd "$PROJECT_ROOT" && ./gradlew clean build -x test)
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Spring Boot jar built successfully"
    else
        echo ""
        echo "❌ Error: Failed to build Spring Boot jar"
        exit 1
    fi
else
    echo "✅ Spring Boot jar found at $JAR_FILE"
    
    # Check if jar is older than source files (optional rebuild check)
    if [ -n "$(find $PROJECT_ROOT/src -type f -newer $JAR_FILE 2>/dev/null)" ]; then
        echo "⚠️  Source files are newer than jar file"
        read -p "Would you like to rebuild? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "🔨 Rebuilding Spring Boot application..."
            (cd "$PROJECT_ROOT" && ./gradlew clean build -x test)
            if [ $? -eq 0 ]; then
                echo "✅ Spring Boot jar rebuilt successfully"
            else
                echo "❌ Error: Failed to rebuild Spring Boot jar"
                exit 1
            fi
        fi
    fi
fi

echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found"
    echo ""
    
    if [ -f .env.example ]; then
        echo "📋 Creating .env from .env.example..."
        cp .env.example .env
        echo "✅ .env file created"
        echo ""
        echo "⚠️  IMPORTANT: Please edit .env file and update these values:"
        echo "   - WEBSOCKET_TOKEN (required for WebSocket connections)"
        echo "   - ACTIVEMQ_USERNAME and ACTIVEMQ_PASSWORD (for security)"
        echo "   - SNYK_WEBHOOK_SECRET (if using Snyk integration)"
        echo ""
        read -p "Press Enter to continue after editing .env, or Ctrl+C to exit..."
    else
        echo "⚠️  .env.example not found, continuing without .env"
    fi
else
    echo "✅ .env file found"
fi

echo ""
echo "════════════════════════════════════════════════════════════"
echo "Starting Docker Compose deployment..."
echo "════════════════════════════════════════════════════════════"
echo ""

# Ask if user wants to force rebuild
echo "🔨 Build Options:"
echo "   1) Use cached layers (faster)"
echo "   2) Force fresh build (--no-cache, slower but ensures fresh image)"
echo ""
read -p "Select option (1/2) [default: 1]: " -n 1 -r BUILD_OPTION
echo ""

if [[ $BUILD_OPTION == "2" ]]; then
    echo "🔨 Force building fresh image (no cache)..."
    # Remove existing images to ensure completely fresh build
    docker-compose -f $COMPOSE_FILE down --rmi local 2>/dev/null || true
    docker-compose -f $COMPOSE_FILE build --no-cache --pull
else
    echo "🔨 Building with cache..."
    docker-compose -f $COMPOSE_FILE build --pull
fi

echo ""
echo "🚀 Starting services..."
docker-compose -f $COMPOSE_FILE up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 5

# Check service status
echo ""
echo "📊 Service Status:"
docker-compose -f $COMPOSE_FILE ps

echo ""
echo "════════════════════════════════════════════════════════════"
echo "✅ Deployment Complete!"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "🌐 Access Points:"
echo "   • Spring Boot App:    http://localhost:8081"
echo "   • Health Check:       http://localhost:8081/actuator/health"
echo "   • Actuator:           http://localhost:8081/actuator"
echo "   • ActiveMQ Console:   http://localhost:8161/admin/"
echo "     (default: admin/admin)"
echo ""
echo "📝 Useful Commands:"
echo "   • View logs:          docker-compose -f $COMPOSE_FILE logs -f"
echo "   • Stop services:      docker-compose -f $COMPOSE_FILE down"
echo "   • Restart:            docker-compose -f $COMPOSE_FILE restart"
echo "   • Check status:       docker-compose -f $COMPOSE_FILE ps"
echo ""

# Offer to show logs
echo ""
read -p "Show logs now? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Showing logs (Ctrl+C to exit)..."
    docker-compose -f $COMPOSE_FILE logs -f
fi

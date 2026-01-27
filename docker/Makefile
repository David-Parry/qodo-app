# Makefile for Docker Build Optimization
# Provides convenient shortcuts for building Docker images

.PHONY: help base app fast benchmark clean test run stop logs shell local-start local-stop local-logs

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)Docker Build Optimization - Available Commands$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(YELLOW)Quick Start:$(NC)"
	@echo "  1. make base     # Build base image (one-time, ~5-10 min)"
	@echo "  2. make fast     # Fast build for development (~30-60 sec)"
	@echo "  3. make run      # Start the application"
	@echo ""

build: ## Build JAR locally (uses local Maven dependencies)
	@echo "$(BLUE)Building JAR locally...$(NC)"
	@cd .. && ./gradlew clean build -x test
	@echo "$(GREEN)✓ JAR built successfully at build/libs/qodo-app.jar$(NC)"

base: ## Build base Docker image (run rarely, ~5-10 min)
	@echo "$(BLUE)Building base image...$(NC)"
	@./build-base.sh

app: build ## Build application image (builds JAR first, then Docker, then starts services)
	@echo "$(BLUE)Building Docker image...$(NC)"
	@docker build -t qodo/qodo-app:latest -f Dockerfile ..
	@echo "$(GREEN)✓ Docker image built successfully$(NC)"
	@echo "$(BLUE)Starting services with docker-compose...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose up 2>&1 | tee docker/out.log; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose up 2>&1 | tee out.log; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo ""
	@echo "$(GREEN)✓ Application started$(NC)"
	@echo "$(YELLOW)Access at: http://localhost:8081$(NC)"
	@echo "$(YELLOW)Logs saved to: docker/out.log$(NC)"
	@echo ""
	@echo "$(BLUE)Last 20 lines of output:$(NC)"
	@if [ -f "out.log" ]; then \
		tail -20 out.log; \
	elif [ -f "../docker/out.log" ]; then \
		tail -20 ../docker/out.log; \
	fi

fast: ## Fast build for development (recommended, ~30-60 sec)
	@echo "$(BLUE)Building JAR locally...$(NC)"
	@cd .. && ./gradlew clean build -x test
	@echo "$(GREEN)✓ JAR built successfully$(NC)"
	@echo "$(BLUE)Building Docker image...$(NC)"
	@docker build -t qodo/qodo-app:latest -f Dockerfile ..
	@echo "$(GREEN)✓ Docker image built successfully$(NC)"

rebuild: build ## Force complete rebuild (JAR + Docker no-cache + start services)
	@echo "$(BLUE)Stopping existing services...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose down 2>/dev/null || true; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose down 2>/dev/null || true; \
	fi
	@echo "$(BLUE)Removing old images...$(NC)"
	@docker rmi -f qodo/qodo-app:latest 2>/dev/null || true
	@docker rmi -f qodo/qodo-app:fast-dev 2>/dev/null || true
	@echo "$(BLUE)Clearing build cache...$(NC)"
	@docker builder prune -af
	@echo "$(BLUE)Force rebuilding Docker image (no cache)...$(NC)"
	@docker build --no-cache \
		--build-arg CACHEBUST=$$(date +%s) \
		--build-arg GITHUB_ACTOR=${GITHUB_ACTOR} \
		--build-arg GITHUB_TOKEN=${GITHUB_TOKEN} \
		-t qodo/qodo-app:latest \
		-f Dockerfile \
		..
	@echo "$(GREEN)✓ Force rebuild complete$(NC)"
	@echo "$(BLUE)Starting services with docker-compose...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose up 2>&1 | tee docker/out.log; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose up 2>&1 | tee out.log; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo ""
	@echo "$(GREEN)✓ Application started$(NC)"
	@echo "$(YELLOW)Access at: http://localhost:8081$(NC)"
	@echo "$(YELLOW)Logs saved to: docker/out.log$(NC)"
	@echo ""
	@echo "$(BLUE)Last 20 lines of output:$(NC)"
	@if [ -f "out.log" ]; then \
		tail -20 out.log; \
	elif [ -f "../docker/out.log" ]; then \
		tail -20 ../docker/out.log; \
	fi

clean: ## Clean Docker images and build cache
	@echo "$(YELLOW)Cleaning Docker images and cache...$(NC)"
	@docker rmi qodo/qodo-app:latest qodo/qodo-app:fast-dev 2>/dev/null || true
	@docker builder prune -f
	@echo "$(GREEN)✓ Cleanup complete$(NC)"

clean-all: ## Clean everything including base image
	@echo "$(YELLOW)Cleaning all Docker images and cache...$(NC)"
	@docker rmi qodo/qodo-app:latest qodo/qodo-app:fast-dev qodo/qodo-app-base:latest 2>/dev/null || true
	@docker builder prune -af
	@echo "$(GREEN)✓ Full cleanup complete$(NC)"

test: ## Test the Docker image
	@echo "$(BLUE)Testing Docker image...$(NC)"
	@docker run --rm qodo/qodo-app:latest java --version
	@docker run --rm qodo/qodo-app:latest node --version
	@docker run --rm qodo/qodo-app:latest snyk --version
	@echo "$(GREEN)✓ All tests passed$(NC)"

run: ## Run the application with docker-compose
	@echo "$(BLUE)Starting application...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose up -d; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose up -d; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Application started$(NC)"
	@echo "$(YELLOW)Access at: http://localhost:8081$(NC)"

stop: ## Stop the application
	@echo "$(YELLOW)Stopping application...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose down; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose down; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Application stopped$(NC)"

local-start: ## Start local environment (no ActiveMQ, in-memory queue)
	@echo "$(BLUE)Starting local environment...$(NC)"
	@docker compose -f docker-compose.local.yml up -d
	@echo "$(GREEN)✓ Local environment started$(NC)"
	@echo "$(YELLOW)Access at: http://localhost:8081$(NC)"

local-stop: ## Stop local environment
	@echo "$(YELLOW)Stopping local environment...$(NC)"
	@docker compose -f docker-compose.local.yml down
	@echo "$(GREEN)✓ Local environment stopped$(NC)"

local-logs: ## Show local environment logs
	@docker compose -f docker-compose.local.yml logs -f command-sdk

restart: fast ## Fast rebuild and restart (development workflow)
	@echo "$(BLUE)Restarting application...$(NC)"
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose restart qodo-app; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose restart qodo-app; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Application restarted$(NC)"

logs: ## Show application logs
	@if [ -f "../docker-compose.yml" ]; then \
		cd .. && docker compose logs -f qodo-app; \
	elif [ -f "docker-compose.yml" ]; then \
		docker compose logs -f qodo-app; \
	else \
		echo "$(YELLOW)Error: docker-compose.yml not found$(NC)"; \
		exit 1; \
	fi

shell: ## Open shell in running container
	@docker exec -it qodo-app /bin/bash

stats: ## Show Docker image sizes
	@echo "$(BLUE)Docker Image Statistics:$(NC)"
	@echo ""
	@docker images qodo/qodo-app-base --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
	@docker images qodo/qodo-app --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
	@echo ""

info: ## Show build system information
	@echo "$(BLUE)Build System Information:$(NC)"
	@echo ""
	@echo "$(GREEN)Base Image:$(NC)"
	@docker image inspect qodo/qodo-app-base:latest --format '  Size: {{.Size}} bytes ({{.VirtualSize}} virtual)' 2>/dev/null || echo "  Not built yet"
	@echo ""
	@echo "$(GREEN)Application Image:$(NC)"
	@docker image inspect qodo/qodo-app:latest --format '  Size: {{.Size}} bytes ({{.VirtualSize}} virtual)' 2>/dev/null || echo "  Not built yet"
	@echo ""
	@echo "$(GREEN)BuildKit Status:$(NC)"
	@docker buildx version 2>/dev/null || echo "  BuildKit not available"
	@echo ""

# Development workflow shortcuts
dev-setup: base fast ## Complete development setup (base + fast build)
	@echo "$(GREEN)✓ Development environment ready!$(NC)"

dev-rebuild: fast restart ## Quick development cycle (rebuild + restart)
	@echo "$(GREEN)✓ Development rebuild complete!$(NC)"

# CI/CD workflow
ci-build: base app ## CI/CD build workflow
	@echo "$(GREEN)✓ CI/CD build complete!$(NC)"

# Health check
health: ## Check application health
	@echo "$(BLUE)Checking application health...$(NC)"
	@curl -s http://localhost:8081/actuator/health | jq . || echo "$(YELLOW)Application not running or jq not installed$(NC)"

# Version info
version: ## Show version information
	@echo "$(BLUE)Version Information:$(NC)"
	@echo ""
	@echo "$(GREEN)Java:$(NC)"
	@docker run --rm qodo/qodo-app:latest java --version 2>/dev/null || echo "  Image not built"
	@echo ""
	@echo "$(GREEN)Node.js:$(NC)"
	@docker run --rm qodo/qodo-app:latest node --version 2>/dev/null || echo "  Image not built"
	@echo ""
	@echo "$(GREEN)Snyk:$(NC)"
	@docker run --rm qodo/qodo-app:latest snyk --version 2>/dev/null || echo "  Image not built"
	@echo ""

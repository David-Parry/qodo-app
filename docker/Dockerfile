# Runtime-only image for Spring Boot application
# JAR is built locally before Docker build using: ./gradlew clean build -x test
# This allows use of local Maven dependencies without authentication issues

FROM timbru31/java-node:21-jdk-20

# Set working directory
WORKDIR /app

# Install system dependencies (Git/GitHub tools, no Gradle/Maven)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    bash \
    git \
    openssh-client \
    sudo \
    procps \
    curl \
    ca-certificates \
    gnupg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME for timbru31/java-node image
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Reset working directory back to /app
WORKDIR /app

# Create a non-root user (Ubuntu syntax)
RUN groupadd -r spring && useradd -r -g spring -d /home/spring -s /bin/bash -m spring

# Create .ssh directory for spring user with proper permissions
RUN mkdir -p /home/spring/.ssh && \
    chmod 700 /home/spring/.ssh && \
    chown -R spring:spring /home/spring/.ssh

# Configure git globally
RUN git config --global push.autoSetupRemote true && \
    git config --global user.email "agent@qodo.ai" && \
    git config --global user.name "Agent User"

# Copy the pre-built JAR from local build directory
COPY --chown=spring:spring build/libs/qodo-app.jar app.jar

# Copy the agent.yml configuration file to the app directory
COPY --chown=spring:spring docker/agent.yml /app/agent.yml

# Copy application.yml to override the one in the JAR
COPY --chown=spring:spring src/main/resources/application.yml /app/application.yml

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Copy MCP internal JAR
COPY mcp/mcp-internal-1.0.3.jar /app/mcp-internal-1.0.3.jar
RUN chmod +x /app/mcp-internal-1.0.3.jar

# Copy the entrypoint script
COPY --chown=spring:spring docker/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Install Snyk CLI globally using npm (works on all architectures)
RUN npm install -g snyk@latest && \
    npm cache clean --force

# Verify installations
RUN echo "Verifying installations..." && \
    java --version && \
    node --version && \
    npm --version && \
    snyk --version && \
    git --version

# Set PATH environment variable to ensure binaries are found
ENV PATH="/usr/local/bin:/usr/bin:/bin:${PATH}"

# Switch to non-root user
USER spring

# Configure git for spring user
RUN git config --global --add safe.directory '*'

# Add GitHub to known hosts
RUN ssh-keyscan github.com >> /home/spring/.ssh/known_hosts \
 && chmod 600 /home/spring/.ssh/known_hosts

RUN git config --global url."git@github.com:".insteadOf "https://github.com/" \
 && git config --global push.autoSetupRemote true \
 && git config --global user.email "agent@qodo.ai" \
 && git config --global user.name "Agent User"

# Expose the application port
EXPOSE 8081

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Use the entrypoint script
ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]

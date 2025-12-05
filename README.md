# Qodo App

A Spring Boot application built on the Qodo Agent Core framework for processing webhooks and orchestrating AI agents.

## Overview

This application demonstrates how to build production-ready AI agent integrations using the Agent Core framework. The included examples (Jira and CloudWatch integrations) show patterns for processing events through custom handlers that orchestrate AI agents for tasks like code generation, bug analysis, and log monitoring.

**Built on Spring Boot**, this application leverages the full Spring ecosystem - REST controllers, JMS messaging, Spring Data, Spring Security, and more. Add your own integrations using familiar Spring patterns: REST endpoints, message listeners, scheduled tasks, database repositories, or custom Spring beans. The examples provided demonstrate the patterns, but you can integrate with any system or build entirely custom workflows.

### Key Capabilities

- **Spring Boot Foundation** - Leverage the entire Spring ecosystem (Web, Data, Security, Cloud, etc.)
- **Flexible Integration** - REST APIs, JMS/ActiveMQ, scheduled jobs, database triggers, Kafka consumers, gRPC services
- **Deterministic Workflows** - Map agent responses to predictable business logic and outcomes
- **Event-Driven Architecture** - Async message processing with guaranteed delivery and retry logic
- **Extensible Handler System** - Create custom handlers for domain-specific agent orchestration
- **Production-Ready** - Built-in monitoring, health checks, and observability with Prometheus/Grafana
- **Cloud-Native** - Containerized deployment with Docker Compose or Kubernetes

## Prerequisites

- Java 21+
- Docker and Docker Compose (for containerized deployment)
- Access to local Maven repository (for Agent Core dependency)

## Quick Start

### 1. Build the application

```bash
./gradlew build
```

### 3. Run locally

```bash
./gradlew bootRun
```

### 4. Build and run with Docker

```bash
cd docker
make fast    # Fast build for development
make run     # Start all services
```

## Version Management

The application uses the Agent Core framework. Version is configured in `gradle.properties`:

```properties
# gradle.properties
internalCoreVersion=2.0.5
mcpInternalVersion=1.0.3
```

To update to a newer version:

1. Update the version in `gradle.properties`
2. Rebuild: `./gradlew build --refresh-dependencies`
3. Test and deploy

## Project Structure

```
qodo-app/
├── src/main/java/ai/qodo/app/
│   ├── Application.java               # Main Spring Boot application
│   ├── config/                        # Configuration classes
│   │   ├── JiraAgentProperties.java   # Jira webhook settings
│   │   ├── WebhookProperties.java     # General webhook config
│   │   └── WebSecurityConfig.java     # Security configuration
│   ├── handlers/                      # Agent message handlers
│   │   ├── JiraAgentHandler.java      # Processes Jira events
│   │   ├── CodingAgentHandler.java    # Handles code generation
│   │   ├── CloudWatchAgentHandler.java # Processes AWS logs
│   │   └── IncompleteAgentResponseHandler.java
│   └── controllers/                   # REST API endpoints
│       ├── JiraWebhookController.java # Jira webhook endpoint
│       ├── JiraWebhookValidator.java  # Webhook signature validation
│       └── CloudWatchLogWebhookController.java
├── src/main/resources/
│   └── application.yml                # Application configuration
├── docker/                            # Docker build system
│   ├── Dockerfile.app                 # Optimized build
│   ├── Dockerfile.app-fast            # Fast development build
│   ├── docker-compose.yml             # Multi-service orchestration
│   ├── Makefile                       # Build commands
│   └── agent.yml                      # Agent configuration
└── mcp/                               # MCP server tools
```

## Configuration

### Application Configuration

The application uses a layered configuration system:

1. **Agent Core defaults** - Base framework configuration
2. **Application overrides** - Custom settings in `src/main/resources/application.yml`

Key configuration properties:

```yaml
# Jira webhook configuration
jira:
  webhook:
    secret: ${JIRA_WEBHOOK_SECRET}
    validate-signature: ${JIRA_WEBHOOK_VALIDATION_ENABLED:false}
    agent:
      account-id: ${JIRA_AGENT_ACCOUNT_ID:}
      block-enabled: ${JIRA_BLOCK_AGENT_WEBHOOKS:true}

# Messaging configuration
messaging:
  provider: ${MESSAGING_PROVIDER:activemq}
  activemq:
    broker-url: ${MESSAGING_ACTIVEMQ_BROKER_URL:tcp://localhost:61616}

# MCP (Model Context Protocol) Configuration
qodo:
  mcp:
    request-timeout-seconds: ${QODO_MCP_REQUEST_TIMEOUT_SECONDS:300}
```

### Environment Variables

**Required:**
- `JIRA_WEBHOOK_SECRET` - Secret for validating Jira webhook signatures

**Optional:**
- `JIRA_WEBHOOK_VALIDATION_ENABLED` - Enable/disable signature validation (default: false)
- `JIRA_AGENT_ACCOUNT_ID` - Jira account ID to block from triggering webhooks
- `JIRA_BLOCK_AGENT_WEBHOOKS` - Prevent webhook loops (default: true)
- `MESSAGING_PROVIDER` - Message broker type: `activemq` or `local` (default: activemq)
- `MESSAGING_ACTIVEMQ_BROKER_URL` - ActiveMQ connection URL
- `QODO_MCP_REQUEST_TIMEOUT_SECONDS` - MCP request timeout (default: 300)
- `GITHUB_API_TOKEN` - GitHub API token for agent operations
- `SNYK_TOKEN` - Snyk API token for security scanning

See `docker/.env.example` for a complete list of environment variables.

## Integration Patterns & Use Cases

This application serves as a **reference implementation** demonstrating how to build production-ready integrations on top of the Agent Core framework. The included examples (Jira, CloudWatch) are just starting points - the real power comes from adapting these patterns to your specific environment.

### Example Integration Scenarios

**DevOps & Incident Management:**
- PagerDuty alerts → Automated root cause analysis → Slack notification with remediation steps
- Datadog anomaly detection → Agent investigates logs → Creates Jira ticket with findings
- GitHub PR opened → Security scan + code review → Auto-comment with suggestions

**Project Management:**
- Jira bug created → Agent analyzes stack trace → Suggests fix + creates PR
- Linear issue updated → Agent checks dependencies → Updates related tickets
- Asana task assigned → Agent gathers context → Prepares briefing document

**Monitoring & Observability:**
- CloudWatch alarm triggered → Agent analyzes metrics → Determines if scaling needed
- Prometheus alert → Agent correlates with recent deployments → Identifies culprit
- Sentry error → Agent reproduces issue → Generates test case

### Building Deterministic Workflows

The key to production reliability is **mapping non-deterministic agent outputs to deterministic business logic**. Here's how:

#### Pattern 1: Response Classification

```java
@Service("incidentAnalysisHandler" + Handler.HANDLER_SUFFIX)
@Scope("prototype")
public class IncidentAnalysisHandler extends BaseHandler {
    
    @Override
    public Map<String, Object> handle(Map<String, Object> message) {
        // Agent provides analysis (non-deterministic)
        String agentResponse = (String) message.get("analysis");
        
        // Map to deterministic outcomes
        IncidentSeverity severity = classifySeverity(agentResponse);
        List<String> actionItems = extractActionItems(agentResponse);
        
        // Trigger predictable workflows based on classification
        switch (severity) {
            case CRITICAL -> escalateToOnCall(actionItems);
            case HIGH -> createUrgentTicket(actionItems);
            case MEDIUM -> scheduleForNextSprint(actionItems);
            case LOW -> addToBacklog(actionItems);
        }
        
        return buildResponse(severity, actionItems);
    }
    
    private IncidentSeverity classifySeverity(String analysis) {
        // Use keywords, confidence scores, or secondary validation
        if (analysis.contains("production down") || analysis.contains("data loss")) {
            return IncidentSeverity.CRITICAL;
        }
        // ... more classification logic
    }
}
```

#### Pattern 2: Multi-Stage Validation

```java
@Service("codeReviewHandler" + Handler.HANDLER_SUFFIX)
@Scope("prototype")
public class CodeReviewHandler extends BaseHandler {
    
    @Override
    public Map<String, Object> handle(Map<String, Object> message) {
        String prUrl = (String) message.get("pull_request_url");
        
        // Stage 1: Agent performs initial review
        AgentResponse initialReview = invokeAgent("code_review", prUrl);
        
        // Stage 2: Validate findings with static analysis
        List<Issue> validatedIssues = validateWithSonarQube(initialReview.getIssues());
        
        // Stage 3: Check against team standards
        List<Issue> policyViolations = checkAgainstPolicy(validatedIssues);
        
        // Stage 4: Deterministic decision
        if (policyViolations.isEmpty() && validatedIssues.size() < 5) {
            approvePR(prUrl);
        } else {
            requestChanges(prUrl, policyViolations);
        }
        
        return buildResponse(validatedIssues, policyViolations);
    }
}
```

#### Pattern 3: Confidence-Based Routing

```java
@Service("bugTriageHandler" + Handler.HANDLER_SUFFIX)
@Scope("prototype")
public class BugTriageHandler extends BaseHandler {
    
    @Override
    public Map<String, Object> handle(Map<String, Object> message) {
        String bugDescription = (String) message.get("description");
        
        // Agent analyzes bug
        TriageResult result = invokeAgent("bug_triage", bugDescription);
        
        // Route based on confidence level
        if (result.getConfidence() > 0.9) {
            // High confidence: Auto-assign to team
            assignToTeam(result.getTeam(), result.getReasoning());
            
        } else if (result.getConfidence() > 0.7) {
            // Medium confidence: Suggest assignment, require human approval
            suggestAssignment(result.getTeam(), result.getReasoning());
            
        } else {
            // Low confidence: Route to human triage
            escalateToHumanTriage(bugDescription, result.getReasoning());
        }
        
        return buildResponse(result);
    }
}
```

### Integration Best Practices

**1. Idempotency**
```java
// Always check if work was already done
if (ticketAlreadyCreated(eventId)) {
    logger.info("Ticket already exists for event {}", eventId);
    return existingTicket;
}
```

**2. Retry Logic with Exponential Backoff**
```yaml
# application.yml
messaging:
  local:
    retry-attempts: 3
    retry-delay-ms: 1000
    max-retry-delay-ms: 30000
    exponential-backoff: true
```

**3. Circuit Breaker for External APIs**
```java
@Service
public class ExternalApiClient {
    
    @CircuitBreaker(name = "jiraApi", fallbackMethod = "fallbackCreateTicket")
    public Ticket createTicket(TicketRequest request) {
        return jiraClient.createIssue(request);
    }
    
    private Ticket fallbackCreateTicket(TicketRequest request, Exception e) {
        logger.error("Jira API unavailable, queuing for retry", e);
        queueForRetry(request);
        return Ticket.pending();
    }
}
```

**4. Structured Logging for Observability**
```java
logger.info("Processing webhook event", 
    kv("event_type", eventType),
    kv("event_id", eventId),
    kv("source", source),
    kv("handler", this.getClass().getSimpleName())
);
```

**5. Metrics for Business Insights**
```java
@Service
public class MetricsService {
    private final MeterRegistry registry;
    
    public void recordAgentInvocation(String agentType, boolean success, long durationMs) {
        registry.counter("agent.invocations", 
            "type", agentType,
            "success", String.valueOf(success)
        ).increment();
        
        registry.timer("agent.duration",
            "type", agentType
        ).record(Duration.ofMillis(durationMs));
    }
}
```

### Extending for Your Environment

The Spring Boot foundation means you can integrate using any pattern you're familiar with. Here are common approaches:

#### Option 1: REST API Integration

Build REST endpoints to receive events from external systems:

```java
@RestController
@RequestMapping("/api/github")
public class GitHubIntegrationController {
    
    private final MessagePublisher messagePublisher;
    
    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody String payload) {
        
        // Validate and publish to message queue
        messagePublisher.publish("github_event", Map.of(
            "event_type", eventType,
            "payload", payload,
            "timestamp", Instant.now()
        ));
        
        return ResponseEntity.accepted().build();
    }
}
```

#### Option 2: Scheduled Jobs

Use Spring's `@Scheduled` to poll external systems or trigger periodic agent tasks:

```java
@Component
public class PeriodicSecurityScanJob {
    
    private final MessagePublisher messagePublisher;
    
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void runSecurityScan() {
        List<Repository> repos = fetchRepositories();
        
        repos.forEach(repo -> {
            messagePublisher.publish("security_scan", Map.of(
                "repository", repo.getName(),
                "branch", "main",
                "scan_type", "full"
            ));
        });
    }
}
```

#### Option 3: JMS Message Listener

Consume messages from external message brokers:

```java
@Component
public class ExternalEventListener {
    
    private final MessagePublisher messagePublisher;
    
    @JmsListener(destination = "external.events.queue")
    public void handleExternalEvent(String message) {
        // Transform external message format to internal format
        Map<String, Object> event = parseExternalMessage(message);
        
        // Route to appropriate handler
        messagePublisher.publish("agent_task", event);
    }
}
```

#### Option 4: Database Triggers

Use Spring Data to monitor database changes:

```java
@Service
public class DatabaseEventMonitor {
    
    @Transactional
    @EventListener
    public void onDeploymentCreated(DeploymentCreatedEvent event) {
        // Trigger agent to analyze deployment
        messagePublisher.publish("deployment_analysis", Map.of(
            "deployment_id", event.getDeploymentId(),
            "environment", event.getEnvironment(),
            "version", event.getVersion()
        ));
    }
}
```

#### Option 5: Kafka Consumer

Integrate with Kafka streams for high-throughput event processing:

```java
@Component
public class KafkaEventConsumer {
    
    @KafkaListener(topics = "production-errors", groupId = "agent-processor")
    public void consumeErrorEvent(ErrorEvent event) {
        // Process high-volume error stream
        messagePublisher.publish("error_analysis", Map.of(
            "error_id", event.getId(),
            "stack_trace", event.getStackTrace(),
            "frequency", event.getCount()
        ));
    }
}
```

#### Option 6: gRPC Service

Expose gRPC endpoints for high-performance integrations:

```java
@GrpcService
public class AgentGrpcService extends AgentServiceGrpc.AgentServiceImplBase {
    
    @Override
    public void triggerAnalysis(AnalysisRequest request, 
                                StreamObserver<AnalysisResponse> responseObserver) {
        // Handle gRPC request
        messagePublisher.publish("grpc_analysis", Map.of(
            "request_id", request.getId(),
            "data", request.getData()
        ));
        
        responseObserver.onNext(AnalysisResponse.newBuilder()
            .setStatus("ACCEPTED")
            .build());
        responseObserver.onCompleted();
    }
}
```

#### Complete Integration Example

Here's a full example integrating with GitHub:

**Step 1: Create REST Controller**
```java
@RestController
@RequestMapping("/api/github")
public class GitHubIntegrationController {
    
    private final MessagePublisher messagePublisher;
    private final GitHubWebhookValidator validator;
    
    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody String payload) {
        
        if (!validator.isValid(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        messagePublisher.publish("github_event", Map.of(
            "event_type", eventType,
            "payload", payload,
            "timestamp", Instant.now()
        ));
        
        return ResponseEntity.accepted().build();
    }
}
```

**Step 2: Create Handler**
```java
@Service("githubPrHandler" + Handler.HANDLER_SUFFIX)
@Scope("prototype")
public class GitHubPrHandler extends BaseHandler {
    
    private final GitHubApiClient githubClient;
    
    @Override
    public String type() {
        return "github_pr_review";
    }
    
    @Override
    public Map<String, Object> handle(Map<String, Object> message) {
        String prUrl = (String) message.get("pull_request_url");
        
        // Invoke agent for code review
        AgentResponse review = invokeAgent("code_review", prUrl);
        
        // Post review comments
        githubClient.postReview(prUrl, review.getComments());
        
        return Map.of(
            "status", "completed",
            "comments_posted", review.getComments().size()
        );
    }
}
```

**Step 3: Add Configuration**
```java
@Configuration
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {
    private String apiToken;
    private String webhookSecret;
    private String baseUrl = "https://api.github.com";
    
    // getters and setters
}
```

**Step 4: Configure in application.yml**
```yaml
github:
  api-token: ${GITHUB_API_TOKEN}
  webhook-secret: ${GITHUB_WEBHOOK_SECRET}
  base-url: https://api.github.com
```

**Step 5: Add Tests**
```java
@SpringBootTest
@AutoConfigureMockMvc
class GitHubIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldAcceptValidGitHubWebhook() throws Exception {
        String payload = loadTestPayload("pr_opened.json");
        String signature = generateSignature(payload);
        
        mockMvc.perform(post("/api/github/events")
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Event", "pull_request")
                .content(payload))
            .andExpect(status().isAccepted());
    }
}
```

## Extending the Framework

### Adding Custom Handlers

Handlers process messages from the Agent Core framework:

```java
import com.davidparry.agent.core.api.Handler;
import com.davidparry.agent.core.service.BaseHandler;
import com.davidparry.agent.core.service.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("myCustomHandler" + Handler.HANDLER_SUFFIX)
@Scope("prototype")
public class MyCustomHandler extends BaseHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(MyCustomHandler.class);
    
    public MyCustomHandler(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        super(messagePublisher, objectMapper);
    }
    
    @Override
    public String type() {
        return "my_custom_type";
    }
    
    @Override
    public Map<String, Object> handle(Map<String, Object> map) {
        // Custom handling logic
        logger.info("Processing custom message: {}", map);
        return map;
    }
}
```

**Key Classes from Agent Core:**
- `com.davidparry.agent.core.service.BaseHandler` - Base class for all message handlers
- `com.davidparry.agent.core.service.MessagePublisher` - Publishes messages to message queues
- `com.davidparry.agent.core.api.Handler` - Handler interface and constants

### Adding Custom Controllers

Add REST endpoints for webhooks or APIs:

```java
@RestController
@RequestMapping("/api/custom")
public class MyCustomController {
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        // Handle webhook
        return ResponseEntity.ok("OK");
    }
}
```

### Adding Custom Configuration

```java
@Configuration
@ConfigurationProperties(prefix = "myapp")
public class MyAppProperties {
    private String customProperty;
    
    // getters and setters
}
```

## Docker Deployment

The application includes an optimized Docker build system with **85-90% faster build times** for development.

### Quick Start

```bash
cd docker

# First time: Build base image (5-10 minutes, one-time)
make base

# Build application (30-60 seconds)
make fast

# Start all services
make run
```

### Services Included

- **qodo-app** - Main application
- **activemq** - Message broker for async processing
- **prometheus** - Metrics collection
- **grafana** - Metrics visualization dashboard

### Available Commands

```bash
make help          # Show all available commands
make fast          # Fast development build (recommended)
make run           # Start all services
make stop          # Stop all services
make restart       # Rebuild and restart
make logs          # View application logs
make health        # Check application health
make clean         # Clean up images
```

### Environment Configuration

1. Copy the example environment file:
   ```bash
   cp docker/.env.example docker/.env
   ```

2. Edit `docker/.env` with your configuration:
   ```bash
   # Required
   JIRA_WEBHOOK_SECRET=your-secret-here
   
   # Optional
   JIRA_WEBHOOK_VALIDATION_ENABLED=true
   MESSAGING_PROVIDER=activemq
   ```

3. Start the application:
   ```bash
   make run
   ```

See [docker/README.md](docker/README.md) for detailed Docker documentation.

## Development Workflow

### Local Development

```bash
# Make code changes
vim src/main/java/ai/qodo/app/handlers/MyHandler.java

# Build and test
./gradlew build
./gradlew test

# Run locally
./gradlew bootRun
```

### Docker Development (Recommended)

```bash
# Make code changes
vim src/main/java/ai/qodo/app/handlers/MyHandler.java

# Fast rebuild and restart (30-60 seconds)
cd docker
make restart

# View logs
make logs
```

### Updating Agent Core Version

When a new version of Agent Core is released:

1. Update `gradle.properties`:
   ```properties
   internalCoreVersion=X.Y.Z
   ```

2. Rebuild with fresh dependencies:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

3. Rebuild Docker image:
   ```bash
   cd docker
   make fast
   ```

4. Test and deploy

## Monitoring and Observability

### Health Checks

```bash
# Application health
curl http://localhost:8081/actuator/health

# Quick health check with Docker
cd docker && make health
```

### Metrics

```bash
# All available metrics
curl http://localhost:8081/actuator/metrics

# Specific metric
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Prometheus format
curl http://localhost:8081/actuator/prometheus
```

### Grafana Dashboard

Access the monitoring dashboard at: **http://localhost:3000**

- **Username:** `admin`
- **Password:** `admin`

The dashboard includes:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Message queue statistics
- Custom application metrics

### Logs

```bash
# View application logs
cd docker && make logs

# Follow logs in real-time
docker compose -f docker/docker-compose.yml logs -f qodo-app

# View specific service logs
docker compose -f docker/docker-compose.yml logs activemq
```

## Troubleshooting

### Application Won't Start

**Check logs:**
```bash
cd docker && make logs
```

**Common issues:**
- Missing environment variables - check `docker/.env`
- Port conflicts - ensure ports 8081, 61616, 9090, 3000 are available
- ActiveMQ not ready - wait 30 seconds for broker to start

### MCP Client Initialization Timeout

**Symptom:** Application fails to start with timeout error

**Solution:**
1. Increase timeout in `docker/.env`:
   ```bash
   QODO_MCP_REQUEST_TIMEOUT_SECONDS=600
   ```

2. Restart:
   ```bash
   cd docker && make restart
   ```

### Build Fails

**Gradle build fails:**
```bash
# Clean and rebuild
./gradlew clean build

# Force refresh dependencies
./gradlew build --refresh-dependencies
```

**Docker build fails:**
```bash
cd docker

# Clean everything
make clean-all

# Rebuild from scratch
make base
make fast
```

### Webhook Not Receiving Events

**Check webhook endpoint:**
```bash
curl -X POST http://localhost:8081/api/jira/webhook \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

**Verify Jira webhook configuration:**
- URL should point to: `http://your-server:8081/api/jira/webhook`
- Secret must match `JIRA_WEBHOOK_SECRET` environment variable
- Signature validation enabled if `JIRA_WEBHOOK_VALIDATION_ENABLED=true`

### Message Queue Issues

**Check ActiveMQ console:**
- URL: http://localhost:8161
- Username: `admin`
- Password: `admin`

**Verify queues exist:**
- `audit` - Audit messages
- `event` - Event messages  
- `response` - Response messages

### Performance Issues

**Check resource usage:**
```bash
docker stats
```

**Increase JVM memory:**
Edit `docker/docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: "-Xmx2g -Xms1g"
```

## API Endpoints

### Jira Webhook

**POST** `/api/jira/webhook`

Receives Jira webhook events (issue created, updated, etc.)

**Headers:**
- `Content-Type: application/json`
- `X-Hub-Signature` (if validation enabled)

**Example:**
```bash
curl -X POST http://localhost:8081/api/jira/webhook \
  -H "Content-Type: application/json" \
  -d @jira-event.json
```

### CloudWatch Logs Webhook

**POST** `/api/cloudwatch/webhook`

Receives AWS CloudWatch log events

**Example:**
```bash
curl -X POST http://localhost:8081/api/cloudwatch/webhook \
  -H "Content-Type: application/json" \
  -d @cloudwatch-event.json
```

### Health Check

**GET** `/actuator/health`

Returns application health status

### Metrics

**GET** `/actuator/metrics`

Returns available metrics

**GET** `/actuator/prometheus`

Returns metrics in Prometheus format

## Architecture

### Message Flow

```
Webhook → Controller → Message Queue → Handler → Agent → Response Queue
```

1. **Webhook Controller** receives HTTP POST from external system
2. **Validation** checks signature and payload
3. **Message Queue** stores event for async processing
4. **Handler** processes message and invokes appropriate agent
5. **Agent** performs task (code generation, analysis, etc.)
6. **Response Queue** stores result for delivery

### Components

- **Controllers** - REST endpoints for receiving webhooks
- **Handlers** - Process messages and orchestrate agents
- **Message Broker** - ActiveMQ for async message processing
- **Agent Core** - Framework for agent lifecycle and communication
- **MCP Server** - Model Context Protocol tools and capabilities

## License

AGPL-3.0

## Related Projects

- **Agent Core** - Core framework for agent orchestration
- **MCP Internal** - Model Context Protocol server implementation

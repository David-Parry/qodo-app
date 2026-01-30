import java.util.concurrent.TimeUnit

plugins {
    id("java")
    id("application")
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.unbroken-dome.test-sets") version "4.1.0"
}

group = project.findProperty("group") as String? ?: "ai.qodo.app"
version = project.findProperty("version") as String? ?: "1.0.0-SNAPSHOT"

// Version properties
val internalCoreVersion = project.findProperty("internalCoreVersion") as String? ?: "2.1.6"
val mcpInternalVersion = project.findProperty("mcpInternalVersion") as String? ?: "1.0.3"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

// Custom configuration for MCP JAR
val mcpServer by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

repositories {
    mavenLocal()
    mavenCentral()
    
    maven {
        name = "GitHubPackagesInternalCore"
        url = uri("https://maven.pkg.github.com/David-Parry/spring-command-sdk")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
        }
    }
}

dependencies {
    // Internal Core - uses dynamic version resolution
    implementation("com.davidparry.agent:core:$internalCoreVersion")
    
    // Spring Boot startersm
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // MCP Internal Server JAR
    mcpServer("ai.qodo.mcp:mcp-internal:$mcpInternalVersion")
}

application {
    mainClass.set("ai.qodo.app.Application")
}

springBoot {
    buildInfo {
        properties {
            additional.put("core.version", 
                configurations.runtimeClasspath.get()
                    .resolvedConfiguration
                    .resolvedArtifacts
                    .find { it.moduleVersion.id.name == "core" }
                    ?.moduleVersion?.id?.version ?: internalCoreVersion
            )
        }
    }
}

tasks.register<Copy>("copyMcpJar") {
    from(mcpServer)
    into(layout.projectDirectory.dir("mcp"))
    rename { "mcp-internal-${mcpInternalVersion}.jar" }
}

tasks.register("prepareMcp") {
    dependsOn("copyMcpJar")
    doLast {
        println("MCP JAR copied to mcp/mcp-internal-${mcpInternalVersion}.jar")
    }
}

tasks.register("showVersions") {
    doLast {
        val resolvedVersion = configurations.runtimeClasspath.get()
            .resolvedConfiguration
            .resolvedArtifacts
            .find { it.moduleVersion.id.name == "core" }
            ?.moduleVersion?.id?.version
        
        println("Resolved core version: $resolvedVersion")
        println("MCP internal version: $mcpInternalVersion")
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("bootBuildInfo")
    
    val resolvedInternalCoreVersion = provider {
        configurations.runtimeClasspath.get()
            .resolvedConfiguration
            .resolvedArtifacts
            .find { it.moduleVersion.id.name == "core" }
            ?.moduleVersion?.id?.version ?: internalCoreVersion
    }
    
    val tokenMap = mapOf(
        "project.version" to version.toString(),
        "core.version" to resolvedInternalCoreVersion.get()
    )
    
    filesMatching("application.yml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to tokenMap
        )
    }
    
    filesMatching("banner.txt") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to tokenMap
        )
    }
}

tasks.named("compileJava") {
    dependsOn("bootBuildInfo")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}

tasks.bootJar {
    archiveFileName.set("qodo-app.jar")
    
    dependsOn("bootBuildInfo")
    from(layout.buildDirectory.file("resources/main/META-INF/build-info.properties")) {
        into("BOOT-INF/classes/META-INF")
    }
}

testSets {
    create("integrationTest")
}

tasks.named<Test>("integrationTest") {
    useJUnitPlatform()
}

tasks.register("refreshDependencies") {
    doLast {
        configurations.all {
            resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
            resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
        }
    }
}

// Load environment variables from docker/.env file
fun loadEnvFile(envFile: File): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            // Skip comments and empty lines
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val equalIndex = trimmed.indexOf('=')
                if (equalIndex > 0) {
                    val key = trimmed.substring(0, equalIndex).trim()
                    val value = trimmed.substring(equalIndex + 1).trim()
                    envMap[key] = value
                }
            }
        }
    }
    return envMap
}

// Filter out Docker/container-specific environment variables that don't work locally
fun filterForLocalRun(envVars: MutableMap<String, String>): MutableMap<String, String> {
    // Remove JAVA_OPTS as it may contain container-specific flags like UseContainerSupport
    envVars.remove("JAVA_OPTS")
    return envVars
}

// Configure bootRun task to use environment variables from docker/.env
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    doFirst {
        val envFile = file("docker/.env")
        if (envFile.exists()) {
            val envVars = loadEnvFile(envFile).toMutableMap()
            
            // Map Atlassian variables to the names expected by the application
            envVars["ATLASSIAN_EMAIL"]?.let { envVars["ATLASSIAN_JIRA_USERNAME"] = it }
            envVars["ATLASSIAN_API_TOKEN"]?.let { envVars["ATLASSIAN_JIRA_API_TOKEN"] = it }
            envVars["ATLASSIAN_SITE_URL"]?.let { envVars["ATLASSIAN_JIRA_URL"] = it }
            
            // Filter out container-specific variables
            filterForLocalRun(envVars)
            
            // Set the agent config file to use the local file path
            envVars["QODO_AGENT_CONFIG_FILE"] = "file:${projectDir}/src/main/resources/agent.yml"
            
            // Apply environment variables
            environment(envVars)
            
            println("Loaded environment variables from docker/.env")
        }
    }
}

// Task to run bootRun with environment variables from docker/.env (processes agent.yml)
tasks.register<JavaExec>("bootRunLocal") {
    group = "application"
    description = "Processes agent.yml with .env values and runs Spring Boot locally"
    
    dependsOn("classes")
    
    mainClass.set("ai.qodo.app.Application")
    classpath = sourceSets["main"].runtimeClasspath
    
    doFirst {
        val envFile = file("docker/.env")
        val envVars = loadEnvFile(envFile).toMutableMap()
        
        // Map Atlassian variables to the names expected by the application
        envVars["ATLASSIAN_EMAIL"]?.let { envVars["ATLASSIAN_JIRA_USERNAME"] = it }
        envVars["ATLASSIAN_API_TOKEN"]?.let { envVars["ATLASSIAN_JIRA_API_TOKEN"] = it }
        envVars["ATLASSIAN_SITE_URL"]?.let { envVars["ATLASSIAN_JIRA_URL"] = it }
        
        // Filter out container-specific variables
        filterForLocalRun(envVars)
        
        // Read agent.yml and replace placeholders
        val agentYmlFile = file("src/main/resources/agent.yml")
        var agentYmlContent = agentYmlFile.readText()
        
        // Replace {VARIABLE_NAME} placeholders with actual values
        envVars.forEach { (key, value) ->
            agentYmlContent = agentYmlContent.replace("{$key}", value)
        }
        
        // Write processed agent.yml to build directory
        val processedAgentYml = file("${layout.buildDirectory.get()}/resources/main/agent-processed.yml")
        processedAgentYml.parentFile.mkdirs()
        processedAgentYml.writeText(agentYmlContent)
        
        println("Processed agent.yml written to: ${processedAgentYml.absolutePath}")
        
        // Set the agent config to use the processed file
        envVars["QODO_AGENT_CONFIG_FILE"] = "file:${processedAgentYml.absolutePath}"
        
        // Print loaded environment variables (excluding sensitive values)
        println("\nLoaded environment variables from docker/.env:")
        envVars.keys.sorted().forEach { key ->
            val displayValue = if (key.contains("TOKEN", ignoreCase = true) || 
                                   key.contains("SECRET", ignoreCase = true) ||
                                   key.contains("PASSWORD", ignoreCase = true) ||
                                   key.contains("KEY", ignoreCase = true)) {
                "****"
            } else {
                envVars[key]
            }
            println("  $key=$displayValue")
        }
        
        println("\nStarting Spring Boot application...")
        
        // Apply environment variables to this JavaExec task
        environment(envVars)
    }
}

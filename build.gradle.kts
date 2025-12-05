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
val internalCoreVersion = project.findProperty("internalCoreVersion") as String? ?: "2.0.4"
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
    
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.boot:spring-boot-starter-activemq")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
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

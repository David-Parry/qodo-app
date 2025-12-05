/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Qodo Command SDK.
 * 
 * The internal-core module is automatically configured via Spring Boot
 * auto-configuration (QodoCommandAutoConfiguration). This application 
 * extends the core functionality with custom handlers, controllers, 
 * and configurations.
 * 
 * No explicit @ComponentScan is needed - auto-configuration handles it.
 */
@SpringBootApplication(scanBasePackages = {"ai.qodo.app", "com.davidparry.agent.core"})
public class Application {
    public static void main(String[] args) {

        // Create SpringApplication and disable the default banner since we already printed it
        SpringApplication app = new SpringApplication(Application.class);
        app.run(args);
    }

}

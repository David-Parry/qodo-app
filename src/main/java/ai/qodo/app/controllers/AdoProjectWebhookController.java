/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.app.controllers;

import ai.qodo.app.config.AdoProperties;
import com.davidparry.agent.core.api.StringConstants;
import com.davidparry.agent.core.service.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot WebMVC Controller to handle Jira webhook calls.
 * Jira webhooks are triggered by various events like issue creation, updates, etc.
 * The issue key is extracted dynamically from the webhook payload.
 * <p>
 * Configuration required in application.properties:
 * - jira.webhook.secret: Your webhook secret for validation (optional)
 * - jira.webhook.validate-signature: Enable/disable signature validation (default: false)
 */
@RestController
@RequestMapping("/api/webhooks")
public class AdoProjectWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(AdoProjectWebhookController.class);

    // Jira webhook header names
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String X_ADO_PROJECT_SIGNATURE = "X-ADO-Project-Signature";
    private static final String RESPONSE_ERROR = "error";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_MESSAGE = "message";
    private static final String RESPONSE_SERVICE = "service";
    private static final String RESPONSE_SIGNATURE_VALIDATION = "signatureValidation";
    private static final String RESPONSE_TIMESTAMP = "timestamp";
    private static final String WIKI_AGENT= "ado_wiki_agent";
    private final ObjectMapper objectMapper;
    private final MessagePublisher messagePublisher;
    private final AdoProperties adoProperties;
    public AdoProjectWebhookController(ObjectMapper objectMapper,
                                       MessagePublisher messagePublisher, AdoProperties adoProperties) {
        this.objectMapper = objectMapper;
        this.messagePublisher = messagePublisher;
        this.adoProperties = adoProperties;
    }


    @PostMapping("/ado/project")
    public ResponseEntity<?> handleAdoProjectWebhook(
            @RequestHeader(value = HEADER_USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = HEADER_CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = X_ADO_PROJECT_SIGNATURE, required = false) String signature,
            @RequestBody String rawBody) {

        logger.info("Received ADO Project log - User-Agent: {}, Content-Type: {} body {}", userAgent, contentType,
                    rawBody);

        if(adoProperties.isBlockEnabled()) {
            if(!adoProperties.getSignature().equals(signature)) {
                logger.error("Webhook signature validation failed - ADO Project webhook error: {}", rawBody);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(RESPONSE_ERROR, "Invalid signature"));
            }
        }
        logger.info("Webhook signature validation passed - ADO Project webhook processed successfully body of message {}", rawBody);


        try {
            // Parse the JSON payload to extract fields for template processing
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);

            // Prepare message payload - merge the parsed payload at root level
            // This allows template processor to resolve paths like {/resource/repository/remoteUrl}
            Map<String, Object> messagePayload = new HashMap<>(payload);
            messagePayload.put(StringConstants.MESSAGE_TYPE.getValue(), WIKI_AGENT);

            // Publish message to the event queue
            String msgQueue = objectMapper.writeValueAsString(messagePayload);
            messagePublisher.publishEvent(msgQueue);

            logger.info("Successfully processed ADO Project webhook, message size {}", msgQueue.length());

        } catch (Exception e) {
            logger.error("Error processing AWS Cloudwatch log webhook", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(RESPONSE_ERROR, "Internal server error"));
        }


        return ResponseEntity.ok(Map.of(RESPONSE_STATUS, "success", RESPONSE_MESSAGE, "Webhook processed " +
                "successfully"));
    }

    /**
     * Health check endpoint for Jira webhook service
     */
    @GetMapping("/ado/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(RESPONSE_STATUS, "healthy", RESPONSE_SERVICE, "AWS Log Webhook Handler",
                                        RESPONSE_SIGNATURE_VALIDATION, true, RESPONSE_TIMESTAMP, Instant
                .now()
                .toString()));
    }

}
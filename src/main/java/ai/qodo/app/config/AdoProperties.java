/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 */
@Component
@ConfigurationProperties(prefix = "ado.webhook")
public class AdoProperties {
    
    private String signature;
    
    /**
     * Whether to block webhooks triggered by the agent.
     * When true, webhooks from the agent account will be skipped to prevent loops.
     */
    private boolean blockEnabled = true;


    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isBlockEnabled() {
        return blockEnabled;
    }
    
    public void setBlockEnabled(boolean blockEnabled) {
        this.blockEnabled = blockEnabled;
    }
}

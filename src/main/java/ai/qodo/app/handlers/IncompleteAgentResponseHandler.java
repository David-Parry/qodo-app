/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.app.handlers;

import com.davidparry.agent.core.api.Handler;
import com.davidparry.agent.core.service.BaseHandler;
import com.davidparry.agent.core.service.EndFlowCleanup;
import com.davidparry.agent.core.service.MessagePublisher;
import com.davidparry.agent.core.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service(MessageService.INCOMPLETE_NODE + Handler.HANDLER_SUFFIX)
@Scope("prototype")
public class IncompleteAgentResponseHandler extends BaseHandler {

    private static final Logger logger = LoggerFactory.getLogger(IncompleteAgentResponseHandler.class);

    public IncompleteAgentResponseHandler(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        super(messagePublisher, objectMapper);
    }

    @Override
    public String type() {
        return EndFlowCleanup.TYPE;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> map) {
        logger.info("nothing to do but pass through {}", map.size());
        if(logger.isTraceEnabled()) {
            try {
                logger.trace("Complete map contents: {}", objectMapper.writeValueAsString(map));
            } catch (Exception e) {
                logger.trace("Complete map contents (fallback): {}", map);
            }
        }
        return map;
    }


}

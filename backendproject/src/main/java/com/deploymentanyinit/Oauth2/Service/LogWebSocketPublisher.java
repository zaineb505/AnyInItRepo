package com.deploymentanyinit.Oauth2.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogWebSocketPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public LogWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendLog(String logLine) {
        messagingTemplate.convertAndSend("/topic/logs", logLine);
    }
}

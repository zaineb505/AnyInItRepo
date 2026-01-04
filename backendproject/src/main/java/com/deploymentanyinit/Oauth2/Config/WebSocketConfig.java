package com.deploymentanyinit.Oauth2.Config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /*@Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/logs").setAllowedOriginPatterns("*").withSockJS();
    }*/


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // Endpoint WebSocket
                .setAllowedOriginPatterns("*")  // Autoriser toutes les origines
                .withSockJS();  // Support pour SockJS
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

}

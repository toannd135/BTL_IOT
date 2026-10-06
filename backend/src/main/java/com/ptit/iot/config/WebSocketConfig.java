package com.ptit.iot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String TOPIC_SENSORS = "/topic/sensors";
    public static final String TOPIC_DEVICE_STATUS = "/topic/devices/%d/status";

    private final AppProperties props;

    public WebSocketConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = props.cors().allowedOrigins().toArray(String[]::new);
        registry.addEndpoint("/ws/sensors", "/ws/devices").setAllowedOriginPatterns(origins);
        registry.addEndpoint("/ws/sensors", "/ws/devices").setAllowedOriginPatterns(origins).withSockJS();
    }
}

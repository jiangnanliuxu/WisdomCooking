package com.ruoyi.cook.message.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 消息中心 WebSocket 配置。
 * <p>
 * 第四阶段使用 Spring 内置 Simple Broker，客户端可连接 /ws 并订阅：
 * /topic/conversations/{conversationId} 接收新消息，
 * /topic/conversations/{conversationId}/events 接收会话更新事件。
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class CookWebSocketConfig implements WebSocketMessageBrokerConfigurer
{
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry)
    {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry)
    {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}

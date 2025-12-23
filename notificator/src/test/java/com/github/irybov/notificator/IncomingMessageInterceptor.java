package com.github.irybov.notificator;

import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

//@GlobalChannelInterceptor(patterns = "input")
public class IncomingMessageInterceptor  implements ChannelInterceptor {
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
	
	detection( (String) message.getPayload());
        return message;
    }

    public void detection(String payload) {}
    
}

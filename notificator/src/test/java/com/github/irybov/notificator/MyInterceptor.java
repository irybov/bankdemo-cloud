package com.github.irybov.notificator;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

@TestComponent
@GlobalChannelInterceptor(patterns = "input-in-0")
public class MyInterceptor implements ChannelInterceptor {
    
    private final AtomicReference<Message<?>> atomicMessage = new AtomicReference<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        atomicMessage.set(message);
        return message;
    }

    public AtomicReference<Message<?>> getAtomicReference(){return atomicMessage;}
}

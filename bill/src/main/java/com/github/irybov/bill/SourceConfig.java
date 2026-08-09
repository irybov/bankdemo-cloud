package com.github.irybov.bill;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

//@Configuration
public class SourceConfig {

    @Bean
    public Supplier<Message<Map<Integer, Double>>> output(){
        return () -> {
            Map<Integer, Double> data = null;
            return MessageBuilder.withPayload(data).build();
        };
    }
    
}

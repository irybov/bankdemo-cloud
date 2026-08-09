package com.github.irybov.notificator;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SinkConfig {

//    @Autowired
//    private ObjectMapper mapper;
    
    @Bean
    public Consumer<String> input() {

        return json -> {
            String payload  = extractPayload(json);
            if(payload.length() > 3) System.out.println(payload);
            else throw new RuntimeException("OMG");
        };
    }
    
    private String extractPayload(@Payload String data) {

        // JsonNode node = mapper.readTree(data);
        // JsonNode payload = node.get("payload");
        // return payload.toString();
        return data;
    }

}

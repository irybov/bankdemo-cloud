package com.github.irybov.notificator;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SinkConfig {
	
    private final RestClient restClient;
    private final ObjectMapper mapper;
    public SinkConfig(RestClient.Builder builder, ObjectMapper mapper, @Value("${external.service}") String externalUrl) {
    	this.restClient = builder.baseUrl(externalUrl).build();
    	this.mapper = mapper;
    }
	
	private static final TypeReference<Map<Integer, Double>> MAP_TYPE = new TypeReference<>() {};
	/*
	 * private record PayloadWrapper(Map<Integer, Double> payload) {
	 * 
	 * @JsonCreator PayloadWrapper { if (payload == null || payload.isEmpty() ||
	 * payload.containsValue(null)) { throw new
	 * IllegalArgumentException("Payload must be a non-empty Map<Integer, Double> without nulls"
	 * ); } } }
	 */

    @Bean
    public Consumer<String> input() {
        return json -> {
        String payload  = extractPayload(json);
            restClient.post()
            .uri("/display")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
        };
    }

//    private String extractPayload(@Payload String data) {return data;}
    private String extractPayload(String data) {
    	
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Incoming message is empty");
        }
//        PayloadWrapper wrapper = mapper.readValue(data, PayloadWrapper.class);
        Map<Integer, Double> validatedMap = mapper.readValue(data, MAP_TYPE);
        
        if (validatedMap == null || validatedMap.isEmpty()) {
            throw new IllegalArgumentException("Payload map cannot be null or empty");
        }

        for (Map.Entry<Integer, Double> entry : validatedMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("Invalid map entry: " + entry);
            }
        }
//        return mapper.writeValueAsString(wrapper.payload());
        return mapper.writeValueAsString(validatedMap);
    }

}

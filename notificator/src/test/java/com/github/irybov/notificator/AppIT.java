package com.github.irybov.notificator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
@Import(MyInterceptor.class)
public class AppIT {

	@Container
    @ServiceConnection
    private static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.3.6-alpine"));
    
    // @Autowired
    // private StreamBridge streamBridge;
    @Autowired
    private AmqpAdmin admin;
    @Autowired
    private RabbitTemplate template;

    @Value("${spring.cloud.stream.bindings.input-in-0.destination}")
    private String EXCHANGE;
    @Value("${spring.cloud.stream.bindings.input-in-0.group}")
    private String GROUP;
    private String INPUT_QUEUE;
    private String DLQ;
    @Value("${spring.cloud.stream.rabbit.bindings.input-in-0.consumer.binding-routing-key}")
    private String KEY;

    @Autowired
    private ObjectMapper mapper;
    private Map<Integer, Double> data;
    private String json;
    
	@BeforeAll
	void prepare() throws JsonProcessingException {
	    
	    INPUT_QUEUE = EXCHANGE + "." + GROUP;
	    DLQ = INPUT_QUEUE + ".dlq";
	    
        data = new LinkedHashMap<>(2);
        data.put(1, -3.00);
        data.put(2, 44.00);
        json = mapper.writeValueAsString(data);
	}

    @Test
	void context_loading(ApplicationContext context) {
	    assertThat(context).isNotNull();
    	assertThat(rabbitmq.isRunning()).isTrue();
	    assertThat(context.getBean("sinkConfig", SinkConfig.class)).isNotNull();
        assertThat(context.getBean("input")).isNotNull();
        assertThat(context.getBean("myInterceptor")).isNotNull();
	}

    @Test
    void message_consuming(ApplicationContext context) {

        MyInterceptor interceptor = context.getBean("myInterceptor", MyInterceptor.class);
        final AtomicReference<Message<?>> atomicMessage = interceptor.getAtomicReference();

        this.template.convertAndSend(EXCHANGE, KEY, json);
//        streamBridge.setAsync(true);
//        streamBridge.send("output-in-0", json);
        Awaitility.await().untilAsserted(() -> {
                Message<?> message = atomicMessage.get();
                assertThat(message).isNotNull();
                assertThat(message).hasFieldOrPropertyWithValue("payload", json.getBytes());
        });
    }

    @Test
    void dead_letters_queue() throws JsonProcessingException {

        Message<String> message = MessageBuilder.withPayload("OMG").build();

        this.template.setReceiveTimeout(-1);
        this.template.convertAndSend(EXCHANGE, KEY, "OMG");
        // streamBridge.setAsync(true);
        // streamBridge.send("output-in-0", mapper.writeValueAsString(message));

        final AtomicReference<org.springframework.amqp.core.Message> atomicMessage = new AtomicReference<>();
        atomicMessage.set(template.receive(DLQ));
        Awaitility.await().untilAsserted(() -> {
            org.springframework.amqp.core.Message received = atomicMessage.get();
            assertThat(received).isNotNull();
            assertThat(received.getMessageProperties().getHeader("x-death").toString()).isNotBlank();
            assertThat(received.getBody()).isEqualTo("OMG".getBytes());
    	});
    }

    @AfterAll
    void clear() {    	
        this.admin.deleteExchange(EXCHANGE);
        this.admin.deleteExchange("DLX");
        this.admin.deleteQueue(INPUT_QUEUE);
        this.admin.deleteQueue(DLQ);
    }
    
}

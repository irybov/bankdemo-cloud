package com.github.irybov.notificator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
@Import(MyInterceptor.class)
//@AutoConfigureMockRestServiceServer
@TestPropertySource(properties = "spring.config.location=classpath:/")
public class AppIT {

	@Container
    @ServiceConnection
    private static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.3.6-alpine"));
    
// 	  @Autowired
// 	  private StreamBridge streamBridge;
    @Autowired
    private RabbitTemplate template;
    @Autowired
    private AmqpAdmin admin;

    @Value("${spring.cloud.stream.bindings.input-in-0.destination}")
    private String EXCHANGE;
    @Value("${spring.cloud.stream.bindings.input-in-0.group}")
    private String GROUP;
    private String INPUT_QUEUE;
    private String DLQ;
    @Value("${spring.cloud.stream.rabbit.bindings.input-in-0.consumer.binding-routing-key}")
    private String KEY;
    @Value("${external.service.url}")
    private String externalUrl;

    @Autowired
    private ObjectMapper mapper;
    private Map<Integer, Double> data;
    private String json;
    
    @Autowired
    private MockRestServiceServer mockServer;
    @TestConfiguration
    static class MockServerConfig {
    	
    	private MockRestServiceServer mockServer;
    	
        @Bean
        @Primary
        RestClient.Builder testRestClientBuilder() {
            RestClient.Builder builder = RestClient.builder();
            this.mockServer = MockRestServiceServer.bindTo(builder).build();
            return builder;
//            return RestClient.builder();
        }
        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
//            return MockRestServiceServer.bindTo(builder).build();
            return this.mockServer;
        }
    }
    
	@BeforeAll
	void prepare() throws JsonProcessingException {
	    
	    INPUT_QUEUE = EXCHANGE + "." + GROUP;
	    DLQ = INPUT_QUEUE + ".dlq";
	    
        data = new LinkedHashMap<>(2);
        data.put(1, -3.00);
        data.put(2, 44.00);
        json = mapper.writeValueAsString(data);
	}
	
    @BeforeEach
    void resetState(@Autowired MyInterceptor interceptor) {
        mockServer.reset();
        interceptor.getAtomicReference().set(null);
        admin.purgeQueue(DLQ, false);
    }

    @Test
	void context_loading(ApplicationContext context) {
	    assertThat(context).isNotNull();
    	assertThat(rabbitmq.isRunning()).isTrue();
	    assertThat(context.getBean("sinkConfig", SinkConfig.class)).isNotNull();
        assertThat(context.getBean("input")).isNotNull();
        assertThat(context.getBean(MyInterceptor.class)).isNotNull();
	}

    @Test
    void message_consuming(@Autowired MyInterceptor interceptor) {
    	
        mockServer.expect(requestTo(externalUrl + "/display"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(json))
        .andRespond(withNoContent());

        this.template.convertAndSend(EXCHANGE, KEY, json);
//        streamBridge.setAsync(true);
//        streamBridge.send("output-in-0", json);
        final AtomicReference<Message<?>> atomicMessage = interceptor.getAtomicReference();
        Awaitility.await().untilAsserted(() -> {
                Message<?> received = atomicMessage.get();
                assertThat(received).isNotNull();
                assertThat(received).hasFieldOrPropertyWithValue("payload", json.getBytes(StandardCharsets.UTF_8));
                mockServer.verify();
        });
        assertThat(template.receive(DLQ)).isNull();
    }
    
    @Test
    void external_failure() {
    	
        mockServer.expect(manyTimes(), requestTo(externalUrl + "/display"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(json))
        .andRespond(withServerError());

//        this.template.setReceiveTimeout(-1);
        this.template.convertAndSend(EXCHANGE, KEY, json);
        
        Awaitility.await().untilAsserted(() -> {
        		org.springframework.amqp.core.Message received = template.receive(DLQ);
                assertThat(received).isNotNull();
	            assertThat(received.getMessageProperties().getHeader("x-death").toString()).isNotBlank();
                assertThat(received.getBody()).isEqualTo(json.getBytes(StandardCharsets.UTF_8));
                mockServer.verify();
        });
    }

    @Test
    void dead_letters_queue() throws JsonProcessingException {

    	String[] payload = new String[5];
    	payload[0] = "null";
    	payload[1] = " ";
    	payload[2] = "{}";
    	payload[3] = "XYZ";
    	data.clear();
        data.put(1, -3.00);
        data.put(2, null);
    	payload[4] = mapper.writeValueAsString(data);
//        Message<String> message = MessageBuilder.withPayload("XYZ").build();
    	for(String corrupted : payload) {
//	        this.template.setReceiveTimeout(-1);
	        this.template.convertAndSend(EXCHANGE, KEY, corrupted);
	        
	        AtomicReference<org.springframework.amqp.core.Message> atomicMessage = new AtomicReference<>();
	
	        Awaitility.await().untilAsserted(() -> {
	            org.springframework.amqp.core.Message message = template.receive(DLQ);
	            atomicMessage.set(message);
	    	});
	        org.springframework.amqp.core.Message received = atomicMessage.get();
            assertThat(received).isNotNull();
            assertThat(received.getMessageProperties().getHeader("x-death").toString()).isNotBlank();
            assertThat(received.getBody()).isEqualTo(corrupted.getBytes(StandardCharsets.UTF_8));
    	}
    }

    @AfterAll
    void clear() {    	
        admin.deleteExchange(EXCHANGE);
        admin.deleteExchange("DLX");
        admin.deleteQueue(INPUT_QUEUE);
        admin.deleteQueue(DLQ);
    }
    
}

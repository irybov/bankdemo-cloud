package com.github.irybov.notificator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
// import org.springframework.cloud.stream.messaging.Sink;
// import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
// import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
// import org.springframework.messaging.support.ChannelInterceptorAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Disabled
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
// @EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, MessageCollectorAutoConfiguration.class})
public class DeprecatedAppIT {

    // @Autowired
    // private Sink sink;
    @Autowired
    private AmqpAdmin admin;
    @Autowired
    private RabbitTemplate template;
    
    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String EXCHANGE;
    @Value("${spring.cloud.stream.bindings.input.group}")
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
	    DLQ =  INPUT_QUEUE + ".dlq";
	    
        data = new LinkedHashMap<>(2);
        data.put(1, -3.00);
        data.put(2, 44.00);
        json = mapper.writeValueAsString(data);
	}
    
	@Test
	void context_loading(ApplicationContext context) {
	    assertThat(context).isNotNull();
	    assertThat(context.getBean("myListener", MyListener.class)).isNotNull();
//	    assertThat(context.getBean("sink", Sink.class)).isNotNull();
	    // assertThat(context.getBean(Sink.INPUT, SubscribableChannel.class)).isNotNull();
	    }
    
    @Test
    void message_consuming(ApplicationContext context) throws JsonProcessingException {
	
        // AbstractMessageChannel input = (AbstractMessageChannel) this.sink.input();
        final AtomicReference<Message<?>> atomicMessage = new AtomicReference<>();
        // ChannelInterceptor assertionInterceptor = new ChannelInterceptorAdapter() {
    /*	    
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                atomicMessage.set(new GenericMessage<>(message.getPayload()));
                return message;
            }
    */	    
        // @Override
        // public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        //     atomicMessage.set(message);
        //     super.afterSendCompletion(message, channel, sent, ex);
        // }
        // };
        // input.addInterceptor(assertionInterceptor);

        // this.template.convertAndSend(EXCHANGE, KEY, json);
        // Awaitility.await().untilAsserted(() -> {
        //         Message<?> message = atomicMessage.get();
        //         assertThat(message).isNotNull();
        //         assertThat(message).hasFieldOrPropertyWithValue("payload", json.getBytes());
        // });
    }

    @Test
    void dead_letters_queue() throws JsonProcessingException {

//	this.admin.declareBinding(new Binding(DLQ, DestinationType.QUEUE, "DLX", KEY, null));
        this.template.setReceiveTimeout(-1);	
        this.template.convertAndSend(EXCHANGE, KEY, "OMG");

        final AtomicReference<org.springframework.amqp.core.Message> atomicMessage = new AtomicReference<>();
        atomicMessage.set(template.receive(DLQ));
        Awaitility.await().untilAsserted(() -> {
            org.springframework.amqp.core.Message received = atomicMessage.get();
            assertThat(received).isNotNull();
//            assertThat(received.getMessageProperties().getConsumerQueue()).isEqualTo(INPUT_QUEUE);
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

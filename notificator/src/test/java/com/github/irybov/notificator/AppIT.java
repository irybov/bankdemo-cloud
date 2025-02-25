package com.github.irybov.notificator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
import org.springframework.cloud.stream.test.binder.TestSupportBinder;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
@EnableAutoConfiguration(exclude = 
	{TestSupportBinderAutoConfiguration.class, MessageCollectorAutoConfiguration.class})
//@Import(TestSupportBinder.class)
public class AppIT {

//    @Autowired
//    private Processor processor;
//    @Autowired
//    private TestSupportBinder binder;
    @Autowired
    private ObjectMapper mapper;
    
    @Autowired
    private AmqpAdmin admin;
    @Autowired
    private RabbitTemplate template;
    
    private static final String EXCHANGE = "bankdemo.test-exchange";
    
	@Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
    
    @Test
    void message_consuming() throws JsonProcessingException {
    	
        Map<Integer, Double> data = new LinkedHashMap<>();
        data.put(1, -3.00);
        data.put(2, 44.00);
        String json = mapper.writeValueAsString(data);
//    	processor.output().send(MessageBuilder.withPayload(data).build());
    	
//        Queue<Message<?>> queue = collector.forChannel(processor.output());
//        Message<String> message = (Message<String>) queue.peek();
//        assertThat(message.getPayload()).isEqualTo(mapper.writeValueAsString(data));
        
//        assertThat(queue.isEmpty()).isTrue();
    	
        // bind an autodelete queue to the destination exchange
    	org.springframework.amqp.core.Queue queue = this.admin.declareQueue();
        this.admin.declareBinding(new Binding(queue.getName(), DestinationType.QUEUE, 
        		EXCHANGE, "#", null));

        this.template.convertAndSend(EXCHANGE, "#", json);
//        this.processor.output().send(new GenericMessage<>(data));
/*        
        MessageCollector collector = binder.messageCollector();
        Queue<Message<?>> queue2 = collector.forChannel(binder.getChannelForName("output"));
        Message<String> message = (Message<String>) queue2.peek();
        assertThat(message.getPayload()).isEqualTo(json);
*/
        this.template.setReceiveTimeout(-1);
        org.springframework.amqp.core.Message received = template.receive(queue.getName());
        assertThat(new String(received.getBody())).isEqualTo(json);
        
        this.admin.deleteExchange(EXCHANGE);
//        this.admin.deleteExchange("input");
    }

}

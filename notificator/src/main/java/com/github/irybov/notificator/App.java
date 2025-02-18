package com.github.irybov.notificator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.handler.annotation.Payload;

@SpringBootApplication
@EnableEurekaClient
@EnableBinding(Processor.class)
public class App
{
	public static void main( String[] args )
    {
    	SpringApplication.run(App.class, args);
    }

	@StreamListener(Processor.INPUT)
	public void listener(@Payload String data) {
		System.out.println(data);
	}
}

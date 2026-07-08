package com.github.irybov.notificator;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(Sink.class)
public class MyListener {
    
	@StreamListener(Sink.INPUT)
	public void recieve(@Payload String data) {
		
	    if(data.length() > 3) System.out.println(data);
	    else throw new RuntimeException("OMG");
	}

}

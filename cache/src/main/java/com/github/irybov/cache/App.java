package com.github.irybov.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@SpringBootApplication
@EnableCaching
public class App {

	public static void main(String[] args) 
	{
		SpringApplication.run(App.class, args);
	}

    @Bean
    public HazelcastInstance hazelcastInstance() {

        Config config = new Config();
        config.setClusterName("home");
        config.getNetworkConfig().setPort(5701).getInterfaces().setEnabled(false);
        config.addMapConfig(new MapConfig("accounts"));
        config.addMapConfig(new MapConfig("bills"));
        config.addMapConfig(new MapConfig("dtos"));
        config.addMapConfig(new MapConfig("sets"));

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        return Hazelcast.newHazelcastInstance(config);
    }

}

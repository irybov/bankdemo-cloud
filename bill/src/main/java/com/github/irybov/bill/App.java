package com.github.irybov.bill;

// import org.infinispan.client.hotrod.RemoteCacheManager;
// import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
// import org.infinispan.spring.remote.provider.SpringRemoteCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.cache.autoconfigure.metrics.CacheMetricsAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.ExposeHazelcastObjects;
import com.hazelcast.spring.boot.HazelcastBoot4ObjectExtractionAutoConfiguration;
import com.hazelcast.spring.cache.HazelcastCacheManager;

// @SpringBootApplication(exclude = {CacheMetricsAutoConfiguration.class})
@SpringBootApplication(exclude = HazelcastBoot4ObjectExtractionAutoConfiguration.class)
@EnableDiscoveryClient
@EnableCaching
// @ExposeHazelcastObjects(unexposeAll = true)
public class App 
{
    // @Value("#{ '${infinispan.client.hotrod.server_list}'.split(':')[0] }")
	// private String host;
	// @Value("#{ T(Integer).parseInt('${infinispan.client.hotrod.server_list}'.split(':')[1]) }")
	// private int port;
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class, args);
    }

    @Bean
    public ClientConfig clientConfig() {
        ClientConfig config = new ClientConfig();
        config.setClusterName("home");
        config.getNetworkConfig().addAddress("127.0.0.1:5701");
        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return HazelcastClient.newHazelcastClient(clientConfig());
    }
/* 
    @Bean
    public RemoteCacheManager remoteCacheManager() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
               .host(host)
               .port(port);
        return new RemoteCacheManager(builder.build());
    }

    @Bean
    public SpringRemoteCacheManager cacheManager(RemoteCacheManager remoteCacheManager) {
        return new SpringRemoteCacheManager(remoteCacheManager);
    }
 */
    @Bean
    public CacheManager cacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }
}

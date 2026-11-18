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
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.ExposeHazelcastObjects;
//import com.hazelcast.spring.boot.HazelcastBoot4ObjectExtractionAutoConfiguration;
import com.hazelcast.spring.cache.HazelcastCacheManager;

// @SpringBootApplication(exclude = {CacheMetricsAutoConfiguration.class})
@SpringBootApplication
//@SpringBootApplication(exclude = HazelcastBoot4ObjectExtractionAutoConfiguration.class)
@EnableDiscoveryClient
@EnableCaching(order = 0)
@EnableTransactionManagement(order = 1)
// @ExposeHazelcastObjects(unexposeAll = true)

public class App
{
//	@Value("${hazelcast.cluster.name}") String cluster;
//	@Value("${hazelcast.server.addresses}") String[] addresses;
    // @Value("#{ '${infinispan.client.hotrod.server_list}'.split(':')[0] }")
	// private String host;
	// @Value("#{ T(Integer).parseInt('${infinispan.client.hotrod.server_list}'.split(':')[1]) }")
	// private int port;
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class, args);
    }

    @Bean
    public ClientConfig clientConfig(@Value("${hazelcast.cluster.name}") String cluster, 
    								 @Value("${hazelcast.server.addresses}") String[] addresses) {
        ClientConfig config = new ClientConfig();
        config.setClusterName(cluster);
        config.getNetworkConfig().addAddress(addresses);
        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance(ClientConfig clientConfig) {
        return HazelcastClient.newHazelcastClient(clientConfig);
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
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }
}

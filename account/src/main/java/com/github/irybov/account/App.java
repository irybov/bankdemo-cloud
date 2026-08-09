package com.github.irybov.account;

import jakarta.validation.Validator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
//import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.boot.HazelcastBoot4ObjectExtractionAutoConfiguration;
import com.hazelcast.spring.cache.HazelcastCacheManager;

@SpringBootApplication(exclude = HazelcastBoot4ObjectExtractionAutoConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
public class App 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class, args);
    }
/*    
	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
*/	
	@Bean
	public Validator localValidatorFactoryBean() {
	    LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();	    
 		bean.setProviderClass(org.apache.bval.jsr.ApacheValidationProvider.class);
// 		bean.setProviderClass(org.hibernate.validator.HibernateValidator.class);	   
	    return bean;
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
	
    @Bean
    public CacheManager cacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }
}

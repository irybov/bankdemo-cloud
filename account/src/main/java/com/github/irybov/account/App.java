package com.github.irybov.account;

import javax.validation.Validator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
//import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableEurekaClient
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
}

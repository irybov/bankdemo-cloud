package com.github.irybov.operation;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;

@SpringBootApplication
@EnableDiscoveryClient
//@EnableJdbcRepositories
@EnableFeignClients
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
 /*    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
      		  .addModule(new PageJacksonModule())
      		  .addModule(new SortJacksonModule())
      		  .build();
    }

	@Bean
    public ServerHttpMessageConvertersCustomizer jacksonServerConverterCustomizer() {
        return httpMessageConverters -> {
            httpMessageConverters.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper()));
        };
    }
     */
    @Bean
    @Primary
    public SQLQueryFactory queryFactory(DataSource dataSource) {
		SQLTemplates templates = new PostgreSQLTemplates(){{setPrintSchema(true);}};
		Configuration configuration = new Configuration(templates);
		configuration.setExceptionTranslator(new SpringExceptionTranslator());
		SpringConnectionProvider provider = new SpringConnectionProvider(dataSource);
		return new SQLQueryFactory(configuration, provider);
    }
    
}

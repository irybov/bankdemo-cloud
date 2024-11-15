package com.github.irybov.operation;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;

@SpringBootApplication
@EnableEurekaClient
//@EnableJdbcRepositories
public class App 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class, args);
    }
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
      		  .addModule(new PageJacksonModule())
      		  .addModule(new SortJacksonModule())
      		  .build();
    }
    
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

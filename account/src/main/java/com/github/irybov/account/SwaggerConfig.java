package com.github.irybov.account;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI customOpenAPI() {
            return new OpenAPI()
                    .info(new Info()
                            .title("Spring Cloud (bank demo)")
                            .version("1.2.1")
                            .description("Swagger configuration for application")
                            .license(new License().name("Apache 2.0").url("https://springdoc.org"))
                            .contact(new Contact()
                                        .name("Ivan Ryabov")
                                        .email("v_cho@list.ru")
                                        .url("https://github.com/irybov")));
        }
        @Bean
        public GroupedOpenApi publicApi() {
                return GroupedOpenApi.builder()
                        .group("public-apis")
                        .packagesToScan("com.github.irybov.account")
                        .build();
        }
}

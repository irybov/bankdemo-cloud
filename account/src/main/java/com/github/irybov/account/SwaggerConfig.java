package com.github.irybov.account;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI customOpenAPI() {
        	final String securitySchemeName = "bearerAuth";
            return new OpenAPI()
                    .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                    .components(new Components()
                            .addSecuritySchemes(securitySchemeName,
                                    new SecurityScheme()
                                            .name(securitySchemeName)
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")))
                    .info(new Info()
                            .title("Spring Cloud (bank demo)")
                            .version("1.2.5")
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

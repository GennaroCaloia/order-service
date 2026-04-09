package com.example.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Microservizio di gestione ordini")
                        .version("v1")
                        .contact(new Contact()
                                .name("Platform Team")
                                .email("platform@example.com"))
                        .license(new License()
                                .name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080")
                                .description("Local"),
                        new Server().url("https://api-dev.example.com")
                                .description("Dev")));
    }
}
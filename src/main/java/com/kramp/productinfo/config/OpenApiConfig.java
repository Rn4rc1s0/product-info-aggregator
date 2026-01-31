package com.kramp.productinfo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productInfoAggregatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Information Aggregator API")
                        .description("Backend service that aggregates product information from multiple internal services")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Kramp B2B Platform")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}


package com.myfinance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI myFinanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyFinance API")
                        .description(
                                "Personal finance assessment and advisory platform for Indian users. "
                                        + "6-step wizard (profile, cash flow, net worth, goals, insurance, tax) with AI-powered insights.")
                        .version("1.0.0")
                        .contact(new Contact().name("MyFinancial").url("https://myfinancial.in")))
                .servers(List.of(new Server().url("/").description("Current")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from POST /api/v1/auth/google")));
    }
}

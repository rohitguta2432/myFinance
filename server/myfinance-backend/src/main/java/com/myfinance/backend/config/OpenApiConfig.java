package com.myfinance.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI myFinanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyFinance API")
                        .description("6-step Financial Assessment Wizard REST API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MyFinance Team")
                                .url("https://github.com/rohitguta2432/myFinance")));
    }
}

package com.myfinance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI myFinanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyFinance API")
                        .description("Personal finance assessment and advisory platform for Indian users. " +
                                "6-step wizard (profile, cash flow, net worth, goals, insurance, tax) with AI-powered insights.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MyFinancial")
                                .url("https://app.myfinancial.in")))
                .servers(List.of(
                        new Server().url("/").description("Current")));
    }

    @Bean
    public OperationCustomizer addUserIdHeader() {
        return (operation, handlerMethod) -> {
            // Auto-document X-User-Id header on endpoints that use it
            var params = handlerMethod.getMethodParameters();
            for (var param : params) {
                var header = param.getParameterAnnotation(
                        org.springframework.web.bind.annotation.RequestHeader.class);
                if (header != null && "X-User-Id".equals(header.value())) {
                    operation.addParametersItem(new Parameter()
                            .in("header")
                            .name("X-User-Id")
                            .description("Authenticated user ID (from Google OAuth)")
                            .required(false)
                            .schema(new io.swagger.v3.oas.models.media.StringSchema()
                                    .example("1")));
                    break;
                }
            }
            return operation;
        };
    }
}

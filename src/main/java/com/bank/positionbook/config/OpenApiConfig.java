package com.bank.positionbook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.bank.positionbook.constants.ApiConstants.OPEN_API_DESCRIPTION;
import static com.bank.positionbook.constants.ApiConstants.OPEN_API_TITLE;
import static com.bank.positionbook.constants.ApiConstants.OPEN_API_VERSION;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI positionBookOpenApi() {
        return new OpenAPI().info(new Info()
                .title(OPEN_API_TITLE)
                .version(OPEN_API_VERSION)
                .description(OPEN_API_DESCRIPTION));
    }
}

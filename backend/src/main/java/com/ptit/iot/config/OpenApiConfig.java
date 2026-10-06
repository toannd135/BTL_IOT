package com.ptit.iot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui/index.html, raw spec at /v3/api-docs. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI iotMonitorOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("IoT Monitor Backend API")
                .description("REST, MQTT and WebSocket contracts for the IoT device monitoring system (chapter 3.3).")
                .version("1.0.0"));
    }
}

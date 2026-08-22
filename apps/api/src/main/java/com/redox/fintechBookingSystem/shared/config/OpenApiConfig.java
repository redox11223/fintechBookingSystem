package com.redox.fintechBookingSystem.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI citafinOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("CitaFin API")
            .version("v1")
            .description("API para agendar y administrar consultorías financieras."));
  }
}

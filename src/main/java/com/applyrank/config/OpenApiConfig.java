package com.applyrank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html - click "Authorize", paste the token from /api/auth/login, try every endpoint. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI applyRankOpenApi() {
        return new OpenAPI()
                .info(new Info().title("ApplyRank API").version("1.0")
                        .description("Verified-rank hiring platform. Login first, then press Authorize and paste the JWT."))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}

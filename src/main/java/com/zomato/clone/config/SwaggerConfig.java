package com.zomato.clone.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // 1. Define the API Information
                .info(new Info()
                        .title("Zomato Clone API")
                        .version("1.0")
                        .description("REST API documentation for the Zomato Clone application. Includes secure payment and order processing."))

                // 2. Define the Security Scheme (Tells Swagger how to format the header)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))

                // 3. Apply the Security Requirement Globally (Adds the lock icon to endpoints)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}

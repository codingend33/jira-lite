package com.jiralite.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;

import com.jiralite.backend.filter.TraceIdFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuration for OpenAPI/Swagger and filters.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure OpenAPI with basic info.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("Jira Lite API")
                        .version("1.0.0")
                        .description("Day 4 auth + RBAC + tenant context API"));
    }

    /**
     * Expose all REST endpoints in OpenAPI.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .pathsToExclude("/actuator/**") // exclude actuator endpoints from OpenAPI
                .build();
    }

    /**
     * Register TraceIdFilter with highest priority to ensure it runs first.
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter traceIdFilter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(traceIdFilter);
        registration.setOrder(-100); // Execute before other filters
        return registration;
    }
}

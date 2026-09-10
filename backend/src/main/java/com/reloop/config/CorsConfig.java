package com.reloop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Without this, the browser blocks every request from the React dev server
 * (http://localhost:5173) to this API (http://localhost:8080) because
 * they're different origins - this is the one piece of "security" config
 * this project needs, and it's about browser same-origin policy, not
 * authentication.
 *
 * Allowed origins come from application.properties
 * (reloop.cors.allowed-origins) so the deployed frontend's real domain can
 * be added later without touching this class - just set the
 * RELOOP_CORS_ALLOWED_ORIGINS environment variable in production.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${reloop.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}

package com.fintrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebMvc
public class WebConfig {

    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Allow all endpoints
                        .allowedOrigins(baseUrl) // Use the configured base URL
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Allow all common HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .exposedHeaders("Authorization") // Expose the Authorization header
                        .allowCredentials(true) // Allow cookies and credentials
                        .maxAge(3600); // Cache preflight requests for 1 hour
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                // Add StringHttpMessageConverter first
                StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
                List<MediaType> stringMediaTypes = new ArrayList<>();
                stringMediaTypes.add(MediaType.TEXT_PLAIN);
                stringMediaTypes.add(MediaType.TEXT_HTML);
                stringMediaTypes.add(MediaType.APPLICATION_JSON);
                stringConverter.setSupportedMediaTypes(stringMediaTypes);
                converters.add(stringConverter);

                // Add JSON converter
                MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
                List<MediaType> jsonMediaTypes = new ArrayList<>();
                jsonMediaTypes.add(MediaType.APPLICATION_JSON);
                jsonMediaTypes.add(new MediaType("application", "json", StandardCharsets.UTF_8));
                jsonMediaTypes.add(new MediaType("application", "*+json", StandardCharsets.UTF_8));
                jsonConverter.setSupportedMediaTypes(jsonMediaTypes);
                converters.add(jsonConverter);
            }
        };
    }
}
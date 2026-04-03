package de.tozwhv.vereinskasse.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Macht den lokalen Ordner unter /media/** erreichbar
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:/opt/vereinskasse/uploads/")
                .setCachePeriod(31536000); // 1 Jahr Caching für den Browser
    }
}
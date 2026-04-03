package de.tozwhv.vereinskasse.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + path + "/";

        // Macht den lokalen Ordner unter /media/** erreichbar
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location)
                .setCachePeriod(31536000); // 1 Jahr Caching für den Browser
    }
}
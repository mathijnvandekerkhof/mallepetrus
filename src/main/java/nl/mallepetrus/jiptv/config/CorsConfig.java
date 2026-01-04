package nl.mallepetrus.jiptv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://admin.mallepetrus.nl",    // Production admin dashboard
            "http://localhost:3000",           // Local development
            "https://mallepetrus.nl",          // Production main domain
            "http://localhost:*",              // Any localhost port for development
            "*"                                // Temporary: allow all origins for debugging
        ));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (for cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        // Expose headers that the client can access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
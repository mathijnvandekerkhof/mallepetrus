package nl.mallepetrus.jiptv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for serving static resources and admin dashboard
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve admin dashboard static files
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("file:/app/static/admin/")
                .setCachePeriod(3600); // 1 hour cache for static assets
        
        // Serve Next.js static assets with longer cache
        registry.addResourceHandler("/admin/_next/static/**")
                .addResourceLocations("file:/app/static/admin/_next/static/")
                .setCachePeriod(31536000); // 1 year cache for immutable assets
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to admin dashboard
        registry.addRedirectViewController("/", "/admin/");
    }
}
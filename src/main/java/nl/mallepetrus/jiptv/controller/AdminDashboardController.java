package nl.mallepetrus.jiptv.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving the integrated admin dashboard
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private static final String ADMIN_STATIC_PATH = "/app/static/admin";

    /**
     * Serve the main admin dashboard page
     */
    @GetMapping({"", "/", "/login", "/setup", "/dashboard/**"})
    public ResponseEntity<Resource> serveAdminDashboard() {
        try {
            Path indexPath = Paths.get(ADMIN_STATIC_PATH, "index.html");
            if (Files.exists(indexPath)) {
                Resource resource = new org.springframework.core.io.FileSystemResource(indexPath.toFile());
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(resource);
            }
        } catch (Exception e) {
            // Fallback to classpath resource
        }
        
        // Fallback to serving a simple HTML page
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/admin-fallback.html"));
    }

    /**
     * Serve static assets for the admin dashboard
     */
    @GetMapping("/_next/static/**")
    public ResponseEntity<Resource> serveNextStaticAssets(@PathVariable String path) {
        try {
            Path assetPath = Paths.get(ADMIN_STATIC_PATH, "_next", "static", path);
            if (Files.exists(assetPath)) {
                Resource resource = new org.springframework.core.io.FileSystemResource(assetPath.toFile());
                
                // Determine content type based on file extension
                String contentType = getContentType(path);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                        .body(resource);
            }
        } catch (Exception e) {
            // Log error but don't expose details
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Serve other static assets (CSS, JS, images, etc.)
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveStaticAssets(@PathVariable String filename) {
        try {
            Path assetPath = Paths.get(ADMIN_STATIC_PATH, filename);
            if (Files.exists(assetPath)) {
                Resource resource = new org.springframework.core.io.FileSystemResource(assetPath.toFile());
                
                // Determine content type based on file extension
                String contentType = getContentType(filename);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(resource);
            }
        } catch (Exception e) {
            // Log error but don't expose details
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Determine content type based on file extension
     */
    private String getContentType(String filename) {
        if (filename.endsWith(".js")) {
            return "application/javascript";
        } else if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".html")) {
            return "text/html";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (filename.endsWith(".ico")) {
            return "image/x-icon";
        } else if (filename.endsWith(".json")) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }
}
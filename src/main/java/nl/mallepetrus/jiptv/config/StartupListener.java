package nl.mallepetrus.jiptv.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class StartupListener {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    private final Environment environment;

    public StartupListener(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "default";
        
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    🚀 JIPTV STARTED SUCCESSFULLY             ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Started at: {}                              ║", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("║ Profile:    {}                                        ║", 
                String.format("%-43s", profile));
        log.info("║ Port:       {}                                           ║", 
                String.format("%-47s", serverPort));
        log.info("║ Context:    {}                                         ║", 
                String.format("%-45s", contextPath));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ 🔐 Authentication:     JWT + MFA                            ║");
        log.info("║ 🛡️  Zero Trust:        Enabled                              ║");
        log.info("║ 📱 Device Pairing:     WebOS TV Support                    ║");
        log.info("║ 🎬 Stream Management:  FFmpeg + Transcoding                ║");
        log.info("║ 📊 Health Check:       {}{}                    ║", 
                "http://localhost:" + serverPort + contextPath, "/actuator/health");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        
        // Log important endpoints
        log.info("📍 Available endpoints:");
        log.info("   • Setup:      {}/setup/status", contextPath);
        log.info("   • Auth:       {}/auth/login", contextPath);
        log.info("   • Health:     {}/actuator/health", contextPath);
        log.info("   • Streams:    {}/streams", contextPath);
        log.info("   • Devices:    {}/device-pairing", contextPath);
    }
}
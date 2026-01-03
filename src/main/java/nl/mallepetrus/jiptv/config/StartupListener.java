package nl.mallepetrus.jiptv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class StartupListener {

    private static final Logger log = LoggerFactory.getLogger(StartupListener.class);

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
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    🚀 JIPTV STARTED SUCCESSFULLY             ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Started at: {}                              ║", timestamp);
        log.info("║ Profile:    {}                                        ║", profile);
        log.info("║ Port:       {}                                           ║", serverPort);
        log.info("║ Context:    {}                                         ║", contextPath);
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ 🔐 Authentication:     JWT + MFA                            ║");
        log.info("║ 🛡️  Zero Trust:        Enabled                              ║");
        log.info("║ 📱 Device Pairing:     WebOS TV Support                    ║");
        log.info("║ 🎬 Stream Management:  FFmpeg + Transcoding                ║");
        log.info("║ 📊 Health Check:       localhost:{}{}/actuator/health      ║", 
                serverPort, contextPath);
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
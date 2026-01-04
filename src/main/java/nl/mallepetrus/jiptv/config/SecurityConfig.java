package nl.mallepetrus.jiptv.config;

import nl.mallepetrus.jiptv.security.CustomUserDetailsService;
import nl.mallepetrus.jiptv.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Setup endpoints - accessible without authentication
                .requestMatchers("/setup/**").permitAll()
                // Authentication endpoints
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                // Registration endpoints - accessible without authentication
                .requestMatchers("/register/**").permitAll()
                .requestMatchers("/invitations/code/**").permitAll()
                // Device pairing endpoints - WebOS TV app access
                .requestMatchers("/device-pairing/pair").permitAll()
                .requestMatchers("/device-pairing/heartbeat").permitAll()
                .requestMatchers("/device-pairing/validate/**").permitAll()
                // Health check endpoints
                .requestMatchers("/actuator/**").permitAll()
                // MFA endpoints require authentication
                .requestMatchers("/mfa/**").authenticated()
                // Zero Trust endpoints require authentication
                .requestMatchers("/zero-trust/**").authenticated()
                // Invitation management requires authentication (admin only)
                .requestMatchers("/invitations/**").authenticated()
                // Device pairing management requires authentication
                .requestMatchers("/device-pairing/**").authenticated()
                // Stream management endpoints require authentication
                .requestMatchers("/streams/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
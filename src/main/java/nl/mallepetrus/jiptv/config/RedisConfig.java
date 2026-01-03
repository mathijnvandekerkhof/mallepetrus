package nl.mallepetrus.jiptv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    // Redis Database Allocation:
    // Database 0: General cache (user sessions, etc.)
    // Database 1: Zero Trust data (risk scores, device fingerprints)
    // Database 2: Rate limiting (API throttling)
    // Database 3: Stream metadata (track info, preferences)
    // Database 4: Stream URLs (transcoded content URLs)

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return createConnectionFactory(0); // Default database
    }

    @Bean("zeroTrustRedisConnectionFactory")
    public RedisConnectionFactory zeroTrustRedisConnectionFactory() {
        return createConnectionFactory(1);
    }

    @Bean("rateLimitRedisConnectionFactory")
    public RedisConnectionFactory rateLimitRedisConnectionFactory() {
        return createConnectionFactory(2);
    }

    @Bean("streamMetadataRedisConnectionFactory")
    public RedisConnectionFactory streamMetadataRedisConnectionFactory() {
        return createConnectionFactory(3);
    }

    @Bean("streamUrlRedisConnectionFactory")
    public RedisConnectionFactory streamUrlRedisConnectionFactory() {
        return createConnectionFactory(4);
    }

    private RedisConnectionFactory createConnectionFactory(int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(database);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        return createRedisTemplate(connectionFactory);
    }

    @Bean("zeroTrustRedisTemplate")
    public RedisTemplate<String, Object> zeroTrustRedisTemplate() {
        return createRedisTemplate(zeroTrustRedisConnectionFactory());
    }

    @Bean("rateLimitRedisTemplate")
    public RedisTemplate<String, Object> rateLimitRedisTemplate() {
        return createRedisTemplate(rateLimitRedisConnectionFactory());
    }

    @Bean("streamMetadataRedisTemplate")
    public RedisTemplate<String, Object> streamMetadataRedisTemplate() {
        return createRedisTemplate(streamMetadataRedisConnectionFactory());
    }

    @Bean("streamUrlRedisTemplate")
    public RedisTemplate<String, Object> streamUrlRedisTemplate() {
        return createRedisTemplate(streamUrlRedisConnectionFactory());
    }

    private RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values with Java 8 time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
# Multi-stage build for JIPTV Spring Boot application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy Maven files
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Install FFmpeg for transcoding support
RUN apk add --no-cache ffmpeg wget

# Add non-root user for security
RUN addgroup -g 1001 -S jiptv && \
    adduser -S jiptv -u 1001 -G jiptv

WORKDIR /app

# Create directories for transcoding output
RUN mkdir -p /app/transcoded/hls /app/transcoded/transcoded /app/logs && \
    chown -R jiptv:jiptv /app

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to jiptv user
RUN chown -R jiptv:jiptv /app

# Switch to non-root user
USER jiptv

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application with optimized JVM settings for production
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1g", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-XX:+OptimizeStringConcat", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
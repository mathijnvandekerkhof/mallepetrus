package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.StreamSession;
import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.TvDevice;
import nl.mallepetrus.jiptv.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {

    // Find session by token
    Optional<StreamSession> findBySessionTokenAndActiveTrue(String sessionToken);

    // Find active sessions for user
    List<StreamSession> findByUserAndActiveTrueOrderByStartedAtDesc(User user);

    List<StreamSession> findByUserIdAndActiveTrueOrderByStartedAtDesc(Long userId);

    // Find active sessions for device
    List<StreamSession> findByTvDeviceAndActiveTrueOrderByStartedAtDesc(TvDevice tvDevice);

    List<StreamSession> findByTvDeviceIdAndActiveTrueOrderByStartedAtDesc(Long tvDeviceId);

    // Find active sessions for stream source
    List<StreamSession> findByStreamSourceAndActiveTrueOrderByStartedAtDesc(StreamSource streamSource);

    List<StreamSession> findByStreamSourceIdAndActiveTrueOrderByStartedAtDesc(Long streamSourceId);

    // Find all sessions for user (including ended)
    Page<StreamSession> findByUserOrderByStartedAtDesc(User user, Pageable pageable);

    Page<StreamSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    // Find sessions by date range
    List<StreamSession> findByStartedAtBetweenOrderByStartedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // Find expired sessions (heartbeat timeout)
    @Query("SELECT s FROM StreamSession s WHERE s.active = true AND " +
           "s.lastHeartbeatAt < :timeoutThreshold")
    List<StreamSession> findExpiredSessions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    // Find long-running sessions
    @Query("SELECT s FROM StreamSession s WHERE s.active = true AND " +
           "s.startedAt < :thresholdTime")
    List<StreamSession> findLongRunningSessions(@Param("thresholdTime") LocalDateTime thresholdTime);

    // Count active sessions
    @Query("SELECT COUNT(s) FROM StreamSession s WHERE s.active = true")
    long countActiveSessions();

    // Count active sessions by user
    @Query("SELECT COUNT(s) FROM StreamSession s WHERE s.user = :user AND s.active = true")
    long countActiveSessionsByUser(@Param("user") User user);

    @Query("SELECT COUNT(s) FROM StreamSession s WHERE s.user.id = :userId AND s.active = true")
    long countActiveSessionsByUserId(@Param("userId") Long userId);

    // Count active sessions by device
    @Query("SELECT COUNT(s) FROM StreamSession s WHERE s.tvDevice = :device AND s.active = true")
    long countActiveSessionsByDevice(@Param("device") TvDevice device);

    // Find concurrent sessions (same user, different devices)
    @Query("SELECT s FROM StreamSession s WHERE s.user = :user AND s.active = true AND " +
           "s.tvDevice != :currentDevice")
    List<StreamSession> findConcurrentSessions(@Param("user") User user, @Param("currentDevice") TvDevice currentDevice);

    // Update heartbeat for session
    @Modifying
    @Query("UPDATE StreamSession s SET s.lastHeartbeatAt = :heartbeatTime WHERE s.sessionToken = :token")
    int updateHeartbeat(@Param("token") String sessionToken, @Param("heartbeatTime") LocalDateTime heartbeatTime);

    // Update playback position
    @Modifying
    @Query("UPDATE StreamSession s SET s.playbackPositionSeconds = :position, s.lastHeartbeatAt = :heartbeatTime " +
           "WHERE s.sessionToken = :token AND s.active = true")
    int updatePlaybackPosition(@Param("token") String sessionToken, 
                              @Param("position") java.math.BigDecimal position,
                              @Param("heartbeatTime") LocalDateTime heartbeatTime);

    // End sessions by criteria
    @Modifying
    @Query("UPDATE StreamSession s SET s.active = false, s.endedAt = :endTime " +
           "WHERE s.active = true AND s.lastHeartbeatAt < :timeoutThreshold")
    int endExpiredSessions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold, 
                          @Param("endTime") LocalDateTime endTime);

    @Modifying
    @Query("UPDATE StreamSession s SET s.active = false, s.endedAt = :endTime " +
           "WHERE s.user = :user AND s.active = true")
    int endAllUserSessions(@Param("user") User user, @Param("endTime") LocalDateTime endTime);

    @Modifying
    @Query("UPDATE StreamSession s SET s.active = false, s.endedAt = :endTime " +
           "WHERE s.tvDevice = :device AND s.active = true")
    int endAllDeviceSessions(@Param("device") TvDevice device, @Param("endTime") LocalDateTime endTime);

    // Statistics queries
    @Query("SELECT s.quality, COUNT(s) FROM StreamSession s GROUP BY s.quality")
    List<Object[]> getSessionStatsByQuality();

    @Query("SELECT DATE(s.startedAt), COUNT(s) FROM StreamSession s " +
           "WHERE s.startedAt >= :fromDate GROUP BY DATE(s.startedAt) ORDER BY DATE(s.startedAt)")
    List<Object[]> getSessionCountsByDate(@Param("fromDate") LocalDateTime fromDate);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (COALESCE(ended_at, CURRENT_TIMESTAMP) - started_at))/60) " +
           "FROM stream_sessions WHERE started_at >= :fromDate", nativeQuery = true)
    Double getAverageSessionDurationMinutes(@Param("fromDate") LocalDateTime fromDate);

    // Find popular streams
    @Query("SELECT s.streamSource, COUNT(s) as sessionCount FROM StreamSession s " +
           "WHERE s.startedAt >= :fromDate GROUP BY s.streamSource ORDER BY sessionCount DESC")
    List<Object[]> getMostPopularStreams(@Param("fromDate") LocalDateTime fromDate, Pageable pageable);

    // Find active users
    @Query("SELECT DISTINCT s.user FROM StreamSession s WHERE s.startedAt >= :fromDate")
    List<User> getActiveUsers(@Param("fromDate") LocalDateTime fromDate);
}
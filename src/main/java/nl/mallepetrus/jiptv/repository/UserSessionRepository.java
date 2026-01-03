package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionTokenAndActiveTrue(String sessionToken);

    List<UserSession> findByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user = :user AND s.active = true")
    long countActiveSessionsForUser(@Param("user") User user);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.active = true AND s.requiresStepUpAuth = true")
    List<UserSession> findSessionsRequiringStepUpAuth(@Param("user") User user);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.expiresAt < :now")
    int deactivateExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.user = :user AND s.id != :currentSessionId")
    int deactivateOtherUserSessions(@Param("user") User user, @Param("currentSessionId") Long currentSessionId);

    void deleteByActiveFalseAndCreatedAtBefore(LocalDateTime before);
}
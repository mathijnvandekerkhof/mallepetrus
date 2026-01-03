package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.SecurityEvent;
import nl.mallepetrus.jiptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByUserOrderByCreatedAtDesc(User user);

    List<SecurityEvent> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime after);

    @Query("SELECT COUNT(s) FROM SecurityEvent s WHERE s.user = :user AND s.eventType = :eventType AND s.createdAt > :since")
    long countEventsByTypeAndUser(@Param("user") User user, 
                                  @Param("eventType") SecurityEvent.SecurityEventType eventType, 
                                  @Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.riskScore) FROM SecurityEvent s WHERE s.user = :user AND s.createdAt > :since")
    Double getAverageRiskScoreForUser(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT s FROM SecurityEvent s WHERE s.user = :user AND s.riskScore >= :minRiskScore AND s.createdAt > :since ORDER BY s.createdAt DESC")
    List<SecurityEvent> findHighRiskEvents(@Param("user") User user, 
                                          @Param("minRiskScore") Integer minRiskScore, 
                                          @Param("since") LocalDateTime since);

    void deleteByCreatedAtBefore(LocalDateTime before);
}
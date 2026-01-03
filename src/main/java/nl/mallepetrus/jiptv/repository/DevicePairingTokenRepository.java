package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.DevicePairingToken;
import nl.mallepetrus.jiptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DevicePairingTokenRepository extends JpaRepository<DevicePairingToken, Long> {
    
    Optional<DevicePairingToken> findByPairingToken(String pairingToken);
    
    List<DevicePairingToken> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT t FROM DevicePairingToken t WHERE t.user = :user AND t.used = false AND t.expiresAt > :now")
    List<DevicePairingToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM DevicePairingToken t WHERE t.expiresAt < :now AND t.used = false")
    List<DevicePairingToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(t) FROM DevicePairingToken t WHERE t.user = :user AND t.used = false AND t.expiresAt > :now")
    long countValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    void deleteByUserAndUsedTrueAndCreatedAtBefore(User user, LocalDateTime before);
}
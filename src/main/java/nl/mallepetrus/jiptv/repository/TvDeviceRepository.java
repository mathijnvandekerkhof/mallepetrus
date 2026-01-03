package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.TvDevice;
import nl.mallepetrus.jiptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TvDeviceRepository extends JpaRepository<TvDevice, Long> {
    
    List<TvDevice> findByUserOrderByPairedAtDesc(User user);
    
    List<TvDevice> findByUserAndActiveTrue(User user);
    
    Optional<TvDevice> findByMacAddressHash(String macAddressHash);
    
    Optional<TvDevice> findByMacAddress(String macAddress);
    
    boolean existsByMacAddress(String macAddress);
    
    boolean existsByMacAddressHash(String macAddressHash);
    
    @Query("SELECT COUNT(t) FROM TvDevice t WHERE t.user = :user AND t.active = true")
    long countActiveDevicesByUser(@Param("user") User user);
    
    @Query("SELECT t FROM TvDevice t WHERE t.user = :user AND t.lastSeenAt > :since")
    List<TvDevice> findRecentlyActiveDevices(@Param("user") User user, @Param("since") LocalDateTime since);
    
    @Query("SELECT t FROM TvDevice t WHERE t.lastSeenAt < :threshold AND t.active = true")
    List<TvDevice> findInactiveDevices(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT t FROM TvDevice t WHERE t.user = :user AND t.id = :deviceId AND t.active = true")
    Optional<TvDevice> findActiveDeviceByUserAndId(@Param("user") User user, @Param("deviceId") Long deviceId);
}
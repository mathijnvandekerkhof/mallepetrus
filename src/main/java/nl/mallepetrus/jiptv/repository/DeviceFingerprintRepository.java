package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.DeviceFingerprint;
import nl.mallepetrus.jiptv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, Long> {

    Optional<DeviceFingerprint> findByFingerprintHash(String fingerprintHash);

    List<DeviceFingerprint> findByUserOrderByLastSeenAtDesc(User user);

    List<DeviceFingerprint> findByUserAndTrustedTrue(User user);

    @Query("SELECT COUNT(d) FROM DeviceFingerprint d WHERE d.user = :user AND d.lastSeenAt > :since")
    long countActiveDevicesForUser(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT d FROM DeviceFingerprint d WHERE d.user = :user AND d.lastSeenAt < :before")
    List<DeviceFingerprint> findInactiveDevices(@Param("user") User user, @Param("before") LocalDateTime before);

    void deleteByUserAndLastSeenAtBefore(User user, LocalDateTime before);
}
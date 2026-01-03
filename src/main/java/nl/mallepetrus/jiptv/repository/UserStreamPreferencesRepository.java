package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.User;
import nl.mallepetrus.jiptv.entity.UserStreamPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStreamPreferencesRepository extends JpaRepository<UserStreamPreferences, Long> {

    // Find preferences by user and stream
    Optional<UserStreamPreferences> findByUserAndStreamSource(User user, StreamSource streamSource);

    Optional<UserStreamPreferences> findByUserIdAndStreamSourceId(Long userId, Long streamSourceId);

    // Find all preferences for a user
    List<UserStreamPreferences> findByUserOrderByUpdatedAtDesc(User user);

    List<UserStreamPreferences> findByUserIdOrderByUpdatedAtDesc(Long userId);

    // Find all preferences for a stream source
    List<UserStreamPreferences> findByStreamSourceOrderByUpdatedAtDesc(StreamSource streamSource);

    List<UserStreamPreferences> findByStreamSourceIdOrderByUpdatedAtDesc(Long streamSourceId);

    // Find preferences with specific quality setting
    List<UserStreamPreferences> findByUserAndPreferredQuality(User user, UserStreamPreferences.Quality quality);

    // Find users with subtitle preferences enabled
    @Query("SELECT p FROM UserStreamPreferences p WHERE p.subtitleEnabled = true")
    List<UserStreamPreferences> findUsersWithSubtitlesEnabled();

    // Find preferences with custom track selections
    @Query("SELECT p FROM UserStreamPreferences p WHERE p.user = :user AND " +
           "(p.preferredVideoTrack IS NOT NULL OR p.preferredAudioTrack IS NOT NULL OR " +
           "p.preferredSubtitleTrack IS NOT NULL)")
    List<UserStreamPreferences> findUserPreferencesWithCustomTracks(@Param("user") User user);

    // Count preferences by quality setting
    @Query("SELECT p.preferredQuality, COUNT(p) FROM UserStreamPreferences p GROUP BY p.preferredQuality")
    List<Object[]> countPreferencesByQuality();

    // Find most popular quality setting
    @Query("SELECT p.preferredQuality FROM UserStreamPreferences p " +
           "GROUP BY p.preferredQuality ORDER BY COUNT(p) DESC")
    List<UserStreamPreferences.Quality> findMostPopularQualitySettings();

    // Check if user has any preferences
    boolean existsByUser(User user);

    boolean existsByUserId(Long userId);

    // Check if user has preferences for specific stream
    boolean existsByUserAndStreamSource(User user, StreamSource streamSource);

    boolean existsByUserIdAndStreamSourceId(Long userId, Long streamSourceId);

    // Delete preferences for a user
    void deleteByUser(User user);

    void deleteByUserId(Long userId);

    // Delete preferences for a stream source
    void deleteByStreamSource(StreamSource streamSource);

    void deleteByStreamSourceId(Long streamSourceId);

    // Statistics
    @Query("SELECT COUNT(DISTINCT p.user) FROM UserStreamPreferences p")
    long countUsersWithPreferences();

    @Query("SELECT COUNT(DISTINCT p.streamSource) FROM UserStreamPreferences p")
    long countStreamsWithPreferences();
}
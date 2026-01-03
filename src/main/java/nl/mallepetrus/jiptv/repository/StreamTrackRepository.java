package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.StreamTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreamTrackRepository extends JpaRepository<StreamTrack, Long> {

    // Find tracks by stream source
    List<StreamTrack> findByStreamSourceOrderByTrackTypeAscTrackIndexAsc(StreamSource streamSource);

    // Find tracks by stream source ID
    List<StreamTrack> findByStreamSourceIdOrderByTrackTypeAscTrackIndexAsc(Long streamSourceId);

    // Find tracks by type
    List<StreamTrack> findByStreamSourceAndTrackTypeOrderByTrackIndexAsc(
            StreamSource streamSource, StreamTrack.TrackType trackType);

    List<StreamTrack> findByStreamSourceIdAndTrackTypeOrderByTrackIndexAsc(
            Long streamSourceId, StreamTrack.TrackType trackType);

    // Find default tracks
    List<StreamTrack> findByStreamSourceAndIsDefaultTrueOrderByTrackTypeAsc(StreamSource streamSource);

    // Find tracks by language
    List<StreamTrack> findByStreamSourceAndLanguageOrderByTrackTypeAscTrackIndexAsc(
            StreamSource streamSource, String language);

    // Find WebOS compatible tracks
    List<StreamTrack> findByStreamSourceAndWebosCompatibleTrueOrderByTrackTypeAscTrackIndexAsc(
            StreamSource streamSource);

    // Find tracks requiring transcoding
    List<StreamTrack> findByStreamSourceAndTranscodingRequiredTrueOrderByTrackTypeAscTrackIndexAsc(
            StreamSource streamSource);

    // Find video tracks with specific resolution
    @Query("SELECT t FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.trackType = 'VIDEO' AND t.width = :width AND t.height = :height " +
           "ORDER BY t.trackIndex ASC")
    List<StreamTrack> findVideoTracksByResolution(
            @Param("streamSource") StreamSource streamSource,
            @Param("width") Integer width,
            @Param("height") Integer height);

    // Find audio tracks by channel count
    @Query("SELECT t FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.trackType = 'AUDIO' AND t.channels = :channels " +
           "ORDER BY t.trackIndex ASC")
    List<StreamTrack> findAudioTracksByChannels(
            @Param("streamSource") StreamSource streamSource,
            @Param("channels") Integer channels);

    // Find tracks by codec
    List<StreamTrack> findByStreamSourceAndCodecNameOrderByTrackTypeAscTrackIndexAsc(
            StreamSource streamSource, String codecName);

    // Find best quality video track
    @Query("SELECT t FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.trackType = 'VIDEO' AND t.webosCompatible = true " +
           "ORDER BY t.width DESC, t.height DESC, t.bitrate DESC")
    Optional<StreamTrack> findBestVideoTrack(@Param("streamSource") StreamSource streamSource);

    // Find best quality audio track
    @Query("SELECT t FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.trackType = 'AUDIO' AND t.webosCompatible = true " +
           "ORDER BY t.channels DESC, t.bitrate DESC, t.sampleRate DESC")
    Optional<StreamTrack> findBestAudioTrack(@Param("streamSource") StreamSource streamSource);

    // Find subtitle tracks by language preference
    @Query("SELECT t FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.trackType = 'SUBTITLE' AND " +
           "(t.language = :preferredLanguage OR t.language = :fallbackLanguage) " +
           "ORDER BY CASE WHEN t.language = :preferredLanguage THEN 0 ELSE 1 END, " +
           "t.isDefault DESC, t.trackIndex ASC")
    List<StreamTrack> findSubtitleTracksByLanguagePreference(
            @Param("streamSource") StreamSource streamSource,
            @Param("preferredLanguage") String preferredLanguage,
            @Param("fallbackLanguage") String fallbackLanguage);

    // Statistics and counts
    @Query("SELECT t.trackType, COUNT(t) FROM StreamTrack t WHERE t.streamSource = :streamSource GROUP BY t.trackType")
    List<Object[]> countTracksByType(@Param("streamSource") StreamSource streamSource);

    @Query("SELECT COUNT(t) FROM StreamTrack t WHERE t.streamSource = :streamSource AND t.webosCompatible = true")
    long countWebosCompatibleTracks(@Param("streamSource") StreamSource streamSource);

    @Query("SELECT COUNT(t) FROM StreamTrack t WHERE t.streamSource = :streamSource AND t.transcodingRequired = true")
    long countTracksRequiringTranscoding(@Param("streamSource") StreamSource streamSource);

    // Find unique languages in stream
    @Query("SELECT DISTINCT t.language FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.language IS NOT NULL ORDER BY t.language")
    List<String> findDistinctLanguagesByStreamSource(@Param("streamSource") StreamSource streamSource);

    // Find unique codecs in stream
    @Query("SELECT DISTINCT t.codecName FROM StreamTrack t WHERE t.streamSource = :streamSource AND " +
           "t.codecName IS NOT NULL ORDER BY t.codecName")
    List<String> findDistinctCodecsByStreamSource(@Param("streamSource") StreamSource streamSource);

    // Delete tracks by stream source (for re-analysis)
    void deleteByStreamSource(StreamSource streamSource);
}
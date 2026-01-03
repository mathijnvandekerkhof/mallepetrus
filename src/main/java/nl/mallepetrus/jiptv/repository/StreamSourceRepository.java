package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.StreamSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StreamSourceRepository extends JpaRepository<StreamSource, Long> {

    // Find by source type
    List<StreamSource> findBySourceTypeAndActiveTrue(StreamSource.SourceType sourceType);

    // Find by file hash (for duplicate detection)
    Optional<StreamSource> findByFileHash(String fileHash);

    // Find active sources
    List<StreamSource> findByActiveTrue();
    Page<StreamSource> findByActiveTrue(Pageable pageable);

    // Find sources that need analysis
    @Query("SELECT s FROM StreamSource s WHERE s.active = true AND " +
           "(s.analyzedAt IS NULL OR s.tracks IS EMPTY)")
    List<StreamSource> findSourcesNeedingAnalysis();

    // Find sources by name (case-insensitive search)
    @Query("SELECT s FROM StreamSource s WHERE s.active = true AND " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<StreamSource> findByNameContainingIgnoreCase(@Param("name") String name);

    // Find sources by content type
    List<StreamSource> findByContentTypeAndActiveTrue(String contentType);

    // Find sources analyzed after a certain date
    List<StreamSource> findByAnalyzedAtAfterAndActiveTrue(LocalDateTime date);

    // Find sources by analysis version (for re-analysis when version changes)
    List<StreamSource> findByAnalysisVersionNotAndActiveTrue(String analysisVersion);

    // Count sources by type
    @Query("SELECT s.sourceType, COUNT(s) FROM StreamSource s WHERE s.active = true GROUP BY s.sourceType")
    List<Object[]> countBySourceType();

    // Find sources with tracks
    @Query("SELECT DISTINCT s FROM StreamSource s LEFT JOIN FETCH s.tracks WHERE s.active = true")
    List<StreamSource> findAllWithTracks();

    // Find source with tracks by ID
    @Query("SELECT s FROM StreamSource s LEFT JOIN FETCH s.tracks WHERE s.id = :id AND s.active = true")
    Optional<StreamSource> findByIdWithTracks(@Param("id") Long id);

    // Statistics queries
    @Query("SELECT COUNT(s) FROM StreamSource s WHERE s.active = true")
    long countActiveSources();

    @Query("SELECT COUNT(s) FROM StreamSource s WHERE s.active = true AND s.analyzedAt IS NOT NULL")
    long countAnalyzedSources();

    @Query("SELECT AVG(s.durationSeconds) FROM StreamSource s WHERE s.active = true AND s.durationSeconds IS NOT NULL")
    Double getAverageDurationSeconds();

    @Query("SELECT SUM(s.fileSize) FROM StreamSource s WHERE s.active = true AND s.fileSize IS NOT NULL")
    Long getTotalFileSize();
}
package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.StreamSource;
import nl.mallepetrus.jiptv.entity.TranscodingJob;
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
public interface TranscodingJobRepository extends JpaRepository<TranscodingJob, Long> {

    // Find jobs by status
    List<TranscodingJob> findByStatusOrderByCreatedAtAsc(TranscodingJob.Status status);

    Page<TranscodingJob> findByStatusOrderByCreatedAtDesc(TranscodingJob.Status status, Pageable pageable);

    // Find jobs by type
    List<TranscodingJob> findByJobTypeOrderByCreatedAtDesc(TranscodingJob.JobType jobType);

    // Find jobs by stream source
    List<TranscodingJob> findByStreamSourceOrderByCreatedAtDesc(StreamSource streamSource);

    List<TranscodingJob> findByStreamSourceIdOrderByCreatedAtDesc(Long streamSourceId);

    // Find jobs by stream source and status
    List<TranscodingJob> findByStreamSourceAndStatusInOrderByCreatedAtDesc(
            StreamSource streamSource, List<TranscodingJob.Status> statuses);

    // Find jobs by stream source, job type and status
    List<TranscodingJob> findByStreamSourceAndJobTypeAndStatusInOrderByCreatedAtDesc(
            StreamSource streamSource, TranscodingJob.JobType jobType, List<TranscodingJob.Status> statuses);

    // Find pending jobs (queue)
    List<TranscodingJob> findByStatusInOrderByCreatedAtAsc(List<TranscodingJob.Status> statuses);

    // Find running jobs
    List<TranscodingJob> findByStatusAndStartedAtIsNotNullOrderByStartedAtAsc(TranscodingJob.Status status);

    // Find jobs by date range
    List<TranscodingJob> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // Find completed jobs with output
    @Query("SELECT j FROM TranscodingJob j WHERE j.status = 'COMPLETED' AND j.outputFile IS NOT NULL " +
           "ORDER BY j.completedAt DESC")
    List<TranscodingJob> findCompletedJobsWithOutput();

    // Find failed jobs
    @Query("SELECT j FROM TranscodingJob j WHERE j.status = 'FAILED' AND j.errorMessage IS NOT NULL " +
           "ORDER BY j.completedAt DESC")
    List<TranscodingJob> findFailedJobsWithErrors();

    // Find jobs by transcoding profile
    List<TranscodingJob> findByTranscodingProfileOrderByCreatedAtDesc(String transcodingProfile);

    // Find stuck jobs (running too long)
    @Query("SELECT j FROM TranscodingJob j WHERE j.status = 'RUNNING' AND " +
           "j.startedAt < :stuckThreshold")
    List<TranscodingJob> findStuckJobs(@Param("stuckThreshold") LocalDateTime stuckThreshold);

    // Find jobs needing cleanup (old completed/failed jobs)
    @Query("SELECT j FROM TranscodingJob j WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND " +
           "j.completedAt < :cleanupThreshold")
    List<TranscodingJob> findJobsForCleanup(@Param("cleanupThreshold") LocalDateTime cleanupThreshold);

    // Check if analysis job exists for stream
    @Query("SELECT j FROM TranscodingJob j WHERE j.streamSource = :streamSource AND " +
           "j.jobType = 'ANALYSIS' AND j.status IN ('PENDING', 'RUNNING', 'COMPLETED') " +
           "ORDER BY j.createdAt DESC")
    List<TranscodingJob> findAnalysisJobsForStream(@Param("streamSource") StreamSource streamSource);

    // Find latest completed analysis for stream
    @Query("SELECT j FROM TranscodingJob j WHERE j.streamSource = :streamSource AND " +
           "j.jobType = 'ANALYSIS' AND j.status = 'COMPLETED' " +
           "ORDER BY j.completedAt DESC")
    Optional<TranscodingJob> findLatestCompletedAnalysis(@Param("streamSource") StreamSource streamSource);

    // Update job progress
    @Modifying
    @Query("UPDATE TranscodingJob j SET j.progressPercent = :progress, j.currentFrame = :currentFrame, " +
           "j.processingSpeed = :speed, j.estimatedCompletionAt = :estimatedCompletion " +
           "WHERE j.id = :jobId")
    int updateJobProgress(@Param("jobId") Long jobId,
                         @Param("progress") Integer progress,
                         @Param("currentFrame") Long currentFrame,
                         @Param("speed") String processingSpeed,
                         @Param("estimatedCompletion") LocalDateTime estimatedCompletion);

    // Update job status
    @Modifying
    @Query("UPDATE TranscodingJob j SET j.status = :status WHERE j.id = :jobId")
    int updateJobStatus(@Param("jobId") Long jobId, @Param("status") TranscodingJob.Status status);

    // Cancel jobs by criteria
    @Modifying
    @Query("UPDATE TranscodingJob j SET j.status = 'CANCELLED', j.completedAt = :cancelTime " +
           "WHERE j.streamSource = :streamSource AND j.status IN ('PENDING', 'RUNNING')")
    int cancelJobsForStream(@Param("streamSource") StreamSource streamSource, 
                           @Param("cancelTime") LocalDateTime cancelTime);

    @Modifying
    @Query("UPDATE TranscodingJob j SET j.status = 'CANCELLED', j.completedAt = :cancelTime " +
           "WHERE j.status = 'RUNNING' AND j.startedAt < :stuckThreshold")
    int cancelStuckJobs(@Param("stuckThreshold") LocalDateTime stuckThreshold,
                       @Param("cancelTime") LocalDateTime cancelTime);

    // Statistics queries
    @Query("SELECT j.status, COUNT(j) FROM TranscodingJob j GROUP BY j.status")
    List<Object[]> getJobCountsByStatus();

    @Query("SELECT j.jobType, COUNT(j) FROM TranscodingJob j GROUP BY j.jobType")
    List<Object[]> getJobCountsByType();

    @Query("SELECT j.transcodingProfile, COUNT(j) FROM TranscodingJob j " +
           "WHERE j.transcodingProfile IS NOT NULL GROUP BY j.transcodingProfile")
    List<Object[]> getJobCountsByProfile();

    @Query("SELECT COUNT(j) FROM TranscodingJob j WHERE j.status = 'RUNNING'")
    long countRunningJobs();

    @Query("SELECT COUNT(j) FROM TranscodingJob j WHERE j.status = 'PENDING'")
    long countPendingJobs();

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - started_at))/60) FROM transcoding_jobs " +
           "WHERE status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL", nativeQuery = true)
    Double getAverageJobDurationMinutes();

    // Queue management
    @Query("SELECT j FROM TranscodingJob j WHERE j.status = 'PENDING' " +
           "ORDER BY j.jobType ASC, j.createdAt ASC")
    List<TranscodingJob> findJobQueue(Pageable pageable);

    // Find next job to process
    @Query("SELECT j FROM TranscodingJob j WHERE j.status = 'PENDING' " +
           "ORDER BY CASE j.jobType " +
           "WHEN 'ANALYSIS' THEN 1 " +
           "WHEN 'THUMBNAIL' THEN 2 " +
           "WHEN 'TRANSCODE' THEN 3 " +
           "WHEN 'SEGMENT' THEN 4 " +
           "END, j.createdAt ASC")
    Optional<TranscodingJob> findNextJobToProcess();
}
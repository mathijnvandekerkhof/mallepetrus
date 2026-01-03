package nl.mallepetrus.jiptv.dto;

import nl.mallepetrus.jiptv.service.TranscodingJobQueueService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TranscodingQueueStatisticsResponse {

    private long pendingJobs;
    private long runningJobs;
    private int maxConcurrentJobs;
    private boolean atCapacity;
    private double averageDurationMinutes;
    private Map<String, Long> statusCounts;
    private Map<String, Long> typeCounts;

    // Constructors
    public TranscodingQueueStatisticsResponse() {}

    public TranscodingQueueStatisticsResponse(TranscodingJobQueueService.JobQueueStatistics stats) {
        this.pendingJobs = stats.getPendingJobs();
        this.runningJobs = stats.getRunningJobs();
        this.maxConcurrentJobs = stats.getMaxConcurrentJobs();
        this.atCapacity = stats.isAtCapacity();
        this.averageDurationMinutes = stats.getAverageDurationMinutes();
        
        // Convert Object[] arrays to Maps
        this.statusCounts = stats.getStatusCounts().stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        
        this.typeCounts = stats.getTypeCounts().stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
    }

    // Getters and Setters
    public long getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(long pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public long getRunningJobs() {
        return runningJobs;
    }

    public void setRunningJobs(long runningJobs) {
        this.runningJobs = runningJobs;
    }

    public int getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    public void setMaxConcurrentJobs(int maxConcurrentJobs) {
        this.maxConcurrentJobs = maxConcurrentJobs;
    }

    public boolean isAtCapacity() {
        return atCapacity;
    }

    public void setAtCapacity(boolean atCapacity) {
        this.atCapacity = atCapacity;
    }

    public double getAverageDurationMinutes() {
        return averageDurationMinutes;
    }

    public void setAverageDurationMinutes(double averageDurationMinutes) {
        this.averageDurationMinutes = averageDurationMinutes;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public Map<String, Long> getTypeCounts() {
        return typeCounts;
    }

    public void setTypeCounts(Map<String, Long> typeCounts) {
        this.typeCounts = typeCounts;
    }
}
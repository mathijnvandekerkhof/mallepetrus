package nl.mallepetrus.jiptv.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RiskAssessmentResponse {
    private Integer currentRiskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean requiresStepUpAuth;
    private List<RiskFactor> riskFactors;
    private String recommendation;
    private LocalDateTime assessedAt;

    // Constructors
    public RiskAssessmentResponse() {}

    public RiskAssessmentResponse(Integer currentRiskScore, String riskLevel, boolean requiresStepUpAuth) {
        this.currentRiskScore = currentRiskScore;
        this.riskLevel = riskLevel;
        this.requiresStepUpAuth = requiresStepUpAuth;
        this.assessedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getCurrentRiskScore() {
        return currentRiskScore;
    }

    public void setCurrentRiskScore(Integer currentRiskScore) {
        this.currentRiskScore = currentRiskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isRequiresStepUpAuth() {
        return requiresStepUpAuth;
    }

    public void setRequiresStepUpAuth(boolean requiresStepUpAuth) {
        this.requiresStepUpAuth = requiresStepUpAuth;
    }

    public List<RiskFactor> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<RiskFactor> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }

    // Inner class for risk factors
    public static class RiskFactor {
        private String type;
        private String description;
        private Integer score;
        private String severity;

        public RiskFactor() {}

        public RiskFactor(String type, String description, Integer score, String severity) {
            this.type = type;
            this.description = description;
            this.score = score;
            this.severity = severity;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }
}
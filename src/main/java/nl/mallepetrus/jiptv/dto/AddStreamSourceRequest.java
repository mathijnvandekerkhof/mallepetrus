package nl.mallepetrus.jiptv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AddStreamSourceRequest {
    
    @NotBlank(message = "Stream name is required")
    @Size(max = 255, message = "Stream name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Source type is required")
    private String sourceType; // FILE, URL, IPTV_CHANNEL
    
    @NotBlank(message = "Source URL is required")
    private String sourceUrl;

    public AddStreamSourceRequest() {}

    public AddStreamSourceRequest(String name, String description, String sourceType, String sourceUrl) {
        this.name = name;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
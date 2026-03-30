package com.example.jobautomate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.embedding")
public class EmbeddingProperties {

    private boolean enabled = true;
    private String baseUrl = "https://job-parsing-embedding.onrender.com";
    private String embedPath = "/embed";
    private int timeoutMs = 10000;
    private int batchSize = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmbedPath() {
        return embedPath;
    }

    public void setEmbedPath(String embedPath) {
        this.embedPath = embedPath;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}

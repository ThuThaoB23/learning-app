package com.learnapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tts")
public class TextToSpeechProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:8000";
    private String synthesizePath = "/tts";
    private long minRequestIntervalMs = 0;
    private long cooldownSeconds = 30;
    private String defaultAccent;

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

    public String getSynthesizePath() {
        return synthesizePath;
    }

    public void setSynthesizePath(String synthesizePath) {
        this.synthesizePath = synthesizePath;
    }

    public long getMinRequestIntervalMs() {
        return minRequestIntervalMs;
    }

    public void setMinRequestIntervalMs(long minRequestIntervalMs) {
        this.minRequestIntervalMs = minRequestIntervalMs;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getDefaultAccent() {
        return defaultAccent;
    }

    public void setDefaultAccent(String defaultAccent) {
        this.defaultAccent = defaultAccent;
    }
}

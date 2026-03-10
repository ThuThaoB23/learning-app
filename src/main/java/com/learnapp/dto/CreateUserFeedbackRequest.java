package com.learnapp.dto;

import com.learnapp.entities.UserFeedbackCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class CreateUserFeedbackRequest {

    @NotNull
    private UserFeedbackCategory category;

    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String message;

    @Size(max = 120)
    private String sourceScreen;

    @Size(max = 50)
    private String appVersion;

    @Size(max = 500)
    private String deviceInfo;

    @Size(max = 20)
    private String locale;

    private List<MultipartFile> attachments;

    public UserFeedbackCategory getCategory() {
        return category;
    }

    public void setCategory(UserFeedbackCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSourceScreen() {
        return sourceScreen;
    }

    public void setSourceScreen(String sourceScreen) {
        this.sourceScreen = sourceScreen;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public List<MultipartFile> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MultipartFile> attachments) {
        this.attachments = attachments;
    }
}

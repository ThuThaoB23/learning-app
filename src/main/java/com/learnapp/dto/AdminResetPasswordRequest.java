package com.learnapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 255) String newPassword
) {}

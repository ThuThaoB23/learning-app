package com.learnapp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @JsonAlias({"email", "username"}) @NotBlank String identifier,
        @NotBlank @Size(min = 8, max = 255) String password
) {}

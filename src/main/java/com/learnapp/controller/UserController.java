package com.learnapp.controller;

import com.learnapp.dto.UpdateMeRequest;
import com.learnapp.dto.UserResponse;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get the current authenticated user's profile.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getMe(principal.id());
    }

    /**
     * Update the current authenticated user's profile fields.
     */
    @PatchMapping("/me")
    public UserResponse updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMeRequest request
    ) {
        return userService.updateMe(principal.id(), request);
    }
}

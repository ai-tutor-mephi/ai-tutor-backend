package com.VLmb.ai_tutor_backend.feature.user.api;

import com.VLmb.ai_tutor_backend.feature.user.api.dto.ChangeUsernameRequest;
import com.VLmb.ai_tutor_backend.feature.user.api.dto.ChangeUsernameResponse;
import com.VLmb.ai_tutor_backend.feature.auth.application.CustomUserDetails;
import com.VLmb.ai_tutor_backend.feature.user.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/change-username")
    public ResponseEntity<ChangeUsernameResponse> changeUsername(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangeUsernameRequest request) {

        ChangeUsernameResponse response = userService.changeUsername(principal.getUser(), request.userName());
        return ResponseEntity.ok(response);
    }
}

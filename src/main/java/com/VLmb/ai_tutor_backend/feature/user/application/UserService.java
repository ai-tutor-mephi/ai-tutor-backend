package com.VLmb.ai_tutor_backend.feature.user.application;

import com.VLmb.ai_tutor_backend.feature.user.api.dto.ChangeUsernameResponse;
import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.DuplicateResourceException;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public ChangeUsernameResponse changeUsername(User currentUser, String newUserName) {
        userRepository.findByUserName(newUserName)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User", "userName", newUserName);
                });

        currentUser.setUserName(newUserName);
        User saved = userRepository.save(currentUser);
        return new ChangeUsernameResponse(saved.getId(), saved.getUserName());
    }
}

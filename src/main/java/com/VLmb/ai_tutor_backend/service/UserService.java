package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.dto.ChangeUsernameResponse;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.exception.DuplicateResourceException;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
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

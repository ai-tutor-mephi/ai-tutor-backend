package com.VLmb.ai_tutor_backend.feature.user.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import com.VLmb.ai_tutor_backend.feature.user.api.dto.ChangeUsernameResponse;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUserName("oldName");
    }

    @Test
    void shouldChangeUsername() {
        when(userRepository.findByUserName("newName")).thenReturn(Optional.empty());
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ChangeUsernameResponse response = userService.changeUsername(currentUser, "newName");

        assertEquals(1L, response.userId());
        assertEquals("newName", response.userName());
        assertEquals("newName", currentUser.getUserName());
        verify(userRepository).findByUserName("newName");
        verify(userRepository).save(currentUser);
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUserName("takenName");

        when(userRepository.findByUserName("takenName")).thenReturn(Optional.of(existingUser));

        assertThrows(DuplicateResourceException.class,
                () -> userService.changeUsername(currentUser, "takenName"));

        verify(userRepository).findByUserName("takenName");
        verify(userRepository, never()).save(currentUser);
    }
}

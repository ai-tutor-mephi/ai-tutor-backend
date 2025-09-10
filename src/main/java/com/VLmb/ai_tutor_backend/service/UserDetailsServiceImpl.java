package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User appUser = userRepository.findByUserName(username).orElseThrow(() ->
                new UsernameNotFoundException("User not found with username: " + username));

        return  new org.springframework.security.core.userdetails.User(appUser.getUserName(),
                appUser.getHashPassword(), new ArrayList<>());
    }
}

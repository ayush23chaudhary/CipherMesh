package com.ciphermesh.user.service;

import com.ciphermesh.common.exception.ResourceNotFoundException;
import com.ciphermesh.user.dto.RegisterUserRequest;
import com.ciphermesh.user.dto.UserResponse;
import com.ciphermesh.user.entity.User;
import com.ciphermesh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of UserService.
 *
 * Security note:
 * - Logs use username only — never log keys or internal IDs externally.
 * - No password or private-key handling in this module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .identityPublicKey(request.getIdentityPublicKey())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: username={}", saved.getUsername());
        // Security: never log the identity key or UUID externally
        return toResponse(saved);
    }

    @Override
    public UserResponse findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Override
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .identityPublicKey(user.getIdentityPublicKey())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

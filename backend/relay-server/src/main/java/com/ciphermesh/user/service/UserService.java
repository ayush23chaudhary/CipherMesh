package com.ciphermesh.user.service;

import com.ciphermesh.user.dto.RegisterUserRequest;
import com.ciphermesh.user.dto.UserResponse;

import java.util.UUID;

/**
 * User service contract.
 * Business logic for user registration and lookup.
 */
public interface UserService {

    UserResponse register(RegisterUserRequest request);

    UserResponse findById(UUID id);

    UserResponse findByUsername(String username);
}

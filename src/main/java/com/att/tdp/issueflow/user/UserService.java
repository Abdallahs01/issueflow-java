package com.att.tdp.issueflow.user;

import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.dto.UpdateUserRequest;
import com.att.tdp.issueflow.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, AuditLogService auditLogService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return UserResponse.from(findUserById(userId));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists.");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.role()
        );

        User savedUser = userRepository.save(user);
        auditLogService.recordCurrentUserAction("USER", savedUser.getId(), "CREATE", "User was created.");

        return UserResponse.from(savedUser);
    }

    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = findUserById(userId);

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        auditLogService.recordCurrentUserAction("USER", user.getId(), "UPDATE", "User was updated.");
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        auditLogService.recordCurrentUserAction("USER", user.getId(), "DELETE", "User was deleted.");
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }
}

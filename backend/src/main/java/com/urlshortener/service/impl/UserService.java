package com.urlshortener.service.impl;

import com.urlshortener.domain.entity.User;
import com.urlshortener.domain.enums.AuditAction;
import com.urlshortener.dto.request.ChangePasswordRequest;
import com.urlshortener.dto.response.UserProfileResponse;
import com.urlshortener.exception.InvalidCredentialsException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.AuditLogService;
import com.urlshortener.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return toProfileResponse(user);
    }

  
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(userId);

        log.info("Password changed for userId={}", userId);
        auditLogService.log(userId, AuditAction.PASSWORD_CHANGED, "User", userId.toString(), ipAddress, Map.of());
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}

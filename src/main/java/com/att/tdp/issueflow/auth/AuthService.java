package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.auth.dto.LoginResponse;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.user.UserService;
import com.att.tdp.issueflow.user.dto.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, TokenBlacklistService tokenBlacklistService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return LoginResponse.bearer(jwtService.generateToken(user));
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        tokenBlacklistService.invalidate(token);
    }

    public UserResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        return userService.getUserById(authenticatedUser.getId());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Missing bearer token.");
        }

        return authorizationHeader.substring(7);
    }
}

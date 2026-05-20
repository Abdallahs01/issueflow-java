package com.att.tdp.issueflow.auth;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Set<String> invalidTokens = ConcurrentHashMap.newKeySet();

    public void invalidate(String token) {
        invalidTokens.add(token);
    }

    public boolean isInvalid(String token) {
        return invalidTokens.contains(token);
    }
}

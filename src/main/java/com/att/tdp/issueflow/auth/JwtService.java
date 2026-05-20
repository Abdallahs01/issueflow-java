package com.att.tdp.issueflow.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${issueflow.jwt.secret}") String secret,
            @Value("${issueflow.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant expiresAt = Instant.now().plusSeconds(expirationMinutes * 60);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("userId", user.getId());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractUsername(String token) {
        return String.valueOf(readPayload(token).get("sub"));
    }

    public boolean isTokenValid(String token, AuthenticatedUser user) {
        return extractUsername(token).equals(user.getUsername()) && !isExpired(token) && hasValidSignature(token);
    }

    private boolean isExpired(String token) {
        Number expiresAt = (Number) readPayload(token).get("exp");
        return Instant.now().getEpochSecond() >= expiresAt.longValue();
    }

    private boolean hasValidSignature(String token) {
        String[] parts = splitToken(token);
        String unsignedToken = parts[0] + "." + parts[1];
        return sign(unsignedToken).equals(parts[2]);
    }

    private Map<String, Object> readPayload(String token) {
        String[] parts = splitToken(token);

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT payload.", exception);
        }
    }

    private String[] splitToken(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format.");
        }

        return parts;
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not write JWT JSON.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT.", exception);
        }
    }
}

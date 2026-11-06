package com.sisge.ms.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final String secret;
    private final long ttl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long ttl) {
        this.secret = secret;
        this.ttl = ttl;
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must have at least 32 characters");
        }
    }

    public String create(CurrentUser user) {
        long expiration = Instant.now().plusSeconds(ttl).getEpochSecond();
        Set<String> permissions = new TreeSet<>(user.globalPermissions());
        user.organizationPermissions().values().forEach(permissions::addAll);

        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"" + escapeJson(user.email()) + "\",\"exp\":" + expiration
                + ",\"permissions\":" + permissionsJson(permissions) + "}");
        return header + "." + payload + "." + sign(header + "." + payload);
    }

    public String subject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !constant(sign(parts[0] + "." + parts[1]), parts[2])) {
                return null;
            }
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long expiration = Long.parseLong(json.replaceAll(".*\"exp\":(\\d+).*", "$1"));
            if (expiration < Instant.now().getEpochSecond()) {
                return null;
            }
            return json.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
        } catch (Exception exception) {
            return null;
        }
    }

    private String permissionsJson(Set<String> permissions) {
        return permissions.stream()
                .map(permission -> "\"" + escapeJson(permission) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constant(String left, String right) {
        return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

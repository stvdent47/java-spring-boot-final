package mephi.bookingservice.service;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mephi.bookingservice.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Slf4j
@Service
public class JwtService {
    @Value("${app.jwt.private-key-location}")
    private String privateKeyLocation;

    @Getter
    @Value("${app.jwt.expiration-ms:3600000}")
    private Long expirationMs;

    @Value("${app.jwt.issuer:booking-service}")
    private String issuer;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey();
            log.info("JWT private key loaded successfully");
        }
        catch (Exception e) {
            log.error("Failed to load JWT private key", e);
            throw new RuntimeException("Failed to initialize JWT service", e);
        }
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getUsername());
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("roles", Collections.singletonList("ROLE_" + user.getRole().name()));

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String keyPath = privateKeyLocation.replace("classpath:", "");
        if (!keyPath.startsWith("/")) {
            keyPath = "/" + keyPath;
        }

        try (InputStream is = getClass().getResourceAsStream(keyPath)) {
            if (is == null) {
                throw new IOException("Private key file not found: " + privateKeyLocation);
            }

            String keyContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(spec);
        }
    }
}

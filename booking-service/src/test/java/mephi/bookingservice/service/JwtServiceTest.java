package mephi.bookingservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import mephi.bookingservice.entity.Role;
import mephi.bookingservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtService Integration Tests")
class JwtServiceTest {
    @Autowired
    private JwtService jwtService;

    private User testUser;
    private PublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test_user");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        publicKey = loadPublicKey();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {
        @Test
        @DisplayName("should generate valid JWT token for user")
        void should_GenerateValidToken_For_User() {
            String token = jwtService.generateToken(testUser);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
        }

        @Test
        @DisplayName("should include correct claims in token")
        void should_IncludeCorrectClaims_In_Token() {
            String token = jwtService.generateToken(testUser);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            assertThat(claims.getSubject()).isEqualTo("test_user");
            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
            assertThat(claims.get("role", String.class)).isEqualTo("USER");
            assertThat(claims.getIssuer()).isEqualTo("booking-service");
        }

        @Test
        @DisplayName("should include roles claim for Spring Security")
        void should_IncludeRolesClaim_For_SpringSecurity() {
            String token = jwtService.generateToken(testUser);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            assertThat(roles).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should generate token with ADMIN role for admin user")
        void should_GenerateToken_With_AdminRole() {
            User adminUser = new User();
            adminUser.setId(2L);
            adminUser.setUsername("admin_user");
            adminUser.setEmail("admin@example.com");
            adminUser.setRole(Role.ADMIN);

            String token = jwtService.generateToken(adminUser);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            assertThat(roles).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should set correct expiration time")
        void should_SetCorrectExpiration_Time() {
            long beforeGeneration = System.currentTimeMillis();

            String token = jwtService.generateToken(testUser);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            long afterGeneration = System.currentTimeMillis();
            long expectedExpirationMin = beforeGeneration + jwtService.getExpirationMs() - 1000;
            long expectedExpirationMax = afterGeneration + jwtService.getExpirationMs() + 1000;

            assertThat(claims.getExpiration().getTime())
                .isGreaterThanOrEqualTo(expectedExpirationMin)
                .isLessThanOrEqualTo(expectedExpirationMax);
        }

        @Test
        @DisplayName("should generate unique tokens for same user")
        void should_GenerateUniqueTokens_For_SameUser() throws InterruptedException {
            String token1 = jwtService.generateToken(testUser);
            Thread.sleep(1100); // Ensure different second for iat/exp claims
            String token2 = jwtService.generateToken(testUser);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("getExpirationMs")
    class GetExpirationMs {
        @Test
        @DisplayName("should return configured expiration time")
        void should_ReturnConfiguredExpiration() {
            Long expirationMs = jwtService.getExpirationMs();

            assertThat(expirationMs).isEqualTo(3600000L);
        }
    }

    private PublicKey loadPublicKey() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/keys/public.pem")) {
            if (is == null) {
                throw new RuntimeException("Public key file not found");
            }

            String keyContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            String publicKeyPEM = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(spec);
        }
    }
}

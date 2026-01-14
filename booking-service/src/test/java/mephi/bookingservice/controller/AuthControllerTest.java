package mephi.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.bookingservice.dto.AuthResponse;
import mephi.bookingservice.dto.LoginRequest;
import mephi.bookingservice.dto.RegisterRequest;
import mephi.bookingservice.dto.UserResponse;
import mephi.bookingservice.entity.Role;
import mephi.bookingservice.exception.AuthenticationException;
import mephi.bookingservice.exception.DuplicateResourceException;
import mephi.bookingservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
@DisplayName("AuthController MockMvc Tests")
class AuthControllerTest {
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain authTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/register", "/auth/login").permitAll()
                    .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("john_doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("john_doe");
        loginRequest.setPassword("password123");

        userResponse = UserResponse.builder()
            .id(1L)
            .username("john_doe")
            .email("john@example.com")
            .firstName("John")
            .lastName("Doe")
            .role(Role.USER)
            .build();

        authResponse = AuthResponse.builder()
            .token("jwt-token-12345")
            .tokenType("Bearer")
            .expiresIn(3600000L)
            .user(userResponse)
            .build();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {
        @Test
        @DisplayName("should register user successfully")
        void should_RegisterUser_When_ValidRequest() throws Exception {
            given(userService.register(any(RegisterRequest.class))).willReturn(authResponse);

            mockMvc.perform(
                post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("jwt-token-12345")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("john_doe")));
        }

        @Test
        @DisplayName("should return 409 when username already exists")
        void should_Return409_When_UsernameExists() throws Exception {
            given(userService.register(any(RegisterRequest.class)))
                .willThrow(new DuplicateResourceException("User", "username", "john_doe"));

            mockMvc.perform(
                post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when request is invalid")
        void should_Return400_When_InvalidRequest() throws Exception {
            RegisterRequest invalidRequest = new RegisterRequest();
            invalidRequest.setUsername(""); // Empty username
            invalidRequest.setEmail("john@example.com");
            invalidRequest.setPassword("pass");

            mockMvc.perform(
                post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when email is invalid format")
        void should_Return400_When_InvalidEmail() throws Exception {
            registerRequest.setEmail("invalid-email");

            mockMvc.perform(
                post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {
        @Test
        @DisplayName("should login successfully with valid credentials")
        void should_Login_When_ValidCredentials() throws Exception {
            given(userService.login(any(LoginRequest.class))).willReturn(authResponse);

            mockMvc.perform(
                post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token-12345")))
                .andExpect(jsonPath("$.expiresIn", is(3600000)));
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void should_Return401_When_InvalidCredentials() throws Exception {
            given(userService.login(any(LoginRequest.class)))
                .willThrow(new AuthenticationException("Invalid username or password"));

            mockMvc.perform(
                post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when request is invalid")
        void should_Return400_When_MissingFields() throws Exception {
            LoginRequest invalidRequest = new LoginRequest();
            invalidRequest.setUsername("");

            mockMvc.perform(
                post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /auth/me")
    class GetCurrentUser {
        @Test
        @DisplayName("should return current user when authenticated")
        void should_ReturnCurrentUser_When_Authenticated() throws Exception {
            UserDetails mockUser = User.builder()
                .username("john_doe")
                .password("password")
                .roles("USER")
                .build();

            given(userService.getUserByUsername(eq("john_doe"))).willReturn(userResponse);

            mockMvc.perform(
                get("/auth/me")
                        .with(user(mockUser))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("john_doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void should_Return401_When_NotAuthenticated() throws Exception {
            mockMvc.perform(
                get("/auth/me")
            )
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/admin/create")
    class CreateAdmin {
        @Test
        @DisplayName("should create admin when called by admin user")
        void should_CreateAdmin_When_CalledByAdmin() throws Exception {
            UserResponse adminUserResponse = UserResponse.builder()
                .id(2L)
                .username("new_admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

            AuthResponse adminAuthResponse = AuthResponse.builder()
                .token("admin-jwt-token")
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .user(adminUserResponse)
                .build();

            given(userService.createAdmin(any(RegisterRequest.class))).willReturn(adminAuthResponse);

            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setUsername("new_admin");
            adminRequest.setEmail("admin@example.com");
            adminRequest.setPassword("adminpass123");
            adminRequest.setFirstName("Admin");
            adminRequest.setLastName("User");

            UserDetails adminUser = User.builder()
                .username("existing_admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/auth/admin/create")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(adminRequest))
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role", is("ADMIN")));
        }

        @Test
        @DisplayName("should return 403 when called by regular user")
        void should_Return403_When_CalledByRegularUser() throws Exception {
            UserDetails regularUser = User.builder()
                .username("regular_user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/auth/admin/create")
                    .with(csrf())
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void should_Return401_When_NotAuthenticated() throws Exception {
            mockMvc.perform(
                post("/auth/admin/create")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )
                .andExpect(status().isUnauthorized());
        }
    }
}

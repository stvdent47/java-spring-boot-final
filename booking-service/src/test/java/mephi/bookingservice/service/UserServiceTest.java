package mephi.bookingservice.service;

import mephi.bookingservice.dto.AuthResponse;
import mephi.bookingservice.dto.LoginRequest;
import mephi.bookingservice.dto.RegisterRequest;
import mephi.bookingservice.dto.UserResponse;
import mephi.bookingservice.entity.Role;
import mephi.bookingservice.entity.User;
import mephi.bookingservice.exception.AuthenticationException;
import mephi.bookingservice.exception.DuplicateResourceException;
import mephi.bookingservice.exception.ResourceNotFoundException;
import mephi.bookingservice.mapper.UserMapper;
import mephi.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);

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
    }

    @Nested
    @DisplayName("register")
    class Register {
        @Test
        @DisplayName("should register user successfully when credentials are unique")
        void should_RegisterUser_When_CredentialsUnique() {
            given(userRepository.existsByUsername("john_doe")).willReturn(false);
            given(userRepository.existsByEmail("john@example.com")).willReturn(false);
            given(userMapper.toEntity(registerRequest)).willReturn(testUser);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(jwtService.generateToken(testUser)).willReturn("jwt-token");
            given(jwtService.getExpirationMs()).willReturn(3600000L);
            given(userMapper.toResponse(testUser)).willReturn(userResponse);

            AuthResponse result = userService.register(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getUser().getUsername()).isEqualTo("john_doe");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username exists")
        void should_ThrowDuplicateResourceException_When_UsernameExists() {
            given(userRepository.existsByUsername("john_doe")).willReturn(true);

            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username")
                .hasMessageContaining("john_doe");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email exists")
        void should_ThrowDuplicateResourceException_When_EmailExists() {
            given(userRepository.existsByUsername("john_doe")).willReturn(false);
            given(userRepository.existsByEmail("john@example.com")).willReturn(true);

            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should set role to USER for regular registration")
        void should_SetRoleToUser_When_RegularRegistration() {
            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userMapper.toEntity(any(RegisterRequest.class))).willReturn(testUser);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                assertThat(user.getRole()).isEqualTo(Role.USER);
                return user;
            });
            given(jwtService.generateToken(any(User.class))).willReturn("jwt-token");
            given(jwtService.getExpirationMs()).willReturn(3600000L);
            given(userMapper.toResponse(any(User.class))).willReturn(userResponse);

            userService.register(registerRequest);

            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {
        @Test
        @DisplayName("should login successfully with valid credentials")
        void should_Login_When_CredentialsValid() {
            given(userRepository.findByUsername("john_doe")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
            given(jwtService.generateToken(testUser)).willReturn("jwt-token");
            given(jwtService.getExpirationMs()).willReturn(3600000L);
            given(userMapper.toResponse(testUser)).willReturn(userResponse);

            AuthResponse result = userService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getExpiresIn()).isEqualTo(3600000L);
        }

        @Test
        @DisplayName("should throw AuthenticationException when username not found")
        void should_ThrowAuthenticationException_When_UsernameNotFound() {
            given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());
            loginRequest.setUsername("unknown");

            assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("should throw AuthenticationException when password is wrong")
        void should_ThrowAuthenticationException_When_PasswordWrong() {
            given(userRepository.findByUsername("john_doe")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongpassword", "encodedPassword")).willReturn(false);
            loginRequest.setPassword("wrongpassword");

            assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("should throw AuthenticationException when account is disabled")
        void should_ThrowAuthenticationException_When_AccountDisabled() {
            testUser.setEnabled(false);
            given(userRepository.findByUsername("john_doe")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

            assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("disabled");
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {
        @Test
        @DisplayName("should return user when found")
        void should_ReturnUser_When_Found() {
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userMapper.toResponse(testUser)).willReturn(userResponse);

            UserResponse result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void should_ThrowResourceNotFoundException_When_UserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {
        @Test
        @DisplayName("should return user when found by username")
        void should_ReturnUser_When_FoundByUsername() {
            given(userRepository.findByUsername("john_doe")).willReturn(Optional.of(testUser));
            given(userMapper.toResponse(testUser)).willReturn(userResponse);

            UserResponse result = userService.getUserByUsername("john_doe");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when username not found")
        void should_ThrowResourceNotFoundException_When_UsernameNotFound() {
            given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("createAdmin")
    class CreateAdmin {
        @Test
        @DisplayName("should create admin user with ADMIN role")
        void should_CreateAdmin_With_AdminRole() {
            given(userRepository.existsByUsername("admin")).willReturn(false);
            given(userRepository.existsByEmail("admin@example.com")).willReturn(false);
            given(userMapper.toEntity(any(RegisterRequest.class))).willReturn(testUser);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                assertThat(user.getRole()).isEqualTo(Role.ADMIN);
                return user;
            });
            given(jwtService.generateToken(any(User.class))).willReturn("admin-jwt-token");
            given(jwtService.getExpirationMs()).willReturn(3600000L);
            given(userMapper.toResponse(any(User.class))).willReturn(userResponse);

            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setUsername("admin");
            adminRequest.setEmail("admin@example.com");
            adminRequest.setPassword("adminpass");

            AuthResponse result = userService.createAdmin(adminRequest);

            assertThat(result).isNotNull();
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when admin username exists")
        void should_ThrowDuplicateResourceException_When_AdminUsernameExists() {
            given(userRepository.existsByUsername("admin")).willReturn(true);

            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setUsername("admin");

            assertThatThrownBy(() -> userService.createAdmin(adminRequest))
                .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }
}

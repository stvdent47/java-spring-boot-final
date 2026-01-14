package mephi.bookingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        String token = jwtService.generateToken(savedUser);

        return AuthResponse.of(token, jwtService.getExpirationMs(), userMapper.toResponse(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new AuthenticationException("Invalid username or password");
        }

        if (!user.getEnabled()) {
            throw new AuthenticationException("User account is disabled");
        }

        log.info("User logged in successfully: {}", user.getUsername());

        String token = jwtService.generateToken(user);

        return AuthResponse.of(token, jwtService.getExpirationMs(), userMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Transactional
    public AuthResponse createAdmin(RegisterRequest request) {
        log.info("Creating admin user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ADMIN);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("Admin user created successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        String token = jwtService.generateToken(savedUser);

        return AuthResponse.of(token, jwtService.getExpirationMs(), userMapper.toResponse(savedUser));
    }
}

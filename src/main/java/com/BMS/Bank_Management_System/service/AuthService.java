package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.dto.AuthRequest;
import com.BMS.Bank_Management_System.dto.AuthResponse;
import com.BMS.Bank_Management_System.dto.UserDTO;
import com.BMS.Bank_Management_System.entity.Role;
import com.BMS.Bank_Management_System.entity.User;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.exception.UnauthorizedActionException;
import com.BMS.Bank_Management_System.repository.UserRepository;
import com.BMS.Bank_Management_System.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(UserDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already taken");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already taken");
        }

        // Public self-registration is restricted to CUSTOMER role only,
        // except bootstrapping the very first ADMIN user if none exists.
        if (request.getRole() != null && !"CUSTOMER".equalsIgnoreCase(request.getRole())) {
            boolean isFirstAdminBootstrap = "ADMIN".equalsIgnoreCase(request.getRole())
                    && userRepository.countByRole(Role.ADMIN) == 0;
            if (!isFirstAdminBootstrap) {
                throw new UnauthorizedActionException("Self-registration allowed only for CUSTOMER role");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setMotherName(request.getMotherName());
        user.setNationalIdImageUrl(request.getNationalIdImageUrl());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole((request.getRole() != null && "ADMIN".equalsIgnoreCase(request.getRole()) && userRepository.countByRole(Role.ADMIN) == 0)
                ? Role.ADMIN
                : Role.CUSTOMER);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    public User registerUserWithRoleControl(UserDTO request, Role creatorRole) {
        Role requestedRole = Role.valueOf(request.getRole().toUpperCase());
        boolean allowed = switch (creatorRole) {
            case ADMIN -> true; // admin can create any role
            case STAFF -> requestedRole == Role.CUSTOMER; // staff only customer
            case CUSTOMER -> false; // not allowed
        };
        if (!allowed) {
            throw new UnauthorizedActionException("Not allowed to create user with role " + requestedRole);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(requestedRole);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return userRepository.save(user);
    }

    private User authenticate(AuthRequest request) {

        User user = userRepository.findByEmailOrPhone(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + request.getUsername()));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now())) {
            throw new LockedException("Account is locked. Please try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLoginAttempt(user);
            throw new BadCredentialsException("Invalid credentials provided.");
        }

        resetFailedLoginAttempts(user);
        return user;
    }

    public AuthResponse login(AuthRequest request) {
        User user = authenticate(request);
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    public AuthResponse loginForAgent(AuthRequest request) {
        User user = authenticate(request);
        String agentToken = jwtUtil.generateAgentToken(user);
        return new AuthResponse(agentToken, user.getUsername(), user.getRole().name(), user.getId());
    }

    private void handleFailedLoginAttempt(User user) {
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setLockedUntil(java.time.Instant.now().plusSeconds(600)); // Lock for 10 minutes
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }

    private void resetFailedLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    public Role getUserRole(String username) {
        return userRepository.findByUsername(username)
                .map(User::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        return dto;
    }
}
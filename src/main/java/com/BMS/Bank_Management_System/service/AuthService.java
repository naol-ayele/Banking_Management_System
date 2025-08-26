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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ===========================
    // Self-registration (CUSTOMER only)
    // ===========================
    public AuthResponse register(UserDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already taken");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already taken");
        }

        // Only CUSTOMER allowed, except bootstrapping first ADMIN
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
        user.setRole((request.getRole() != null && "ADMIN".equalsIgnoreCase(request.getRole())
                && userRepository.countByRole(Role.ADMIN) == 0)
                ? Role.ADMIN
                : Role.CUSTOMER);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail()); // ✅ email is the unique principal
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    // ===========================
    // Admin/staff/loan-officer registration
    // ===========================
    public User registerUserWithRoleControl(UserDTO request, Role creatorRole) {
        Role requestedRole = Role.valueOf(request.getRole());

        boolean allowed = switch (creatorRole) {
            case ADMIN -> true;
            case STAFF, LOAN_OFFICER -> requestedRole == Role.CUSTOMER;
            case CUSTOMER -> false;
        };

        if (!allowed) {
            throw new UnauthorizedActionException("Not allowed to create user with role " + requestedRole);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already taken");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(requestedRole);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return userRepository.save(user);
    }

    // ===========================
    // Login (email/phone only)
    // ===========================
    public AuthResponse login(AuthRequest request) {
        String identifier = request.getIdentifier();

        // ✅ allow login by email or phone
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new RuntimeException("Account locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
            attempts++;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setLockedUntil(Instant.now().plusSeconds(10 * 60)); // lock 10 mins
                user.setFailedLoginAttempts(0);
            }

            userRepository.save(user);
            throw new RuntimeException("Invalid password");
        }

        // ✅ reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail()); // email is unique principal
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    // ===========================
    // Change password
    // ===========================
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ===========================
    // Utility
    // ===========================
    public Role getUserRole(String principal) {
        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .map(User::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

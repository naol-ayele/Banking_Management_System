package com.BMS.Bank_Management_System.controller;

import com.BMS.Bank_Management_System.dto.AuthRequest;
import com.BMS.Bank_Management_System.dto.AuthResponse;
import com.BMS.Bank_Management_System.dto.UserDTO;
import com.BMS.Bank_Management_System.service.AuthService;
import com.BMS.Bank_Management_System.dto.ChangePasswordRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(authService.register(userDTO));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/register/managed")
    public ResponseEntity<UserDTO> registerManaged(@RequestBody UserDTO userDTO, Authentication authentication) {
        var role = authService.getUserRole(authentication.getName());
        var created = authService.registerUserWithRoleControl(userDTO, role);
        return ResponseEntity.ok(authService.toDto(created));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login-agent")
    public ResponseEntity<AuthResponse> loginForAgent(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.loginForAgent(request));
    }


    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN', 'LOAN_OFFICER')")
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest req) {
        authService.changePassword(authentication.getName(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok("Password updated");
    }
}

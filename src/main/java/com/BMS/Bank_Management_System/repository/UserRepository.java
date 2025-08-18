package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.BMS.Bank_Management_System.entity.Role;
import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmailOrPhone(String username, String email, String phone);
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
            String username, String email, String phone, org.springframework.data.domain.Pageable pageable);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    long countByRole(Role role);
}


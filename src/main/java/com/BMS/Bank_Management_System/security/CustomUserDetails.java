package com.BMS.Bank_Management_System.security;

import com.BMS.Bank_Management_System.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails extends User implements UserDetails {

    public CustomUserDetails(User user) {
        this.setId(user.getId());
        this.setUsername(user.getUsername());
        this.setPassword(user.getPassword());
        this.setEmail(user.getEmail());
        this.setRole(user.getRole());
        this.setPhone(user.getPhone());
        this.setMotherName(user.getMotherName());
        this.setNationalIdImageUrl(user.getNationalIdImageUrl());
        this.setFailedLoginAttempts(user.getFailedLoginAttempts());
        this.setLockedUntil(user.getLockedUntil());
        this.setAccounts(user.getAccounts() != null ? user.getAccounts() : new java.util.ArrayList<>());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + getRole()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return getLockedUntil() == null || getLockedUntil().isBefore(java.time.Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isAccountNonLocked();
    }
}

package com.BMS.Bank_Management_System.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String motherName;
    private String nationalIdImageUrl;
    private String role;
    private String password; // Optional, only for registration
}

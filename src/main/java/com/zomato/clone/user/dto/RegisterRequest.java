package com.zomato.clone.user.dto;

import com.zomato.clone.enums.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {

    private String name;
    private String email;
    private String password;
    private String phone;
    private UserRole role;
}

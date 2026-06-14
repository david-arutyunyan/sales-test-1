package com.example.sales_test_1.dto;

import com.example.sales_test_1.model.UserRole;

public class AuthResponse {

    private String token;
    private String userId;
    private String email;
    private String name;
    private UserRole role;

    public AuthResponse(String token, String userId, String email, String name, UserRole role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public UserRole getRole() { return role; }
}

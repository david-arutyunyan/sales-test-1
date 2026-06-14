package com.example.sales_test_1.model;

public class SessionInfo {

    private String userId;
    private String email;
    private String name;
    private UserRole role;

    public SessionInfo() {}

    public SessionInfo(String userId, String email, String name, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean hasRole(UserRole... roles) {
        for (UserRole r : roles) {
            if (this.role == r) return true;
        }
        return false;
    }
}

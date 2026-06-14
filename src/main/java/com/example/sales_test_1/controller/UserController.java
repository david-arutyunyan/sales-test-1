package com.example.sales_test_1.controller;

import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.model.User;
import com.example.sales_test_1.model.UserRole;
import com.example.sales_test_1.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        if (user.getUserId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete yourself"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

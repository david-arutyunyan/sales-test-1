package com.example.sales_test_1.controller;

import com.example.sales_test_1.model.Order;
import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.model.UserRole;
import com.example.sales_test_1.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));

        // CUSTOMER can only see their own orders
        if (user.getRole() == UserRole.CUSTOMER) {
            return ResponseEntity.ok(orderService.getOrdersByEmail(user.getEmail()));
        }

        // SELLER and ADMIN can see all orders, optionally filtered
        if (email != null)  return ResponseEntity.ok(orderService.getOrdersByEmail(email));
        if (status != null) return ResponseEntity.ok(orderService.getOrdersByStatus(status));
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));

        Order order = orderService.getOrderById(id);

        // CUSTOMER can only see their own order
        if (user.getRole() == UserRole.CUSTOMER && !order.getCustomerEmail().equals(user.getEmail())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(order);
    }

    // CUSTOMER, SELLER, ADMIN can place orders
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody Order order, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));

        // Bind order to the authenticated user's email
        order.setCustomerEmail(user.getEmail());
        if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
            order.setCustomerName(user.getName());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(order));
    }

    // SELLER and ADMIN can update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody Map<String, String> body,
                                          HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || !user.hasRole(UserRole.SELLER, UserRole.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "Seller or Admin access required"));
        }
        return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}

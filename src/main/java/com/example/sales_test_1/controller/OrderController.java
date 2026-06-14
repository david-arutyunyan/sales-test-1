package com.example.sales_test_1.controller;

import com.example.sales_test_1.model.Order;
import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.model.UserRole;
import com.example.sales_test_1.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

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

        if (user.getRole() == UserRole.CUSTOMER) {
            log.debug("Customer {} fetching own orders", user.getEmail());
            return ResponseEntity.ok(orderService.getOrdersByEmail(user.getEmail()));
        }

        log.debug("{} {} fetching orders — email={} status={}", user.getRole(), user.getEmail(), email, status);
        if (email != null)  return ResponseEntity.ok(orderService.getOrdersByEmail(email));
        if (status != null) return ResponseEntity.ok(orderService.getOrdersByStatus(status));
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));

        Order order = orderService.getOrderById(id);

        if (user.getRole() == UserRole.CUSTOMER && !order.getCustomerEmail().equals(user.getEmail())) {
            log.warn("Forbidden: customer {} tried to access order {} owned by {}", user.getEmail(), id, order.getCustomerEmail());
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody Order order, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));

        order.setCustomerEmail(user.getEmail());
        if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
            order.setCustomerName(user.getName());
        }

        Order created = orderService.createOrder(order);
        log.info("Order placed: id={} by {} [{}] — {} item(s), total=${}",
                created.getId(), user.getName(), user.getEmail(),
                created.getItems().size(), created.getTotalAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody Map<String, String> body,
                                          HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || !user.hasRole(UserRole.SELLER, UserRole.ADMIN)) {
            log.warn("Forbidden: order status update denied for role={}", user != null ? user.getRole() : "anonymous");
            return ResponseEntity.status(403).body(Map.of("error", "Seller or Admin access required"));
        }
        String newStatus = body.get("status");
        Order updated = orderService.updateStatus(id, newStatus);
        log.info("Order status updated: id={} → '{}' by {} [{}]", id, newStatus, user.getName(), user.getEmail());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            log.warn("Forbidden: order deletion denied for role={}", user != null ? user.getRole() : "anonymous");
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        orderService.deleteOrder(id);
        log.info("Order deleted: id={} by admin {} [{}]", id, user.getName(), user.getEmail());
        return ResponseEntity.noContent().build();
    }
}

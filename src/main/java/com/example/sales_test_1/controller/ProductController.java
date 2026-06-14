package com.example.sales_test_1.controller;

import com.example.sales_test_1.model.Product;
import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.model.UserRole;
import com.example.sales_test_1.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Public — no auth needed (filter allows GET /api/products)
    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean inStock) {
        if (category != null) return productService.getProductsByCategory(category);
        if (search != null)   return productService.searchProducts(search);
        if (Boolean.TRUE.equals(inStock)) return productService.getInStockProducts();
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // SELLER or ADMIN only
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody Product product,
                                           HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || !user.hasRole(UserRole.SELLER, UserRole.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "Seller or Admin access required"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id,
                                           @Valid @RequestBody Product product,
                                           HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || !user.hasRole(UserRole.SELLER, UserRole.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "Seller or Admin access required"));
        }
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

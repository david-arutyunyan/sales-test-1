package com.example.sales_test_1.controller;

import com.example.sales_test_1.model.Product;
import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.model.UserRole;
import com.example.sales_test_1.service.ProductService;
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
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean inStock) {
        log.debug("GET /api/products category={} search={} inStock={}", category, search, inStock);
        if (category != null) return productService.getProductsByCategory(category);
        if (search != null)   return productService.searchProducts(search);
        if (Boolean.TRUE.equals(inStock)) return productService.getInStockProducts();
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody Product product,
                                           HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || !user.hasRole(UserRole.SELLER, UserRole.ADMIN)) {
            log.warn("Forbidden: product creation denied for role={}", user != null ? user.getRole() : "anonymous");
            return ResponseEntity.status(403).body(Map.of("error", "Seller or Admin access required"));
        }
        Product created = productService.createProduct(product);
        log.info("Product created: '{}' (id={}) by {} [{}]", created.getName(), created.getId(), user.getName(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id,
                                           @Valid @RequestBody Product product,
                                           HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || !user.hasRole(UserRole.SELLER, UserRole.ADMIN)) {
            log.warn("Forbidden: product update denied for role={}", user != null ? user.getRole() : "anonymous");
            return ResponseEntity.status(403).body(Map.of("error", "Seller or Admin access required"));
        }
        Product updated = productService.updateProduct(id, product);
        log.info("Product updated: '{}' (id={}) by {} [{}]", updated.getName(), id, user.getName(), user.getEmail());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id, HttpServletRequest request) {
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            log.warn("Forbidden: product deletion denied for role={}", user != null ? user.getRole() : "anonymous");
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        productService.deleteProduct(id);
        log.info("Product deleted: id={} by {} [{}]", id, user.getName(), user.getEmail());
        return ResponseEntity.noContent().build();
    }
}

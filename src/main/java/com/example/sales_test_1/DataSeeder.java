package com.example.sales_test_1;

import com.example.sales_test_1.model.Product;
import com.example.sales_test_1.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            System.out.println("Seeding database with sample products...");
            List<Product> products = List.of(
                new Product(null, "Wireless Headphones", "Premium sound, 30h battery", 129.99, "Electronics", 50, "https://picsum.photos/seed/headphones/400/300"),
                new Product(null, "Mechanical Keyboard", "RGB backlit, tactile switches", 89.99, "Electronics", 30, "https://picsum.photos/seed/keyboard/400/300"),
                new Product(null, "Running Shoes", "Lightweight trail runners", 74.99, "Footwear", 80, "https://picsum.photos/seed/shoes/400/300"),
                new Product(null, "Coffee Grinder", "Burr grinder, 12 grind settings", 49.99, "Kitchen", 25, "https://picsum.photos/seed/grinder/400/300"),
                new Product(null, "Yoga Mat", "Non-slip, 6mm thick", 34.99, "Sports", 60, "https://picsum.photos/seed/yoga/400/300"),
                new Product(null, "Backpack 30L", "Waterproof, laptop compartment", 59.99, "Bags", 40, "https://picsum.photos/seed/backpack/400/300"),
                new Product(null, "Desk Lamp", "LED, adjustable brightness", 44.99, "Home", 35, "https://picsum.photos/seed/lamp/400/300"),
                new Product(null, "Stainless Water Bottle", "Insulated, 750ml", 24.99, "Kitchen", 100, "https://picsum.photos/seed/bottle/400/300")
            );
            productRepository.saveAll(products);
            System.out.println("Seeded " + products.size() + " products.");
        }
    }
}

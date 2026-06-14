package com.example.sales_test_1.repository;

import com.example.sales_test_1.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// MongoRepository<Product, String> gives you CRUD for free:
//   save(), findById(), findAll(), deleteById(), count(), etc.
// Spring auto-generates the implementation at startup — no SQL or queries needed!
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // Spring Data reads method names and generates the query automatically:
    List<Product> findByCategory(String category);

    // Finds products with price <= maxPrice
    List<Product> findByPriceLessThanEqual(Double maxPrice);

    // Finds products whose name contains the search term (case-insensitive)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Finds in-stock products only
    List<Product> findByStockGreaterThan(Integer minStock);
}

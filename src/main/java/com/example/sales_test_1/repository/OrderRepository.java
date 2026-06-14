package com.example.sales_test_1.repository;

import com.example.sales_test_1.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerEmail(String email);

    List<Order> findByStatus(String status);

    // Orders sorted by creation date (newest first)
    List<Order> findAllByOrderByCreatedAtDesc();
}

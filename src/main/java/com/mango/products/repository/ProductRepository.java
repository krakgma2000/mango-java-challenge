package com.mango.products.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import com.mango.products.entity.ProductEntity;

@Repository
public class ProductRepository {

    private final JdbcClient jdbcClient;

    public ProductRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ProductEntity save(ProductEntity productEntity) {
        String sql = "INSERT INTO products (name, description) VALUES (:name, :description) RETURNING id";
        Long generatedId = jdbcClient.sql(sql)
                .param("name", productEntity.getName())
                .param("description", productEntity.getDescription())
                .query(Long.class)
                .single();
        productEntity.setId(generatedId);
        return productEntity;
    }

    //Locks and returns true if the product exists
    public boolean lockById(Long id) {
        return jdbcClient.sql("SELECT id FROM products WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(Long.class)
                .optional()
                .isPresent();
    }
}
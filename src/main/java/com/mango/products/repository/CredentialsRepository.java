package com.mango.products.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.mango.products.entity.CredentialsEntity;
import com.mango.products.entity.UserGrantEntity;

@Repository
public class CredentialsRepository {

    private final JdbcClient jdbcClient;

    public CredentialsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<CredentialsEntity> findByUsername(String username) {
        return jdbcClient.sql("SELECT id, username, password_hash FROM app_users WHERE username = :username")
                .param("username", username)
                .query((rs, rowNum) -> new CredentialsEntity(rs.getLong("id"), rs.getString("username"),
                        rs.getString("password_hash")))
                .optional();
    }

    public List<UserGrantEntity> findAllGrants() {
        return jdbcClient.sql("""
                SELECT u.username, g.endpoint
                FROM app_user_grants g
                JOIN app_users u ON u.id = g.user_id
                """)
                .query((rs, rowNum) -> new UserGrantEntity(rs.getString("username"), rs.getString("endpoint")))
                .list();
    }
}

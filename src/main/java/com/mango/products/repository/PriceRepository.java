package com.mango.products.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.mango.products.entity.PriceEntity;
import com.mango.products.entity.ProductEntity;
import com.mango.products.projection.ProductWithPricesProjection;

@Repository
public class PriceRepository {
    private static final String PRODUCT_WITH_PRICES = """
            SELECT p.id AS product_id, p.name, p.description,
                   pr.id AS price_id, pr.value, pr.currency, pr.init_date, pr.end_date
            FROM products p
            LEFT JOIN prices pr ON pr.product_id = p.id %s
            WHERE p.id = :productId
            ORDER BY pr.init_date
            """;

    private final JdbcClient jdbcClient;

    public PriceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public PriceEntity save(PriceEntity price) {
        Long generatedId = jdbcClient.sql("""
                INSERT INTO prices (product_id, value, currency, init_date, end_date)
                VALUES (:productId, :value, :currency, :initDate, :endDate)
                RETURNING id
                """)
                .param("productId", price.getProductId())
                .param("value", price.getValue())
                .param("currency", price.getCurrency())
                .param("initDate", price.getInitDate())
                .param("endDate", price.getEndDate())
                .query(Long.class)
                .single();
        price.setId(generatedId);
        return price;
    }

    public Optional<PriceEntity> findFirstOverlapping(Long productId, LocalDate initDate, LocalDate endDate) {
        return findFirstOverlapping(productId, initDate, endDate, null);
    }

    public Optional<PriceEntity> findFirstOverlapping(Long productId, LocalDate initDate, LocalDate endDate, Long excludePriceId) {
        return jdbcClient.sql("""
                SELECT id, product_id, value, currency, init_date, end_date
                FROM prices
                WHERE product_id = :productId
                  AND id IS DISTINCT FROM CAST(:excludePriceId AS bigint)
                  AND daterange(init_date, end_date, '[]')
                      && daterange(CAST(:initDate AS date), CAST(:endDate AS date), '[]')
                ORDER BY init_date
                LIMIT 1
                """)
                .param("productId", productId)
                .param("excludePriceId", excludePriceId)
                .param("initDate", initDate)
                .param("endDate", endDate)
                .query((rs, rowNum) -> toPriceEntity(rs, "id"))
                .optional();
    }

    public Optional<ProductWithPricesProjection> findProductWithPriceAt(Long productId, LocalDate date) {
        return jdbcClient.sql(PRODUCT_WITH_PRICES.formatted(
                        "AND daterange(pr.init_date, pr.end_date, '[]') @> CAST(:date AS date)"))
                .param("productId", productId)
                .param("date", date)
                .query(this::projectRowsToProductWithPrices);
    }

    public Optional<ProductWithPricesProjection> findProductWithPrices(Long productId) {
        return jdbcClient.sql(PRODUCT_WITH_PRICES.formatted(""))
                .param("productId", productId)
                .query(this::projectRowsToProductWithPrices);
    }

    private Optional<ProductWithPricesProjection> projectRowsToProductWithPrices(ResultSet rs) throws SQLException {
        ProductEntity product = null;
        List<PriceEntity> prices = new ArrayList<>();
        while (rs.next()) {
            if (product == null) {
                product = new ProductEntity(rs.getLong("product_id"), rs.getString("name"), rs.getString("description"));
            }
            if (rs.getObject("price_id") != null) {
                prices.add(toPriceEntity(rs, "price_id"));
            }
        }
        return product == null ? Optional.empty() : Optional.of(new ProductWithPricesProjection(product, prices));
    }

    private static PriceEntity toPriceEntity(ResultSet rs, String idColumn) throws SQLException {
        BigDecimal value = rs.getBigDecimal("value");
        return new PriceEntity(rs.getLong(idColumn), rs.getLong("product_id"), value, rs.getString("currency"),
                rs.getObject("init_date", LocalDate.class), rs.getObject("end_date", LocalDate.class));
    }

    public Optional<Long> findProductIdByPriceId(Long priceId) {
        return jdbcClient.sql("SELECT product_id FROM prices WHERE id = :priceId")
                .param("priceId", priceId)
                .query(Long.class)
                .optional();
    }

    public boolean update(PriceEntity price) {
        return jdbcClient.sql("""
                UPDATE prices
                SET value = :value, currency = :currency, init_date = :initDate, end_date = :endDate
                WHERE id = :id
                """)
                .param("id", price.getId())
                .param("value", price.getValue())
                .param("currency", price.getCurrency())
                .param("initDate", price.getInitDate())
                .param("endDate", price.getEndDate())
                .update() > 0;
    }

    public boolean deleteById(Long priceId) {
        return jdbcClient.sql("DELETE FROM prices WHERE id = :priceId")
                .param("priceId", priceId)
                .update() > 0;
    }
}
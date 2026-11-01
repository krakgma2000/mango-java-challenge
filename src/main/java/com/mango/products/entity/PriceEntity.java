package com.mango.products.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceEntity {

    private Long id;
    private Long productId;
    private BigDecimal value;
    private String currency;
    private LocalDate initDate;
    private LocalDate endDate;

    public PriceEntity(Long id, Long productId, BigDecimal value, String currency, LocalDate initDate, LocalDate endDate) {
        this.id = id;
        this.productId = productId;
        this.value = value;
        this.currency = currency;
        this.initDate = initDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getInitDate() {
        return initDate;
    }

    public void setInitDate(LocalDate initDate) {
        this.initDate = initDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
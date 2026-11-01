package com.mango.products.domain;

import java.math.BigDecimal;

public class CurrentPriceResponseDTO {

    private Long id;
    private BigDecimal value;
    private String currency;

    public CurrentPriceResponseDTO(Long id, BigDecimal value, String currency) {
        this.id = id;
        this.value = value;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

}
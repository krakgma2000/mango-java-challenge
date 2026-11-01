package com.mango.products.domain;

import java.util.List;

public class ProductPricesResponseDTO {

    private Long id;
    private String name;
    private String description;
    private List<PriceResponseDTO> prices;

    public ProductPricesResponseDTO(Long id, String name, String description, List<PriceResponseDTO> prices) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prices = prices;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PriceResponseDTO> getPrices() {
        return prices;
    }

    public void setPrices(List<PriceResponseDTO> prices) {
        this.prices = prices;
    }
}
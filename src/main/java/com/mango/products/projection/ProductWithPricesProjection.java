package com.mango.products.projection;

import com.mango.products.entity.ProductEntity;
import com.mango.products.entity.PriceEntity;
import java.util.List;

public class ProductWithPricesProjection {    
    private ProductEntity product;
    private List<PriceEntity> prices;

    public ProductWithPricesProjection(ProductEntity product, List<PriceEntity> prices) {
        this.product = product;
        this.prices = prices;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public List<PriceEntity> getPrices() {
        return prices;
    }

    public void setPrices(List<PriceEntity> prices) {
        this.prices = prices;
    }
}
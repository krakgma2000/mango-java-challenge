package com.mango.products.vo;

import java.util.List;

public class ProductPricesVO {

    private ProductVO product;
    private List<PriceVO> prices;

    public ProductPricesVO(ProductVO product, List<PriceVO> prices) {
        this.product = product;
        this.prices = prices;
    }

    public ProductVO getProduct() {
        return product;
    }

    public void setProduct(ProductVO product) {
        this.product = product;
    }

    public List<PriceVO> getPrices() {
        return prices;
    }

    public void setPrices(List<PriceVO> prices) {
        this.prices = prices;
    }
}
package com.mango.products.service;

import org.springframework.stereotype.Service;

import com.mango.products.entity.ProductEntity;
import com.mango.products.mapper.ProductMapper;
import com.mango.products.repository.ProductRepository;
import com.mango.products.vo.ProductVO;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductVO createProduct(ProductVO productVO) {

        ProductEntity product = ProductMapper.toEntity(productVO);
        product = productRepository.save(product);
        return ProductMapper.toVO(product);
    }
}
package com.mango.products.business;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mango.products.domain.CreateProductRequestDTO;
import com.mango.products.domain.ProductResponseDTO;
import com.mango.products.mapper.ProductMapper;
import com.mango.products.service.ProductService;
import com.mango.products.utils.Validator;
import com.mango.products.vo.ProductVO;

@Component
public class ProductBusiness {
    private final ProductService productService;

    public ProductBusiness(ProductService productService) {
        this.productService = productService;
    }

    @Transactional
    public ProductResponseDTO createProduct(CreateProductRequestDTO request) {
        validateRequest(request);

        ProductVO productVO = productService.createProduct(ProductMapper.toVO(request));

        return ProductMapper.toResponse(productVO);
    }

    private void validateRequest(CreateProductRequestDTO request) {
        Validator.requireNotBlank(request.getName(), "name");
    }
}
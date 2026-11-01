package com.mango.products.business;

import org.springframework.stereotype.Component;

import com.mango.products.domain.CreateProductRequestDTO;
import com.mango.products.domain.ProductResponseDTO;
import com.mango.products.mapper.ProductMapper;
import com.mango.products.service.ProductService;
import com.mango.products.utils.Validator;
import com.mango.products.vo.ProductVO;

@Component
public class ProductBusiness {

    static final int NAME_MAX_LENGTH = 255;
    static final int DESCRIPTION_MAX_LENGTH = 1000;

    private final ProductService productService;

    public ProductBusiness(ProductService productService) {
        this.productService = productService;
    }

    public ProductResponseDTO createProduct(CreateProductRequestDTO request) {
        validateRequest(request);

        ProductVO productVO = productService.createProduct(ProductMapper.toVO(request));

        return ProductMapper.toResponse(productVO);
    }

    private void validateRequest(CreateProductRequestDTO request) {
        Validator.requireNotBlank(request.getName(), "name");
        Validator.requireMaxLength(request.getName(), NAME_MAX_LENGTH, "name");
        Validator.requireNoNullCharacter(request.getName(), "name");
        Validator.requireMaxLength(request.getDescription(), DESCRIPTION_MAX_LENGTH, "description");
        Validator.requireNoNullCharacter(request.getDescription(), "description");
    }
}
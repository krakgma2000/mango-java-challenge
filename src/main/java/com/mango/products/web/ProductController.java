package com.mango.products.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mango.products.business.ProductBusiness;
import com.mango.products.domain.CreateProductRequestDTO;
import com.mango.products.domain.ProductResponseDTO;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductBusiness  productBusiness;

    public ProductController(ProductBusiness productBusiness) {
        this.productBusiness = productBusiness;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@RequestBody CreateProductRequestDTO request) {
        ProductResponseDTO response = productBusiness.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
package com.mango.products.web;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mango.products.business.PriceBusiness;
import com.mango.products.domain.CreatePriceRequestDTO;
import com.mango.products.domain.CurrentPriceResponseDTO;
import com.mango.products.domain.PriceResponseDTO;
import com.mango.products.domain.ProductPricesResponseDTO;

@RestController
@RequestMapping("/products/prices")
public class PriceController {

    private final PriceBusiness priceBusiness;

    public PriceController(PriceBusiness priceBusiness) {
        this.priceBusiness = priceBusiness;
    }

    @PostMapping
    public ResponseEntity<PriceResponseDTO> createPrice(@RequestParam(required = true) Long productId, @RequestBody CreatePriceRequestDTO request) {
        PriceResponseDTO response = priceBusiness.createPrice(productId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CurrentPriceResponseDTO> getPriceAt(@RequestParam(required = true) Long productId,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Optional<CurrentPriceResponseDTO> response = priceBusiness.getPriceAt(productId, date);

        if (!response.isPresent()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response.get());
    }

    @GetMapping("/history")
    public ResponseEntity<ProductPricesResponseDTO> getPriceHistory(@RequestParam(required = true) Long productId) {
        return ResponseEntity.ok(priceBusiness.getPriceHistory(productId));
    }
}
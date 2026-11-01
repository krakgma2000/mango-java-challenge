package com.mango.products.business;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mango.products.domain.CreatePriceRequestDTO;
import com.mango.products.domain.CurrentPriceResponseDTO;
import com.mango.products.domain.PriceResponseDTO;
import com.mango.products.domain.ProductPricesResponseDTO;
import com.mango.products.exceptions.ValidationException;
import com.mango.products.mapper.PriceMapper;
import com.mango.products.service.PriceService;
import com.mango.products.utils.Validator;
import com.mango.products.vo.PriceVO;
import com.mango.products.vo.ProductPricesVO;

@Component
public class PriceBusiness {

    static final String DEFAULT_CURRENCY = "EUR";
    static final int MAX_DECIMALS = 2;
    static final BigDecimal MAX_PRICE_VALUE = new BigDecimal("1000000000");

    private final PriceService priceService;

    public PriceBusiness(PriceService priceService) {
        this.priceService = priceService;
    }

    public PriceResponseDTO createPrice(Long productId, CreatePriceRequestDTO request) {
        validateRequest(request);

        PriceVO price = PriceMapper.toVO(productId, request);
        if (price.getCurrency() == null) {
            price.setCurrency(DEFAULT_CURRENCY);
        }

        return PriceMapper.toResponse(priceService.createPrice(price));
    }

    public Optional<CurrentPriceResponseDTO> getPriceAt(Long productId, LocalDate date) {
        ProductPricesVO productWithPrices = priceService.findProductWithPriceAt(productId, date)
                .orElseThrow(() -> new ValidationException("Product " + productId + " does not exist"));

        if (productWithPrices.getPrices() == null || productWithPrices.getPrices().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(PriceMapper.toCurrentPriceResponse(productWithPrices.getPrices().get(0)));
    }

    public ProductPricesResponseDTO getPriceHistory(Long productId) {
        ProductPricesVO result = priceService.findProductWithPrices(productId)
                .orElseThrow(() -> new ValidationException("Product " + productId + " does not exist"));
        return PriceMapper.toResponse(result);
    }

    private void validateRequest(CreatePriceRequestDTO request) {
        Validator.requireNotNull(request.getValue(), "value");
        Validator.requireNotNegative(request.getValue(), "value");
        Validator.requireMaxDecimals(request.getValue(), MAX_DECIMALS, "value");
        Validator.requireLowerThan(request.getValue(), MAX_PRICE_VALUE, "value");
        Validator.requireCurrencyCode(request.getCurrency(), "currency");
        Validator.requireNotNull(request.getInitDate(), "initDate");
        Validator.requireBefore(request.getInitDate(), request.getEndDate(), "initDate", "endDate");
    }

}
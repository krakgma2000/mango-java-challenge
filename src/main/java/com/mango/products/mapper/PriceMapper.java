package com.mango.products.mapper;

import java.util.List;

import com.mango.products.domain.CreatePriceRequestDTO;
import com.mango.products.domain.CurrentPriceResponseDTO;
import com.mango.products.domain.PriceResponseDTO;
import com.mango.products.domain.ProductPricesResponseDTO;
import com.mango.products.entity.PriceEntity;
import com.mango.products.projection.ProductWithPricesProjection;
import com.mango.products.vo.PriceVO;
import com.mango.products.vo.ProductPricesVO;
import com.mango.products.vo.ProductVO;
import com.mango.products.utils.Money;

public final class PriceMapper {

    private PriceMapper() {
    }

    public static PriceVO toVO(Long productId, CreatePriceRequestDTO request) {
        return new PriceVO(null, productId, request.getValue(), request.getCurrency(),
                request.getInitDate(), request.getEndDate());
    }

    public static PriceVO toVO(PriceEntity entity) {
        return new PriceVO(entity.getId(), entity.getProductId(), entity.getValue(), entity.getCurrency(),
                entity.getInitDate(), entity.getEndDate());
    }

    public static ProductPricesVO toVO(ProductWithPricesProjection projection) {
        return new ProductPricesVO(ProductMapper.toVO(projection.getProduct()), projection.getPrices().stream().map(PriceMapper::toVO).toList());
    }

    public static PriceEntity toEntity(PriceVO vo) {
        return new PriceEntity(vo.getId(), vo.getProductId(), vo.getValue(), vo.getCurrency(),
                vo.getInitDate(), vo.getEndDate());
    }

    public static PriceResponseDTO toResponse(PriceVO vo) {
        return new PriceResponseDTO(vo.getId(), Money.format(vo.getValue()), vo.getCurrency(),
                vo.getInitDate(), vo.getEndDate());
    }

    public static CurrentPriceResponseDTO toCurrentPriceResponse(PriceVO vo) {
        return new CurrentPriceResponseDTO(vo.getId(), Money.format(vo.getValue()), vo.getCurrency());
    }

    public static ProductPricesResponseDTO toResponse(ProductPricesVO vo) {
        ProductVO product = vo.getProduct();
        List<PriceResponseDTO> prices = vo.getPrices().stream().map(PriceMapper::toResponse).toList();
        return new ProductPricesResponseDTO(product.getId(), product.getName(), product.getDescription(), prices);
    }

    public static PriceVO toVOForUpdate(Long priceId, CreatePriceRequestDTO request) {
        return new PriceVO(priceId, null, request.getValue(), request.getCurrency(),
                request.getInitDate(), request.getEndDate());
    }
}
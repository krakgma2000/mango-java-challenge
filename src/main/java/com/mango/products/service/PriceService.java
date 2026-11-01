package com.mango.products.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mango.products.entity.PriceEntity;
import com.mango.products.exceptions.ConflictException;
import com.mango.products.exceptions.ValidationException;
import com.mango.products.mapper.PriceMapper;
import com.mango.products.repository.PriceRepository;
import com.mango.products.repository.ProductRepository;
import com.mango.products.vo.PriceVO;
import com.mango.products.vo.ProductPricesVO;

@Service
public class PriceService {

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;

    public PriceService(ProductRepository productRepository, PriceRepository priceRepository) {
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
    }

    @Transactional
    public PriceVO createPrice(PriceVO price) {
        if (!productRepository.lockById(price.getProductId())) {
            throw new ValidationException("Product " + price.getProductId() + " does not exist");
        }

        priceRepository.findFirstOverlapping(price.getProductId(), price.getInitDate(), price.getEndDate())
                .ifPresent(existing -> {
                    throw new ConflictException(buildOverlapMessage(existing));
                });

        return PriceMapper.toVO(priceRepository.save(PriceMapper.toEntity(price)));
    }

    public Optional<ProductPricesVO> findProductWithPriceAt(Long productId, LocalDate date) {
        return priceRepository.findProductWithPriceAt(productId, date).map(PriceMapper::toVO);
    }

    public Optional<ProductPricesVO> findProductWithPrices(Long productId) {
        return priceRepository.findProductWithPrices(productId).map(PriceMapper::toVO);
    }

    private static String buildOverlapMessage(PriceEntity existing) {
        String end = existing.getEndDate() == null ? "open" : existing.getEndDate().toString();
        return "The price overlaps with price " + existing.getId()
                + " (" + existing.getInitDate() + " to " + end + ")";
    }
}
package com.mango.products.mapper;

import com.mango.products.domain.CreateProductRequestDTO;
import com.mango.products.domain.ProductResponseDTO;
import com.mango.products.entity.ProductEntity;
import com.mango.products.vo.ProductVO;

public final class ProductMapper {

	private ProductMapper() {
	}

	public static ProductVO toVO(CreateProductRequestDTO request) {
		return new ProductVO(null, request.getName(), request.getDescription());
	}

	public static ProductVO toVO(ProductEntity entity) {
		return new ProductVO(entity.getId(), entity.getName(), entity.getDescription());
	}

	public static ProductEntity toEntity(ProductVO vo) {
		return new ProductEntity(vo.getId(), vo.getName(), vo.getDescription());
	}

	public static ProductResponseDTO toResponse(ProductVO vo) {
		return new ProductResponseDTO(vo.getId(), vo.getName(), vo.getDescription());
	}
}
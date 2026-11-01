package com.mango.products.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mango.products.domain.CreateProductRequestDTO;
import com.mango.products.domain.ProductResponseDTO;
import com.mango.products.exceptions.ValidationException;
import com.mango.products.service.ProductService;
import com.mango.products.vo.ProductVO;

@ExtendWith(MockitoExtension.class)
class ProductBusinessTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductBusiness productBusiness;

    @Captor
    private ArgumentCaptor<ProductVO> productCaptor;

    @Test
    void createsAValidProductAndReturnsItWithItsId() {
        when(productService.createProduct(any()))
                .thenReturn(new ProductVO(1L, "Sports Shoes", "2026 Limited Edition"));

        ProductResponseDTO response = productBusiness.createProduct(
                new CreateProductRequestDTO("Sports Shoes", "2026 Limited Edition"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Sports Shoes");
        assertThat(response.getDescription()).isEqualTo("2026 Limited Edition");
    }

    @Test
    void passesTheRequestToTheServiceWithoutAnId() {
        when(productService.createProduct(any()))
                .thenReturn(new ProductVO(1L, "Sports Shoes", "2026 Limited Edition"));

        productBusiness.createProduct(new CreateProductRequestDTO("Sports Shoes", "2026 Limited Edition"));

        verify(productService).createProduct(productCaptor.capture());
        assertThat(productCaptor.getValue().getId()).isNull();
        assertThat(productCaptor.getValue().getName()).isEqualTo("Sports Shoes");
        assertThat(productCaptor.getValue().getDescription()).isEqualTo("2026 Limited Edition");
    }

    @Test
    void descriptionIsOptional() {
        when(productService.createProduct(any())).thenReturn(new ProductVO(1L, "Sports Shoes", null));

        ProductResponseDTO response = productBusiness.createProduct(new CreateProductRequestDTO("Sports Shoes", null));

        assertThat(response.getDescription()).isNull();
    }

    @Test
    void rejectsABlankNameWithoutTouchingTheDatabase() {
        CreateProductRequestDTO request = new CreateProductRequestDTO(" ", "2026 Limited Edition");

        assertThatThrownBy(() -> productBusiness.createProduct(request)).isInstanceOf(ValidationException.class);
        verify(productService, never()).createProduct(any());
    }

    @Test
    void rejectsANameWithTheNulCharacter() {
        CreateProductRequestDTO request = new CreateProductRequestDTO("a\u0000b", null);

        assertThatThrownBy(() -> productBusiness.createProduct(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("name cannot contain the NUL character");
        verify(productService, never()).createProduct(any());
    }

    @Test
    void rejectsANameLongerThanTheLimit() {
        CreateProductRequestDTO request = new CreateProductRequestDTO("x".repeat(ProductBusiness.NAME_MAX_LENGTH + 1), null);

        assertThatThrownBy(() -> productBusiness.createProduct(request)).isInstanceOf(ValidationException.class);
        verify(productService, never()).createProduct(any());
    }

    @Test
    void rejectsADescriptionLongerThanTheLimit() {
        CreateProductRequestDTO request = new CreateProductRequestDTO("Sports Shoes",
                "x".repeat(ProductBusiness.DESCRIPTION_MAX_LENGTH + 1));

        assertThatThrownBy(() -> productBusiness.createProduct(request)).isInstanceOf(ValidationException.class);
        verify(productService, never()).createProduct(any());
    }
}
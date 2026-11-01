package com.mango.products.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mango.products.business.ProductBusiness;
import com.mango.products.domain.CreateProductRequestDTO;
import com.mango.products.domain.ProductResponseDTO;
import com.mango.products.exceptions.ValidationException;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductBusiness productBusiness;

    @Test
    void createReturns201WithTheProductAndItsId() throws Exception {
        when(productBusiness.createProduct(any()))
                .thenReturn(new ProductResponseDTO(1L, "Sports Shoes", "2026 Limited Edition"));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Sports Shoes", "description": "2026 Limited Edition"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sports Shoes"))
                .andExpect(jsonPath("$.description").value("2026 Limited Edition"));
    }

    @Test
    void readsTheJsonBodyIntoTheRequestDto() throws Exception {
        when(productBusiness.createProduct(any()))
                .thenReturn(new ProductResponseDTO(1L, "Sports Shoes", null));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Sports Shoes\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateProductRequestDTO> captor = ArgumentCaptor.forClass(CreateProductRequestDTO.class);
        verify(productBusiness).createProduct(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Sports Shoes");
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void validationErrorReturns400AsProblemDetail() throws Exception {
        when(productBusiness.createProduct(any()))
                .thenThrow(new ValidationException("name cannot be null or blank"));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("name cannot be null or blank"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(productBusiness.createProduct(any()))
                .thenThrow(new IllegalStateException("connection refused to db:5432"));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Sports Shoes\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error occurred"));
    }
}
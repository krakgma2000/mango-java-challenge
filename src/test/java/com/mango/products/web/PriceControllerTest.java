package com.mango.products.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.mango.products.business.PriceBusiness;
import com.mango.products.domain.CurrentPriceResponseDTO;
import com.mango.products.domain.PriceResponseDTO;
import com.mango.products.domain.ProductPricesResponseDTO;
import com.mango.products.exceptions.ConflictException;
import com.mango.products.exceptions.ValidationException;

/**
 * Web layer only (no database): routes, JSON, status codes and the error handler.
 */
@WebMvcTest(PriceController.class)
class PriceControllerTest {

    private static final LocalDate JAN_1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate JUN_30 = LocalDate.of(2024, 6, 30);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceBusiness priceBusiness;

    private ResultActions postPrice(String query, String body) throws Exception {
        return mockMvc.perform(post("/products/prices" + query)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ---------- POST ----------

    @Test
    void createReturns201WithThePriceAndTwoDecimals() throws Exception {
        when(priceBusiness.createPrice(eq(1L), any()))
                .thenReturn(new PriceResponseDTO(10L, new BigDecimal("99.90"), "EUR", JAN_1, JUN_30));

        postPrice("?productId=1", """
                {"value": 99.9, "initDate": "2024-01-01", "endDate": "2024-06-30"}
                """)
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {"id": 10, "value": 99.90, "currency": "EUR", "initDate": "2024-01-01", "endDate": "2024-06-30"}
                        """, true))
                .andExpect(content().string(containsString("\"value\":99.90")));
    }

    @Test
    void anOpenPriceKeepsEndDateAsNull() throws Exception {
        when(priceBusiness.createPrice(eq(1L), any()))
                .thenReturn(new PriceResponseDTO(10L, new BigDecimal("99.90"), "EUR", JAN_1, null));

        postPrice("?productId=1", """
                {"value": 99.9, "initDate": "2024-01-01", "endDate": null}
                """)
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"endDate\":null")));
    }

    @Test
    void createWithoutProductIdReturns400() throws Exception {
        postPrice("", "{\"value\": 1, \"initDate\": \"2024-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void overlapReturns409() throws Exception {
        when(priceBusiness.createPrice(eq(1L), any()))
                .thenThrow(new ConflictException("The price overlaps with price 5 (2024-01-01 to open)"));

        postPrice("?productId=1", "{\"value\": 1, \"initDate\": \"2024-01-01\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The price overlaps with price 5 (2024-01-01 to open)"));
    }

    @Test
    void theExclusionConstraintViolationReturns409() throws Exception {
        when(priceBusiness.createPrice(eq(1L), any())).thenThrow(new DataIntegrityViolationException("insert",
                new SQLException("conflicting key value violates exclusion constraint", "23P01")));

        postPrice("?productId=1", "{\"value\": 1, \"initDate\": \"2024-01-01\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void anyOtherIntegrityViolationReturns500() throws Exception {
        when(priceBusiness.createPrice(eq(1L), any())).thenThrow(new DataIntegrityViolationException("insert",
                new SQLException("null value in column violates not-null constraint", "23502")));

        postPrice("?productId=1", "{\"value\": 1, \"initDate\": \"2024-01-01\"}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error occurred"));
    }

    @Test
    void productThatDoesNotExistReturns400() throws Exception {
        when(priceBusiness.createPrice(eq(99L), any())).thenThrow(new ValidationException("Product 99 does not exist"));

        postPrice("?productId=99", "{\"value\": 1, \"initDate\": \"2024-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Product 99 does not exist"));
    }

    // ---------- GET price at a date ----------

    @Test
    void priceAtADateReturns200() throws Exception {
        when(priceBusiness.getPriceAt(1L, LocalDate.of(2024, 4, 15)))
                .thenReturn(Optional.of(new CurrentPriceResponseDTO(10L, new BigDecimal("99.99"), "EUR")));

        mockMvc.perform(get("/products/prices").param("productId", "1").param("date", "2024-04-15"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\": 10, \"value\": 99.99, \"currency\": \"EUR\"}", true));
    }

    @Test
    void noPriceAtTheDateReturns204WithoutBody() throws Exception {
        when(priceBusiness.getPriceAt(1L, JAN_1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/prices").param("productId", "1").param("date", "2024-01-01"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void malformedDateReturns400() throws Exception {
        mockMvc.perform(get("/products/prices").param("productId", "1").param("date", "2024-13-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void nonNumericProductIdReturns400() throws Exception {
        mockMvc.perform(get("/products/prices").param("productId", "abc").param("date", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWithoutProductIdReturns400() throws Exception {
        mockMvc.perform(get("/products/prices").param("date", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWithoutDateReturns400() throws Exception {
        mockMvc.perform(get("/products/prices").param("productId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Required parameter 'date' is not present."));
    }

    // ---------- GET history ----------

    @Test
    void historyReturnsTheProductAndItsPrices() throws Exception {
        when(priceBusiness.getPriceHistory(1L)).thenReturn(new ProductPricesResponseDTO(1L, "Sports Shoes",
                "2026 Limited Edition", List.of(
                        new PriceResponseDTO(10L, new BigDecimal("99.99"), "EUR", JAN_1, JUN_30),
                        new PriceResponseDTO(11L, new BigDecimal("199.99"), "EUR", LocalDate.of(2025, 1, 1), null))));

        mockMvc.perform(get("/products/prices/history").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sports Shoes"))
                .andExpect(jsonPath("$.prices.length()").value(2))
                .andExpect(jsonPath("$.prices[0].initDate").value("2024-01-01"))
                .andExpect(jsonPath("$.prices[1].endDate").value(nullValue()))
                .andExpect(content().string(containsString("\"endDate\":null")));
    }

    @Test
    void historyWithoutProductIdReturns400() throws Exception {
        mockMvc.perform(get("/products/prices/history"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyWithoutPricesReturnsAnEmptyList() throws Exception {
        when(priceBusiness.getPriceHistory(1L))
                .thenReturn(new ProductPricesResponseDTO(1L, "Sports Shoes", null, List.of()));

        mockMvc.perform(get("/products/prices/history").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prices").isEmpty());
    }
}
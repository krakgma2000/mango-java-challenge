package com.mango.products.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mango.products.domain.CreatePriceRequestDTO;
import com.mango.products.domain.CurrentPriceResponseDTO;
import com.mango.products.domain.PriceResponseDTO;
import com.mango.products.domain.ProductPricesResponseDTO;
import com.mango.products.exceptions.ValidationException;
import com.mango.products.service.PriceService;
import com.mango.products.vo.PriceVO;
import com.mango.products.vo.ProductPricesVO;
import com.mango.products.vo.ProductVO;

@ExtendWith(MockitoExtension.class)
class PriceBusinessTest {

    private static final LocalDate JAN_1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate JUN_30 = LocalDate.of(2024, 6, 30);
    private static final ProductVO PRODUCT = new ProductVO(1L, "Sports Shoes", "2026 Limited Edition");

    @Mock
    private PriceService priceService;

    @InjectMocks
    private PriceBusiness priceBusiness;

    @Captor
    private ArgumentCaptor<PriceVO> priceCaptor;

    private static CreatePriceRequestDTO request(String value, String currency, LocalDate initDate, LocalDate endDate) {
        return new CreatePriceRequestDTO(value == null ? null : new BigDecimal(value), currency, initDate, endDate);
    }

    // ---------- createPrice ----------

    @Test
    void createsAPriceAndReturnsItWithTwoDecimals() {
        when(priceService.createPrice(any()))
                .thenReturn(new PriceVO(10L, 1L, new BigDecimal("99.9"), "EUR", JAN_1, JUN_30));

        PriceResponseDTO response = priceBusiness.createPrice(1L, request("99.9", "EUR", JAN_1, JUN_30));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getValue()).isEqualTo(new BigDecimal("99.90"));
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getInitDate()).isEqualTo(JAN_1);
        assertThat(response.getEndDate()).isEqualTo(JUN_30);
    }

    @Test
    void usesTheDefaultCurrencyWhenNoneIsSent() {
        when(priceService.createPrice(any()))
                .thenReturn(new PriceVO(10L, 1L, new BigDecimal("99.99"), "EUR", JAN_1, null));

        priceBusiness.createPrice(1L, request("99.99", null, JAN_1, null));

        verify(priceService).createPrice(priceCaptor.capture());
        assertThat(priceCaptor.getValue().getCurrency()).isEqualTo(PriceBusiness.DEFAULT_CURRENCY);
        assertThat(priceCaptor.getValue().getProductId()).isEqualTo(1L);
        assertThat(priceCaptor.getValue().getId()).isNull();
    }

    @Test
    void keepsTheCurrencyWhenItIsSent() {
        when(priceService.createPrice(any()))
                .thenReturn(new PriceVO(10L, 1L, new BigDecimal("99.99"), "USD", JAN_1, null));

        priceBusiness.createPrice(1L, request("99.99", "USD", JAN_1, null));

        verify(priceService).createPrice(priceCaptor.capture());
        assertThat(priceCaptor.getValue().getCurrency()).isEqualTo("USD");
    }

    @Test
    void acceptsAZeroValue() {
        when(priceService.createPrice(any()))
                .thenReturn(new PriceVO(10L, 1L, BigDecimal.ZERO, "EUR", JAN_1, null));

        assertThat(priceBusiness.createPrice(1L, request("0", null, JAN_1, null)).getValue())
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void rejectsAMissingValue() {
        assertRejected(request(null, null, JAN_1, null), "value cannot be null");
    }

    @Test
    void rejectsANegativeValue() {
        assertRejected(request("-1", null, JAN_1, null), "value cannot be negative");
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        assertRejected(request("99.999", null, JAN_1, null), "value cannot have more than 2 decimals");
    }

    @Test
    void rejectsAValueTooBig() {
        assertRejected(request("1000000000", null, JAN_1, null), "value must be lower than 1000000000");
    }

    @Test
    void rejectsAnInvalidCurrency() {
        assertRejected(request("1", "eur", JAN_1, null), "currency must be 3 uppercase letters");
    }

    @Test
    void rejectsAMissingInitDate() {
        assertRejected(request("1", null, null, JUN_30), "initDate cannot be null");
    }

    @Test
    void rejectsAPriceThatStartsAndEndsTheSameDay() {
        assertRejected(request("1", null, JAN_1, JAN_1), "initDate must be before endDate");
    }

    @Test
    void rejectsAnEndDateBeforeTheInitDate() {
        assertRejected(request("1", null, JUN_30, JAN_1), "initDate must be before endDate");
    }

    private void assertRejected(CreatePriceRequestDTO request, String message) {
        assertThatThrownBy(() -> priceBusiness.createPrice(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage(message);
        verify(priceService, never()).createPrice(any());
    }

    // ---------- getPriceAt ----------

    @Test
    void returnsThePriceInForceOnTheDate() {
        when(priceService.findProductWithPriceAt(1L, JAN_1)).thenReturn(Optional.of(new ProductPricesVO(PRODUCT,
                List.of(new PriceVO(10L, 1L, new BigDecimal("99.9"), "EUR", JAN_1, JUN_30)))));

        Optional<CurrentPriceResponseDTO> response = priceBusiness.getPriceAt(1L, JAN_1);

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo(10L);
        assertThat(response.get().getValue()).isEqualTo(new BigDecimal("99.90"));
        assertThat(response.get().getCurrency()).isEqualTo("EUR");
    }

    @Test
    void returnsEmptyWhenTheProductHasNoPriceOnTheDate() {
        when(priceService.findProductWithPriceAt(1L, JAN_1))
                .thenReturn(Optional.of(new ProductPricesVO(PRODUCT, List.of())));

        assertThat(priceBusiness.getPriceAt(1L, JAN_1)).isEmpty();
    }

    @Test
    void getPriceAtRejectsAProductThatDoesNotExist() {
        when(priceService.findProductWithPriceAt(99L, JAN_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceBusiness.getPriceAt(99L, JAN_1))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product 99 does not exist");
    }

    // ---------- getPriceHistory ----------

    @Test
    void returnsTheProductWithItsPrices() {
        when(priceService.findProductWithPrices(1L)).thenReturn(Optional.of(new ProductPricesVO(PRODUCT, List.of(
                new PriceVO(10L, 1L, new BigDecimal("99.99"), "EUR", JAN_1, JUN_30),
                new PriceVO(11L, 1L, new BigDecimal("199.99"), "EUR", LocalDate.of(2025, 1, 1), null)))));

        ProductPricesResponseDTO response = priceBusiness.getPriceHistory(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Sports Shoes");
        assertThat(response.getDescription()).isEqualTo("2026 Limited Edition");
        assertThat(response.getPrices()).extracting(PriceResponseDTO::getId).containsExactly(10L, 11L);
        assertThat(response.getPrices().get(1).getEndDate()).isNull();
    }

    @Test
    void returnsAnEmptyListWhenTheProductHasNoPrices() {
        when(priceService.findProductWithPrices(1L)).thenReturn(Optional.of(new ProductPricesVO(PRODUCT, List.of())));

        assertThat(priceBusiness.getPriceHistory(1L).getPrices()).isEmpty();
    }

    @Test
    void getPriceHistoryRejectsAProductThatDoesNotExist() {
        when(priceService.findProductWithPrices(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceBusiness.getPriceHistory(99L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product 99 does not exist");
    }
}
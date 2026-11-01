package com.mango.products.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mango.products.entity.PriceEntity;
import com.mango.products.exceptions.ConflictException;
import com.mango.products.exceptions.ValidationException;
import com.mango.products.repository.PriceRepository;
import com.mango.products.repository.ProductRepository;
import com.mango.products.vo.PriceVO;


@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    private static final LocalDate JAN_1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate JUN_30 = LocalDate.of(2024, 6, 30);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private PriceService priceService;

    private static PriceVO newPrice() {
        return new PriceVO(null, 1L, new BigDecimal("99.99"), "EUR", JAN_1, JUN_30);
    }

    @Test
    void locksTheProductThenChecksOverlapsThenSaves() {
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.findFirstOverlapping(1L, JAN_1, JUN_30)).thenReturn(Optional.empty());
        when(priceRepository.save(any())).thenAnswer(invocation -> {
            PriceEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        PriceVO created = priceService.createPrice(newPrice());

        assertThat(created.getId()).isEqualTo(10L);
        InOrder order = inOrder(productRepository, priceRepository);
        order.verify(productRepository).lockById(1L);
        order.verify(priceRepository).findFirstOverlapping(1L, JAN_1, JUN_30);
        order.verify(priceRepository).save(any());
    }

    @Test
    void rejectsAProductThatDoesNotExist() {
        when(productRepository.lockById(1L)).thenReturn(false);

        assertThatThrownBy(() -> priceService.createPrice(newPrice()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product 1 does not exist");
        verify(priceRepository, never()).save(any());
    }

    @Test
    void rejectsAnOverlapNamingTheExistingPrice() {
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.findFirstOverlapping(1L, JAN_1, JUN_30)).thenReturn(Optional.of(
                new PriceEntity(5L, 1L, new BigDecimal("199.99"), "EUR", LocalDate.of(2024, 3, 1), null)));

        assertThatThrownBy(() -> priceService.createPrice(newPrice()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("The price overlaps with price 5 (2024-03-01 to open)");
        verify(priceRepository, never()).save(any());
    }

    // ---------- updatePrice ----------

    private static PriceVO priceToUpdate() {
        return new PriceVO(10L, null, new BigDecimal("150"), "EUR", JAN_1, JUN_30);
    }

    @Test
    void updateLocksTheProductOfThePriceThenChecksOverlapsExcludingItselfThenUpdates() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L));
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.findFirstOverlapping(1L, JAN_1, JUN_30, 10L)).thenReturn(Optional.empty());
        when(priceRepository.update(any())).thenReturn(true);

        PriceVO updated = priceService.updatePrice(priceToUpdate());

        assertThat(updated.getId()).isEqualTo(10L);
        assertThat(updated.getProductId()).isEqualTo(1L);
        InOrder order = inOrder(productRepository, priceRepository);
        order.verify(priceRepository).findProductIdByPriceId(10L);
        order.verify(productRepository).lockById(1L);
        order.verify(priceRepository).findFirstOverlapping(1L, JAN_1, JUN_30, 10L);
        order.verify(priceRepository).update(any());
    }

    @Test
    void updateRejectsAPriceThatDoesNotExist() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.updatePrice(priceToUpdate()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Price 10 does not exist");
        verify(productRepository, never()).lockById(any());
        verify(priceRepository, never()).update(any());
    }

    @Test
    void updateRejectsAnOverlapWithAnotherPrice() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L));
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.findFirstOverlapping(1L, JAN_1, JUN_30, 10L)).thenReturn(Optional.of(
                new PriceEntity(11L, 1L, new BigDecimal("199.99"), "EUR", LocalDate.of(2024, 6, 1), null)));

        assertThatThrownBy(() -> priceService.updatePrice(priceToUpdate()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("The price overlaps with price 11 (2024-06-01 to open)");
        verify(priceRepository, never()).update(any());
    }

    @Test
    void updateRejectsAPriceDeletedWhileWaitingForTheLock() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L));
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.findFirstOverlapping(1L, JAN_1, JUN_30, 10L)).thenReturn(Optional.empty());
        when(priceRepository.update(any())).thenReturn(false);

        assertThatThrownBy(() -> priceService.updatePrice(priceToUpdate()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Price 10 does not exist");
    }

    // ---------- deletePrice ----------

    @Test
    void deleteLocksTheProductOfThePriceThenDeletes() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L));
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.deleteById(10L)).thenReturn(true);

        priceService.deletePrice(10L);

        InOrder order = inOrder(productRepository, priceRepository);
        order.verify(productRepository).lockById(1L);
        order.verify(priceRepository).deleteById(10L);
    }

    @Test
    void deleteRejectsAPriceThatDoesNotExist() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.deletePrice(10L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Price 10 does not exist");
        verify(priceRepository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsAPriceDeletedWhileWaitingForTheLock() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L));
        when(productRepository.lockById(1L)).thenReturn(true);
        when(priceRepository.deleteById(10L)).thenReturn(false);

        assertThatThrownBy(() -> priceService.deletePrice(10L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Price 10 does not exist");
    }

    // ---------- price deleted while waiting for the lock ----------

    @Test
    void updateRejectsAPriceDeletedWhileWaitingForTheLockBeforeCheckingOverlaps() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L), Optional.empty());
        when(productRepository.lockById(1L)).thenReturn(true);

        assertThatThrownBy(() -> priceService.updatePrice(priceToUpdate()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Price 10 does not exist");
        verify(priceRepository, never()).findFirstOverlapping(any(), any(), any(), any());
        verify(priceRepository, never()).update(any());
    }

    @Test
    void deleteRejectsAPriceDeletedWhileWaitingForTheLockBeforeDeleting() {
        when(priceRepository.findProductIdByPriceId(10L)).thenReturn(Optional.of(1L), Optional.empty());
        when(productRepository.lockById(1L)).thenReturn(true);

        assertThatThrownBy(() -> priceService.deletePrice(10L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Price 10 does not exist");
        verify(priceRepository, never()).deleteById(any());
    }
}

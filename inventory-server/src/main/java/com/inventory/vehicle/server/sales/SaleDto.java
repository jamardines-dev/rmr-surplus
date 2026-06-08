package com.inventory.vehicle.server.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SaleDto(
        Long id,
        String sellerName,
        LocalDate soldDate,
        LocalDateTime createdAt,
        BigDecimal totalAmount,
        List<SaleItemDto> items
) {
    static SaleDto from(Sale sale) {
        return new SaleDto(
                sale.getId(),
                sale.getSellerName(),
                sale.getSoldDate(),
                sale.getCreatedAt(),
                sale.getTotalAmount(),
                sale.getItems().stream().map(SaleItemDto::from).toList()
        );
    }
}

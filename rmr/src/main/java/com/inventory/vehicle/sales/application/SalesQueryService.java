package com.inventory.vehicle.sales.application;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.sales.domain.Sale;
import com.inventory.vehicle.sales.domain.SaleItem;
import com.inventory.vehicle.sales.infrastructure.SaleItemRepository;
import com.inventory.vehicle.sales.infrastructure.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesQueryService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SessionService sessionService;

    public SalesQueryService(SaleRepository saleRepository, SaleItemRepository saleItemRepository, SessionService sessionService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<Sale> findSalesByDate(LocalDate soldDate) {
        requireAdmin();
        return saleRepository.findBySoldDateOrderByCreatedAtDesc(soldDate);
    }

    @Transactional(readOnly = true)
    public List<Sale> findSalesBetween(LocalDate startDate, LocalDate endDate) {
        requireAdmin();
        return saleRepository.findBySoldDateBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Sale> findAllSales() {
        requireAdmin();
        return saleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findSaleLinesForSellerByDate(String sellerName, LocalDate soldDate) {
        String allowedSeller = resolveAllowedSeller(sellerName);
        return saleItemRepository.findBySaleSellerNameIgnoreCaseAndSaleSoldDateOrderBySaleCreatedAtDesc(allowedSeller, soldDate)
                .stream()
                .map(this::toLineResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findSaleLinesForSellerBetween(String sellerName, LocalDate startDate, LocalDate endDate) {
        String allowedSeller = resolveAllowedSeller(sellerName);
        return saleItemRepository.findBySaleSellerNameIgnoreCaseAndSaleSoldDateBetweenOrderBySaleCreatedAtDesc(
                        allowedSeller,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toLineResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findAllSaleLinesForSeller(String sellerName) {
        String allowedSeller = resolveAllowedSeller(sellerName);
        return saleItemRepository.findBySaleSellerNameIgnoreCaseOrderBySaleCreatedAtDesc(allowedSeller)
                .stream()
                .map(this::toLineResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findSaleLinesByDate(LocalDate soldDate) {
        requireAdmin();
        return saleItemRepository.findBySaleSoldDateOrderBySaleCreatedAtDesc(soldDate)
                .stream()
                .map(this::toLineResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSalesSummary> summarizeSalesPerEmployee(LocalDate soldDate) {
        requireAdmin();
        Map<String, List<SaleLineResult>> linesByEmployee = findSaleLinesByDate(soldDate)
                .stream()
                .collect(Collectors.groupingBy(SaleLineResult::sellerName));

        return linesByEmployee.entrySet()
                .stream()
                .map(entry -> {
                    List<SaleLineResult> lines = entry.getValue();
                    long transactions = lines.stream()
                            .map(SaleLineResult::saleId)
                            .distinct()
                            .count();
                    int items = lines.stream()
                            .mapToInt(SaleLineResult::quantitySold)
                            .sum();
                    BigDecimal total = lines.stream()
                            .map(SaleLineResult::totalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new EmployeeSalesSummary(entry.getKey(), transactions, items, total);
                })
                .sorted(Comparator.comparing(EmployeeSalesSummary::totalSalesToday).reversed())
                .toList();
    }

    private String resolveAllowedSeller(String sellerName) {
        requireLoggedIn();
        if (sessionService.getCurrentRole() == Role.ADMIN) {
            if (sellerName == null || sellerName.isBlank()) {
                throw new BusinessException("Seller name is required.");
            }
            return sellerName.trim();
        }
        if (sessionService.getCurrentRole() == Role.EMPLOYEE) {
            return sessionService.getCurrentDisplayName();
        }
        throw new BusinessException("You do not have access to sales records.");
    }

    private void requireAdmin() {
        requireLoggedIn();
        if (sessionService.getCurrentRole() != Role.ADMIN) {
            throw new BusinessException("Only admins can view all sales records.");
        }
    }

    private void requireLoggedIn() {
        if (!sessionService.isLoggedIn()) {
            throw new BusinessException("You must be logged in.");
        }
    }

    private SaleLineResult toLineResult(SaleItem saleItem) {
        return new SaleLineResult(
                saleItem.getId(),
                saleItem.getSale().getId(),
                saleItem.getSale().getSellerName(),
                saleItem.getSale().getCreatedAt(),
                saleItem.getProduct().getProductName(),
                saleItem.getProduct().getBrand().getName(),
                saleItem.getProduct().getVehicleType().getName(),
                saleItem.getProduct().getModelCode(),
                saleItem.getProduct().getProductImage(),
                saleItem.getProduct().getProductImageType(),
                saleItem.getQuantitySold(),
                saleItem.getPriceSold(),
                saleItem.getTotalAmount()
        );
    }
}

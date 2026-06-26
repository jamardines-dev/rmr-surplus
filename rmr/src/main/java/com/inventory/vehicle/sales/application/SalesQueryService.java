package com.inventory.vehicle.sales.application;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.inventory.infrastructure.StockMovementRepository;
import com.inventory.vehicle.sales.domain.Sale;
import com.inventory.vehicle.sales.domain.SaleItem;
import com.inventory.vehicle.sales.infrastructure.SaleItemRepository;
import com.inventory.vehicle.sales.infrastructure.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesQueryService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SessionService sessionService;

    public SalesQueryService(SaleRepository saleRepository, SaleItemRepository saleItemRepository,
            StockMovementRepository stockMovementRepository, SessionService sessionService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockMovementRepository = stockMovementRepository;
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
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return saleItemRepository.findBySaleSellerNameIgnoreCaseAndSaleSoldDateOrderBySaleCreatedAtDesc(allowedSeller, soldDate)
                .stream()
                .map(item -> toLineResult(item, lastDrByProductId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findSaleLinesForSellerBetween(String sellerName, LocalDate startDate, LocalDate endDate) {
        String allowedSeller = resolveAllowedSeller(sellerName);
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return saleItemRepository.findBySaleSellerNameIgnoreCaseAndSaleSoldDateBetweenOrderBySaleCreatedAtDesc(
                        allowedSeller,
                        startDate,
                        endDate
                )
                .stream()
                .map(item -> toLineResult(item, lastDrByProductId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findAllSaleLinesForSeller(String sellerName) {
        String allowedSeller = resolveAllowedSeller(sellerName);
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return saleItemRepository.findBySaleSellerNameIgnoreCaseOrderBySaleCreatedAtDesc(allowedSeller)
                .stream()
                .map(item -> toLineResult(item, lastDrByProductId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleLineResult> findSaleLinesByDate(LocalDate soldDate) {
        requireAdmin();
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return saleItemRepository.findBySaleSoldDateOrderBySaleCreatedAtDesc(soldDate)
                .stream()
                .map(item -> toLineResult(item, lastDrByProductId))
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

    private SaleLineResult toLineResult(SaleItem saleItem, Map<Long, String> lastDrByProductId) {
        byte[] firstImage = null;
        String firstImageType = null;
        if (!saleItem.getProduct().getImages().isEmpty()) {
            var img = saleItem.getProduct().getImages().get(0);
            firstImage = img.getImageData();
            firstImageType = img.getImageType();
        }

        String stockNumber = lastDrByProductId.getOrDefault(saleItem.getProduct().getId(), "");

        return new SaleLineResult(
                saleItem.getId(),
                saleItem.getSale().getId(),
                saleItem.getSale().getSellerName(),
                saleItem.getSale().getCreatedAt(),
                saleItem.getProduct().getProductName(),
                saleItem.getProduct().getBrand().getName(),
                saleItem.getProduct().getVehicleType().getName(),
                saleItem.getProduct().getModelCode(),
                stockNumber,
                firstImage,
                firstImageType,
                saleItem.getQuantitySold(),
                saleItem.getOriginalPrice(),
                saleItem.getPriceSold(),
                saleItem.getTotalAmount()
        );
    }

    private Map<Long, String> buildLastDrMap() {
        Map<Long, String> drMap = new HashMap<>();
        stockMovementRepository.findRestocksWithDrNumbers()
                .forEach(movement -> drMap.putIfAbsent(movement.getProduct().getId(), movement.getReferenceId()));
        return drMap;
    }
}

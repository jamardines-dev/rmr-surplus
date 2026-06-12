package com.inventory.vehicle.sales.application;

import com.inventory.vehicle.audit.application.AuditService;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.inventory.domain.StockMovement;
import com.inventory.vehicle.inventory.domain.StockMovementType;
import com.inventory.vehicle.inventory.infrastructure.StockMovementRepository;
import com.inventory.vehicle.product.domain.Product;
import com.inventory.vehicle.product.infrastructure.ProductRepository;
import com.inventory.vehicle.sales.domain.Sale;
import com.inventory.vehicle.sales.domain.SaleItem;
import com.inventory.vehicle.sales.infrastructure.SaleItemRepository;
import com.inventory.vehicle.sales.infrastructure.SaleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordSaleService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuditService auditService;
    private final SessionService sessionService;

    public RecordSaleService(
            ProductRepository productRepository,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            StockMovementRepository stockMovementRepository,
            AuditService auditService,
            SessionService sessionService
    ) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.auditService = auditService;
        this.sessionService = sessionService;
    }

    @Transactional
    public Long recordCartSale(RecordCartSaleCommand command) {
        requireEmployee();
        validate(command);

        BigDecimal saleTotal = command.items()
                .stream()
                .map(item -> item.priceSold().multiply(BigDecimal.valueOf(item.quantitySold())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale sale = new Sale();
        sale.setSellerName(sessionService.getCurrentDisplayName());
        sale.setSoldDate(command.soldDate());
        sale.setTotalAmount(saleTotal);
        sale.setEncodedBy(sessionService.getCurrentUsername());
        sale.setReceiptType(normalizeReceiptType(command.receiptType()));
        Sale savedSale = saleRepository.save(sale);

        for (CartSaleItemCommand item : command.items()) {
            Product product = productRepository.findByIdAndActiveTrue(item.productId())
                    .orElseThrow(() -> new BusinessException("Select an active product."));
            int previousStock = product.getCurrentStock();
            int newStock = previousStock - item.quantitySold();

            if (newStock < 0) {
                throw new BusinessException("Insufficient stock for " + product.getProductName()
                        + ". Available stock: " + previousStock + ".");
            }

            BigDecimal itemTotal = item.priceSold().multiply(BigDecimal.valueOf(item.quantitySold()));
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(savedSale);
            saleItem.setProduct(product);
            saleItem.setQuantitySold(item.quantitySold());
            saleItem.setPriceSold(item.priceSold());
            saleItem.setTotalAmount(itemTotal);
            saleItemRepository.save(saleItem);

            product.setCurrentStock(newStock);
            productRepository.save(product);
            stockMovementRepository.save(createSaleMovement(product, item.quantitySold(), previousStock, newStock, savedSale.getId()));
        }

        auditService.record("RECORD_CART_SALE", "Recorded cart sale " + savedSale.getId(), sessionService.getCurrentUsername());
        return savedSale.getId();
    }

    @Transactional
    void undoSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Sale was not found."));
        List<SaleItem> saleItems = saleItemRepository.findBySaleId(saleId);

        if (saleItems.isEmpty()) {
            throw new BusinessException("Sale has no items to undo.");
        }

        for (SaleItem saleItem : saleItems) {
            Product product = saleItem.getProduct();
            int previousStock = product.getCurrentStock();
            int newStock = previousStock + saleItem.getQuantitySold();

            product.setCurrentStock(newStock);
            productRepository.save(product);
            stockMovementRepository.save(createUndoMovement(product, saleItem.getQuantitySold(), previousStock, newStock, saleId));
        }

        saleItemRepository.deleteAll(saleItems);
        saleRepository.delete(sale);
        auditService.record("UNDO_SALE", "Undid sale " + saleId, sessionService.getCurrentUsername());
    }

    @Transactional
    public Long undoLatestSaleForSeller(String sellerName) {
        requireEmployee();
        String currentSeller = sessionService.getCurrentDisplayName();

        Sale sale = saleRepository.findFirstBySellerNameIgnoreCaseOrderByCreatedAtDesc(currentSeller)
                .orElseThrow(() -> new BusinessException("No sale found for your account."));
        Long saleId = sale.getId();
        undoSale(saleId);
        return saleId;
    }

    @Transactional
    public Long undoSaleItemForSeller(Long saleItemId, String sellerName) {
        requireEmployee();
        if (saleItemId == null) {
            throw new BusinessException("Select a sold product to undo.");
        }

        SaleItem saleItem = saleItemRepository.findById(saleItemId)
                .orElseThrow(() -> new BusinessException("Sold product was not found."));
        Sale sale = saleItem.getSale();
        if (!sale.getSellerName().equalsIgnoreCase(sessionService.getCurrentDisplayName())) {
            throw new BusinessException("You can only undo your own sold products.");
        }

        Product product = saleItem.getProduct();
        int previousStock = product.getCurrentStock();
        int newStock = previousStock + saleItem.getQuantitySold();
        product.setCurrentStock(newStock);
        productRepository.save(product);
        stockMovementRepository.save(createUndoMovement(product, saleItem.getQuantitySold(), previousStock, newStock, sale.getId()));

        List<SaleItem> saleItems = saleItemRepository.findBySaleId(sale.getId());
        if (saleItems.size() <= 1) {
            saleItemRepository.delete(saleItem);
            saleRepository.delete(sale);
        } else {
            sale.setTotalAmount(sale.getTotalAmount().subtract(saleItem.getTotalAmount()));
            saleRepository.save(sale);
            saleItemRepository.delete(saleItem);
        }

        auditService.record("UNDO_SALE_ITEM", "Undid sold product " + saleItemId + " from sale " + sale.getId(), sessionService.getCurrentUsername());
        return sale.getId();
    }

    private void validate(RecordCartSaleCommand command) {
        if (command.soldDate() == null) {
            throw new BusinessException("Sold date is required.");
        }
        normalizeReceiptType(command.receiptType());
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessException("Add at least one product to the cart.");
        }
        for (CartSaleItemCommand item : command.items()) {
            if (item.quantitySold() <= 0) {
                throw new BusinessException("Quantity must be greater than zero.");
            }
            if (item.priceSold() == null || item.priceSold().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Price must be greater than zero.");
            }
        }
    }

    private void requireEmployee() {
        if (!sessionService.isLoggedIn()) {
            throw new BusinessException("You must be logged in.");
        }
        if (sessionService.getCurrentRole() != Role.EMPLOYEE) {
            throw new BusinessException("Only employees can record or undo sales.");
        }
    }

    private String normalizeReceiptType(String receiptType) {
        if ("Official Receipt".equalsIgnoreCase(receiptType) || "OFFICIAL_RECEIPT".equalsIgnoreCase(receiptType)) {
            return "OFFICIAL_RECEIPT";
        }
        if ("Delivery Receipt".equalsIgnoreCase(receiptType) || "DELIVERY_RECEIPT".equalsIgnoreCase(receiptType)) {
            return "DELIVERY_RECEIPT";
        }
        throw new BusinessException("Select Delivery Receipt or Official Receipt.");
    }

    private StockMovement createSaleMovement(Product product, int quantity, int previousStock, int newStock, Long saleId) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.SALE);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setReason("Sale recorded");
        movement.setReferenceId(String.valueOf(saleId));
        movement.setCreatedBy(sessionService.getCurrentUsername());
        return movement;
    }

    private StockMovement createUndoMovement(Product product, int quantity, int previousStock, int newStock, Long saleId) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.RETURNED);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setReason("Sale undone");
        movement.setReferenceId(String.valueOf(saleId));
        movement.setCreatedBy(sessionService.getCurrentUsername());
        return movement;
    }
}

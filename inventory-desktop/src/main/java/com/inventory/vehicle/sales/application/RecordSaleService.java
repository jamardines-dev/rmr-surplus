package com.inventory.vehicle.sales.application;

import com.inventory.vehicle.audit.application.AuditService;
import com.inventory.vehicle.auth.application.SessionService;
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
    public Long recordSale(RecordSaleCommand command) {
        validate(command);

        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new BusinessException("Product was not found."));
        int previousStock = product.getCurrentStock();
        int newStock = previousStock - command.quantitySold();

        if (newStock < 0) {
            throw new BusinessException("Product stock must never become negative.");
        }

        BigDecimal itemTotal = command.priceSold().multiply(BigDecimal.valueOf(command.quantitySold()));
        Sale sale = new Sale();
        sale.setSellerName(command.sellerName().trim());
        sale.setSoldDate(command.soldDate());
        sale.setTotalAmount(itemTotal);
        sale.setEncodedBy(sessionService.getCurrentUsername());
        Sale savedSale = saleRepository.save(sale);

        SaleItem saleItem = new SaleItem();
        saleItem.setSale(savedSale);
        saleItem.setProduct(product);
        saleItem.setQuantitySold(command.quantitySold());
        saleItem.setPriceSold(command.priceSold());
        saleItem.setTotalAmount(itemTotal);
        saleItemRepository.save(saleItem);

        product.setCurrentStock(newStock);
        productRepository.save(product);
        stockMovementRepository.save(createSaleMovement(product, command.quantitySold(), previousStock, newStock, savedSale.getId()));
        auditService.record("RECORD_SALE", "Recorded sale " + savedSale.getId() + " for " + product.getProductName(), sessionService.getCurrentUsername());
        return savedSale.getId();
    }

    @Transactional
    public Long recordCartSale(RecordCartSaleCommand command) {
        validate(command);

        BigDecimal saleTotal = command.items()
                .stream()
                .map(item -> item.priceSold().multiply(BigDecimal.valueOf(item.quantitySold())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale sale = new Sale();
        sale.setSellerName(command.sellerName().trim());
        sale.setSoldDate(command.soldDate());
        sale.setTotalAmount(saleTotal);
        sale.setEncodedBy(sessionService.getCurrentUsername());
        Sale savedSale = saleRepository.save(sale);

        for (CartSaleItemCommand item : command.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new BusinessException("Product was not found."));
            int previousStock = product.getCurrentStock();
            int newStock = previousStock - item.quantitySold();

            if (newStock < 0) {
                throw new BusinessException("Product stock must never become negative.");
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
    public void undoSale(Long saleId) {
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
        if (sellerName == null || sellerName.isBlank()) {
            throw new BusinessException("Seller name is required to undo a sale.");
        }

        Sale sale = saleRepository.findFirstBySellerNameIgnoreCaseOrderByCreatedAtDesc(sellerName.trim())
                .orElseThrow(() -> new BusinessException("No sale found for this seller."));
        Long saleId = sale.getId();
        undoSale(saleId);
        return saleId;
    }

    private void validate(RecordSaleCommand command) {
        if (command.sellerName() == null || command.sellerName().isBlank()) {
            throw new BusinessException("Seller name is required.");
        }
        if (command.soldDate() == null) {
            throw new BusinessException("Sold date is required.");
        }
        if (command.quantitySold() <= 0) {
            throw new BusinessException("Quantity must be greater than zero.");
        }
        if (command.priceSold() == null || command.priceSold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Price must not be negative.");
        }
    }

    private void validate(RecordCartSaleCommand command) {
        if (command.sellerName() == null || command.sellerName().isBlank()) {
            throw new BusinessException("Seller name is required.");
        }
        if (command.soldDate() == null) {
            throw new BusinessException("Sold date is required.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessException("Add at least one product to the cart.");
        }
        for (CartSaleItemCommand item : command.items()) {
            if (item.quantitySold() <= 0) {
                throw new BusinessException("Quantity must be greater than zero.");
            }
            if (item.priceSold() == null || item.priceSold().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Price must not be negative.");
            }
        }
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

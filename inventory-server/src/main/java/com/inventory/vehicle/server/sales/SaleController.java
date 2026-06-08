package com.inventory.vehicle.server.sales;

import com.inventory.vehicle.server.audit.AuditService;
import com.inventory.vehicle.server.auth.Role;
import com.inventory.vehicle.server.inventory.StockMovement;
import com.inventory.vehicle.server.inventory.StockMovementRepository;
import com.inventory.vehicle.server.inventory.StockMovementType;
import com.inventory.vehicle.server.product.Product;
import com.inventory.vehicle.server.product.ProductRepository;
import com.inventory.vehicle.server.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public SaleController(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @PostMapping("/cart")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SaleDto recordCartSale(@Valid @RequestBody RecordCartSaleRequest request) {
        var employee = currentUser.requireRole(Role.EMPLOYEE);
        Sale sale = new Sale(employee.getUsername(), employee.getUsername());

        for (CartSaleItemRequest itemRequest : request.items()) {
            Product product = productRepository.findByIdForSale(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is inactive.");
            }
            int previousStock = product.getCurrentStock();
            product.decreaseStock(itemRequest.quantity());
            SaleItem item = new SaleItem(product, itemRequest.quantity(), itemRequest.priceSold());
            sale.addItem(item);
            stockMovementRepository.save(new StockMovement(
                    product,
                    StockMovementType.SALE,
                    itemRequest.quantity(),
                    previousStock,
                    product.getCurrentStock(),
                    "Sold by " + employee.getUsername(),
                    "SALE_PENDING",
                    employee.getUsername()
            ));
        }

        Sale saved = saleRepository.save(sale);
        auditService.record("SALE_RECORDED", employee.getUsername() + " recorded sale #" + saved.getId(), employee.getUsername());
        return SaleDto.from(saved);
    }

    @GetMapping("/my-today")
    public List<SaleDto> myToday() {
        var employee = currentUser.requireRole(Role.EMPLOYEE);
        return saleRepository.findBySoldDateAndSellerNameWithItems(LocalDate.now(), employee.getUsername()).stream()
                .map(SaleDto::from)
                .toList();
    }

    @GetMapping("/today")
    public List<SaleDto> today() {
        currentUser.requireRole(Role.ADMIN);
        return saleRepository.findBySoldDateWithItems(LocalDate.now()).stream()
                .map(SaleDto::from)
                .toList();
    }
}

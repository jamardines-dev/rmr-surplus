package com.inventory.vehicle.product.application;

import com.inventory.vehicle.audit.application.AuditService;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.inventory.domain.StockMovement;
import com.inventory.vehicle.inventory.domain.StockMovementType;
import com.inventory.vehicle.inventory.infrastructure.StockMovementRepository;
import com.inventory.vehicle.product.domain.Brand;
import com.inventory.vehicle.product.domain.Product;
import com.inventory.vehicle.product.domain.VehicleType;
import com.inventory.vehicle.product.infrastructure.BrandRepository;
import com.inventory.vehicle.product.infrastructure.ProductRepository;
import com.inventory.vehicle.product.infrastructure.VehicleTypeRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuditService auditService;
    private final SessionService sessionService;

    public ProductService(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            VehicleTypeRepository vehicleTypeRepository,
            StockMovementRepository stockMovementRepository,
            AuditService auditService,
            SessionService sessionService
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.auditService = auditService;
        this.sessionService = sessionService;
    }

    @Transactional
    public ProductResult createProduct(CreateProductCommand command) {
        requireAdmin();
        validate(command);

        Brand brand = findOrCreateBrand(command.brandName());
        VehicleType vehicleType = findOrCreateVehicleType(command.vehicleTypeName());

        Product product = new Product();
        product.setProductName(command.productName().trim());
        product.setBrand(brand);
        product.setVehicleType(vehicleType);
        product.setCurrentStock(command.currentStock());
        product.setUnitPrice(command.unitPrice());

        Product savedProduct = productRepository.save(product);
        auditService.record("CREATE_PRODUCT", "Created product " + savedProduct.getProductName(), sessionService.getCurrentUsername());
        return ProductResultMapper.toResult(savedProduct);
    }

    @Transactional
    public ProductResult updateProduct(UpdateProductCommand command) {
        requireAdmin();
        validate(command);

        Product product = productRepository.findByIdAndActiveTrue(command.productId())
                .orElseThrow(() -> new BusinessException("Product was not found."));

        product.setProductName(command.productName().trim());
        product.setBrand(findOrCreateBrand(command.brandName()));
        product.setVehicleType(findOrCreateVehicleType(command.vehicleTypeName()));
        product.setCurrentStock(command.currentStock());
        product.setUnitPrice(command.unitPrice());

        Product savedProduct = productRepository.save(product);
        auditService.record("UPDATE_PRODUCT", "Updated product " + savedProduct.getProductName(), sessionService.getCurrentUsername());
        return ProductResultMapper.toResult(savedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        requireAdmin();

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new BusinessException("Product was not found."));
        product.setActive(false);
        productRepository.save(product);
        auditService.record("DELETE_PRODUCT", "Deleted product " + product.getProductName(), sessionService.getCurrentUsername());
    }

    @Transactional
    public ProductResult restockProduct(Long productId, int quantity) {
        requireAdmin();
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be greater than zero.");
        }

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new BusinessException("Product was not found."));
        int previousStock = product.getCurrentStock();
        int newStock = previousStock + quantity;

        product.setCurrentStock(newStock);
        Product savedProduct = productRepository.save(product);
        stockMovementRepository.save(createRestockMovement(savedProduct, quantity, previousStock, newStock));
        auditService.record("RESTOCK_PRODUCT", "Restocked " + quantity + " item(s) for " + savedProduct.getProductName(), sessionService.getCurrentUsername());
        return ProductResultMapper.toResult(savedProduct);
    }

    private void validate(CreateProductCommand command) {
        if (command.productName() == null || command.productName().isBlank()) {
            throw new BusinessException("Product name is required.");
        }
        if (command.brandName() == null || command.brandName().isBlank()) {
            throw new BusinessException("Brand is required.");
        }
        if (command.vehicleTypeName() == null || command.vehicleTypeName().isBlank()) {
            throw new BusinessException("Vehicle type is required.");
        }
        if (command.currentStock() < 0) {
            throw new BusinessException("Product stock must never become negative.");
        }
        if (command.unitPrice() == null || command.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Price must not be negative.");
        }
    }

    private void validate(UpdateProductCommand command) {
        validate(new CreateProductCommand(
                command.productName(),
                command.brandName(),
                command.vehicleTypeName(),
                command.currentStock(),
                command.unitPrice()
        ));
    }

    private Brand findOrCreateBrand(String name) {
        return brandRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> {
                    Brand brand = new Brand();
                    brand.setName(name.trim());
                    return brandRepository.save(brand);
                });
    }

    private VehicleType findOrCreateVehicleType(String name) {
        return vehicleTypeRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> {
                    VehicleType vehicleType = new VehicleType();
                    vehicleType.setName(name.trim());
                    return vehicleTypeRepository.save(vehicleType);
                });
    }

    private void requireAdmin() {
        if (sessionService.getCurrentRole() != Role.ADMIN) {
            throw new BusinessException("Only admins can manage products.");
        }
    }

    private StockMovement createRestockMovement(Product product, int quantity, int previousStock, int newStock) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.RESTOCK);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setReason("Product restocked");
        movement.setCreatedBy(sessionService.getCurrentUsername());
        return movement;
    }
}

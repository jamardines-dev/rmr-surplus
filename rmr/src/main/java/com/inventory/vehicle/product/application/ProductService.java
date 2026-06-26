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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        product.setModelCode(command.modelCode().trim());
        product.setCurrentStock(command.currentStock());
        product.setUnitPrice(command.unitPrice());
        if (command.images() != null) {
            for (NewProductImage img : command.images()) {
                product.addImage(img.data(), img.type());
            }
        }
        product.setLastRestockedDate(command.lastRestockedDate());

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
        product.setModelCode(command.modelCode().trim());
        product.setCurrentStock(command.currentStock());
        product.setUnitPrice(command.unitPrice());

        if (command.removedImageIds() != null) {
            for (Long imageId : command.removedImageIds()) {
                product.removeImage(imageId);
            }
        }
        if (command.addedImages() != null) {
            for (NewProductImage img : command.addedImages()) {
                product.addImage(img.data(), img.type());
            }
        }

        product.setLastRestockedDate(command.lastRestockedDate());

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
    public void restockProducts(RestockProductsCommand command) {
        requireAdmin();

        List<RestockProductItemCommand> items = command.items() == null ? List.of() : command.items().stream()
                .filter(item -> item.productId() != null && item.quantity() > 0)
                .toList();
        if (items.isEmpty()) {
            throw new BusinessException("Add at least one product quantity to restock.");
        }

        for (RestockProductItemCommand item : items) {
            String drNumber = trimToNull(item.drNumber());
            if (drNumber == null) {
                throw new BusinessException("DR number is required for all items.");
            }
        }

        LocalDate restockedDate = command.restockedDate() == null ? LocalDate.now() : command.restockedDate();
        String username = sessionService.getCurrentUsername();
        for (RestockProductItemCommand item : items) {
            Product product = productRepository.findByIdAndActiveTrue(item.productId())
                    .orElseThrow(() -> new BusinessException("Product was not found."));
            int previousStock = product.getCurrentStock();
            int newStock = previousStock + item.quantity();
            product.setCurrentStock(newStock);
            product.setLastRestockedDate(restockedDate);
            productRepository.save(product);

            String drNumber = trimToNull(item.drNumber());
            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setMovementType(StockMovementType.RESTOCK);
            movement.setQuantity(item.quantity());
            movement.setPreviousStock(previousStock);
            movement.setNewStock(newStock);
            movement.setReason("Restock from DR " + drNumber);
            movement.setReferenceId(drNumber);
            movement.setCreatedBy(username);
            stockMovementRepository.save(movement);
        }

        auditService.record(
                "RESTOCK_PRODUCTS",
                "Restocked " + items.size() + " product(s)",
                username);
    }

    @Transactional
    public void restockNewProducts(RestockNewProductsCommand command) {
        requireAdmin();

        List<CreateProductCommand> products = command.products() == null ? List.of() : command.products();
        if (products.isEmpty()) {
            throw new BusinessException("Add at least one new product to restock.");
        }

        for (CreateProductCommand productCommand : products) {
            String drNumber = trimToNull(productCommand.drNumber());
            if (drNumber == null) {
                throw new BusinessException("DR number is required for all products.");
            }
        }

        Set<String> modelCodes = new HashSet<>();
        for (CreateProductCommand productCommand : products) {
            validate(productCommand);
            String normalizedModelCode = productCommand.modelCode().trim().toLowerCase();
            if (!modelCodes.add(normalizedModelCode)) {
                throw new BusinessException("Model " + productCommand.modelCode().trim() + " is duplicated in this restock.");
            }
        }

        LocalDate restockedDate = command.restockedDate() == null ? LocalDate.now() : command.restockedDate();
        String username = sessionService.getCurrentUsername();
        for (CreateProductCommand productCommand : products) {
            Brand brand = findOrCreateBrand(productCommand.brandName());
            VehicleType vehicleType = findOrCreateVehicleType(productCommand.vehicleTypeName());

            Product product = new Product();
            product.setProductName(productCommand.productName().trim());
            product.setBrand(brand);
            product.setVehicleType(vehicleType);
            product.setModelCode(productCommand.modelCode().trim());
            product.setCurrentStock(productCommand.currentStock());
            product.setUnitPrice(productCommand.unitPrice());
            if (productCommand.images() != null) {
                for (NewProductImage img : productCommand.images()) {
                    product.addImage(img.data(), img.type());
                }
            }
            product.setLastRestockedDate(restockedDate);

            Product savedProduct = productRepository.save(product);

            String drNumber = trimToNull(productCommand.drNumber());
            StockMovement movement = new StockMovement();
            movement.setProduct(savedProduct);
            movement.setMovementType(StockMovementType.RESTOCK);
            movement.setQuantity(productCommand.currentStock());
            movement.setPreviousStock(0);
            movement.setNewStock(productCommand.currentStock());
            movement.setReason("New product restock from DR " + drNumber);
            movement.setReferenceId(drNumber);
            movement.setCreatedBy(username);
            stockMovementRepository.save(movement);
        }

        auditService.record(
                "RESTOCK_NEW_PRODUCTS",
                "Created and restocked " + products.size() + " new product(s)",
                username);
    }

    @Transactional
    public void updateDrRestockLine(UpdateDrRestockLineCommand command) {
        requireAdmin();
        validate(command);

        StockMovement movement = stockMovementRepository.findByIdWithProduct(command.movementId())
                .orElseThrow(() -> new BusinessException("DR restock line was not found."));
        if (movement.getMovementType() != StockMovementType.RESTOCK || trimToNull(movement.getReferenceId()) == null) {
            throw new BusinessException("Only DR restock lines can be edited here.");
        }

        Product product = movement.getProduct();
        int quantityDelta = command.quantity() - movement.getQuantity();
        int updatedStock = product.getCurrentStock() + quantityDelta;
        if (updatedStock < 0) {
            throw new BusinessException("Product stock must never become negative.");
        }

        product.setProductName(command.productName().trim());
        product.setBrand(findOrCreateBrand(command.brandName()));
        product.setVehicleType(findOrCreateVehicleType(command.vehicleTypeName()));
        product.setModelCode(command.modelCode().trim());
        product.setUnitPrice(command.unitPrice());
        product.setCurrentStock(updatedStock);
        productRepository.save(product);

        movement.setQuantity(command.quantity());
        movement.setNewStock(movement.getPreviousStock() + command.quantity());
        movement.setReason("Edited restock from DR " + movement.getReferenceId());
        stockMovementRepository.save(movement);

        auditService.record(
                "UPDATE_DR_RESTOCK_LINE",
                "Updated product " + product.getProductName() + " in DR " + movement.getReferenceId(),
                sessionService.getCurrentUsername());
    }

    @Transactional
    public void deleteDrRestockLine(Long movementId) {
        requireAdmin();

        StockMovement movement = stockMovementRepository.findByIdWithProduct(movementId)
                .orElseThrow(() -> new BusinessException("DR restock line was not found."));
        if (movement.getMovementType() != StockMovementType.RESTOCK || trimToNull(movement.getReferenceId()) == null) {
            throw new BusinessException("Only DR restock lines can be deleted here.");
        }

        Product product = movement.getProduct();
        product.setActive(false);
        productRepository.save(product);
        stockMovementRepository.delete(movement);

        auditService.record(
                "DELETE_DR_RESTOCK_LINE",
                "Deleted product " + product.getProductName() + " from DR " + movement.getReferenceId(),
                sessionService.getCurrentUsername());
    }

    private void validate(CreateProductCommand command) {
        if (command.productName() == null || command.productName().isBlank()) {
            throw new BusinessException("Product name is required.");
        }
        if (command.brandName() == null || command.brandName().isBlank()) {
            throw new BusinessException("Brand is required.");
        }
        if (command.vehicleTypeName() == null || command.vehicleTypeName().isBlank()) {
            throw new BusinessException("Vehicle is required.");
        }
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException("Model is required.");
        }
        if (productRepository.existsByModelCodeIgnoreCase(command.modelCode().trim())) {
            throw new BusinessException("Model already exists.");
        }
        if (command.currentStock() < 0) {
            throw new BusinessException("Product stock must never become negative.");
        }
        if (command.unitPrice() == null || command.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Price must not be negative.");
        }
    }

    private void validate(UpdateDrRestockLineCommand command) {
        if (command.movementId() == null) {
            throw new BusinessException("DR restock line is required.");
        }
        if (command.productName() == null || command.productName().isBlank()) {
            throw new BusinessException("Product name is required.");
        }
        if (command.brandName() == null || command.brandName().isBlank()) {
            throw new BusinessException("Brand is required.");
        }
        if (command.vehicleTypeName() == null || command.vehicleTypeName().isBlank()) {
            throw new BusinessException("Vehicle is required.");
        }
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException("Model is required.");
        }
        StockMovement movement = stockMovementRepository.findByIdWithProduct(command.movementId())
                .orElseThrow(() -> new BusinessException("DR restock line was not found."));
        if (productRepository.existsByModelCodeIgnoreCaseAndIdNot(command.modelCode().trim(), movement.getProduct().getId())) {
            throw new BusinessException("Model already exists.");
        }
        if (command.quantity() <= 0) {
            throw new BusinessException("Restock quantity must be greater than 0.");
        }
        if (command.unitPrice() == null || command.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Price must not be negative.");
        }
    }

    private void validate(UpdateProductCommand command) {
        if (command.productName() == null || command.productName().isBlank()) {
            throw new BusinessException("Product name is required.");
        }
        if (command.brandName() == null || command.brandName().isBlank()) {
            throw new BusinessException("Brand is required.");
        }
        if (command.vehicleTypeName() == null || command.vehicleTypeName().isBlank()) {
            throw new BusinessException("Vehicle is required.");
        }
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException("Model is required.");
        }
        if (productRepository.existsByModelCodeIgnoreCaseAndIdNot(command.modelCode().trim(), command.productId())) {
            throw new BusinessException("Model already exists.");
        }
        if (command.currentStock() < 0) {
            throw new BusinessException("Product stock must never become negative.");
        }
        if (command.unitPrice() == null || command.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Price must not be negative.");
        }
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}

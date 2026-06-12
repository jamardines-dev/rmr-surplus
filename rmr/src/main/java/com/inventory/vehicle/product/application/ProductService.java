package com.inventory.vehicle.product.application;

import com.inventory.vehicle.audit.application.AuditService;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
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
    private final AuditService auditService;
    private final SessionService sessionService;

    public ProductService(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            VehicleTypeRepository vehicleTypeRepository,
            AuditService auditService,
            SessionService sessionService
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
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
        product.setProductImage(command.productImage());
        product.setProductImageType(trimToNull(command.productImageType()));
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
        product.setProductImage(command.productImage());
        product.setProductImageType(trimToNull(command.productImageType()));
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
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException("Model code is required.");
        }
        if (productRepository.existsByModelCodeIgnoreCase(command.modelCode().trim())) {
            throw new BusinessException("Model code already exists.");
        }
        if (command.currentStock() < 0) {
            throw new BusinessException("Product stock must never become negative.");
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
            throw new BusinessException("Vehicle type is required.");
        }
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException("Model code is required.");
        }
        if (productRepository.existsByModelCodeIgnoreCaseAndIdNot(command.modelCode().trim(), command.productId())) {
            throw new BusinessException("Model code already exists.");
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

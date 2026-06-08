package com.inventory.vehicle.server.product;

import com.inventory.vehicle.server.audit.AuditService;
import com.inventory.vehicle.server.auth.Role;
import com.inventory.vehicle.server.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public ProductController(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            VehicleTypeRepository vehicleTypeRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    public List<ProductDto> products() {
        currentUser.requireUser();
        return productRepository.findAllWithDetails().stream()
                .map(ProductDto::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        var user = currentUser.requireRole(Role.ADMIN);
        Brand brand = brandRepository.findByNameIgnoreCase(request.brand())
                .orElseGet(() -> brandRepository.save(new Brand(request.brand().trim())));
        VehicleType vehicleType = vehicleTypeRepository.findByNameIgnoreCase(request.vehicleType())
                .orElseGet(() -> vehicleTypeRepository.save(new VehicleType(request.vehicleType().trim())));
        Product product = new Product(
                request.productName().trim(),
                brand,
                vehicleType,
                request.modelCode().trim(),
                request.currentStock(),
                request.unitPrice()
        );
        Product saved = productRepository.save(product);
        auditService.record("PRODUCT_CREATED", "Created product " + saved.getModelCode(), user.getUsername());
        return ProductDto.from(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        var user = currentUser.requireRole(Role.ADMIN);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        Brand brand = brandRepository.findByNameIgnoreCase(request.brand())
                .orElseGet(() -> brandRepository.save(new Brand(request.brand().trim())));
        VehicleType vehicleType = vehicleTypeRepository.findByNameIgnoreCase(request.vehicleType())
                .orElseGet(() -> vehicleTypeRepository.save(new VehicleType(request.vehicleType().trim())));
        product.updateDetails(
                request.productName().trim(),
                brand,
                vehicleType,
                request.modelCode().trim(),
                request.unitPrice(),
                request.active()
        );
        auditService.record("PRODUCT_UPDATED", "Updated product " + product.getModelCode(), user.getUsername());
        return ProductDto.from(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deactivate(@PathVariable Long id) {
        var user = currentUser.requireRole(Role.ADMIN);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        product.updateDetails(product.getProductName(), product.getBrand(), product.getVehicleType(), product.getModelCode(), product.getUnitPrice(), false);
        auditService.record("PRODUCT_DEACTIVATED", "Deactivated product " + product.getModelCode(), user.getUsername());
    }
}

package com.inventory.vehicle.report.application;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ProductQueryService productQueryService;
    private final SessionService sessionService;

    public ReportService(ProductQueryService productQueryService, SessionService sessionService) {
        this.productQueryService = productQueryService;
        this.sessionService = sessionService;
    }

    public List<ProductResult> activeProducts() {
        requireAdmin();
        return productQueryService.findActiveProducts();
    }

    private void requireAdmin() {
        if (!sessionService.isLoggedIn()) {
            throw new BusinessException("You must be logged in.");
        }
        if (sessionService.getCurrentRole() != Role.ADMIN) {
            throw new BusinessException("Only admins can view reports.");
        }
    }
}

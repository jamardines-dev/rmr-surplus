package com.inventory.vehicle.report.application;

import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ProductQueryService productQueryService;

    public ReportService(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    public List<ProductResult> activeProducts() {
        return productQueryService.findActiveProducts();
    }
}

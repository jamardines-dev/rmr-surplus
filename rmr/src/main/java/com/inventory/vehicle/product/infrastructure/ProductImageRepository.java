package com.inventory.vehicle.product.infrastructure;

import com.inventory.vehicle.product.domain.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query(value = """
            SELECT DISTINCT ON (product_id) id, product_id, image_data, image_type, sort_order, created_at
            FROM product_images
            WHERE product_id IN (:productIds)
            ORDER BY product_id, sort_order, id
            """, nativeQuery = true)
    List<ProductImage> findFirstImagesByProductIds(@Param("productIds") Collection<Long> productIds);
}

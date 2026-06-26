-- Create product_images child table for multi-image support
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_type VARCHAR(80),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- Migrate existing single images from products table to product_images
INSERT INTO product_images (product_id, image_data, image_type, sort_order, created_at)
SELECT id, product_image, product_image_type, 0, CURRENT_TIMESTAMP
FROM products
WHERE product_image IS NOT NULL;

-- Drop old single-image columns from products table
ALTER TABLE products DROP COLUMN product_image;
ALTER TABLE products DROP COLUMN product_image_type;

-- Note: stock_movements.reference_id already exists and already stores per-item DR numbers.
-- No DDL change needed for per-item DR feature; only application/UI layer changes required.

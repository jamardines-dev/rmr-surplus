-- Create product_images child table for multi-image support
CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_type VARCHAR(80),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);

-- Migrate existing single images from products table to product_images
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'products'
          AND column_name = 'product_image'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'products'
              AND column_name = 'product_image_type'
        ) THEN
            EXECUTE '
                INSERT INTO product_images (product_id, image_data, image_type, sort_order, created_at)
                SELECT p.id, p.product_image, p.product_image_type, 0, CURRENT_TIMESTAMP
                FROM products p
                WHERE p.product_image IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM product_images pi
                      WHERE pi.product_id = p.id AND pi.sort_order = 0
                  )';
        ELSE
            EXECUTE '
                INSERT INTO product_images (product_id, image_data, image_type, sort_order, created_at)
                SELECT p.id, p.product_image, NULL, 0, CURRENT_TIMESTAMP
                FROM products p
                WHERE p.product_image IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM product_images pi
                      WHERE pi.product_id = p.id AND pi.sort_order = 0
                  )';
        END IF;
    END IF;
END $$;

-- Drop old single-image columns from products table
ALTER TABLE products DROP COLUMN IF EXISTS product_image;
ALTER TABLE products DROP COLUMN IF EXISTS product_image_type;

-- Note: stock_movements.reference_id already exists and already stores per-item DR numbers.
-- No DDL change needed for per-item DR feature; only application/UI layer changes required.

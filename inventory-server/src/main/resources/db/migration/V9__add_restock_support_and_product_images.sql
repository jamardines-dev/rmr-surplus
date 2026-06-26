-- Add last_restocked_date to products for tracking most recent restock
ALTER TABLE products ADD COLUMN last_restocked_date DATE;

-- Create product_images table for multi-image support
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_type VARCHAR(80),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- Note: stock_movements table already exists from V1 with reference_id column.
-- RESTOCK is a new value in the stock_movement_type VARCHAR column; no DDL change needed.

ALTER TABLE products
    ADD COLUMN product_image BYTEA,
    ADD COLUMN product_image_type VARCHAR(80),
    ADD COLUMN last_restocked_date DATE;

ALTER TABLE sales
    ADD COLUMN receipt_type VARCHAR(30) NOT NULL DEFAULT 'DELIVERY_RECEIPT';

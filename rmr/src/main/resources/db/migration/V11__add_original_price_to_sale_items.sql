ALTER TABLE sale_items ADD COLUMN original_price NUMERIC(12, 2);
UPDATE sale_items SET original_price = price_sold WHERE original_price IS NULL;
ALTER TABLE sale_items ALTER COLUMN original_price SET NOT NULL;

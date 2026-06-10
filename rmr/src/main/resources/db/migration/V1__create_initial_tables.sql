CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE vehicle_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(160) NOT NULL,
    brand_id BIGINT NOT NULL REFERENCES brands(id),
    vehicle_type_id BIGINT NOT NULL REFERENCES vehicle_types(id),
    model_code VARCHAR(100) NOT NULL UNIQUE,
    current_stock INTEGER NOT NULL CHECK (current_stock >= 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    low_stock_threshold INTEGER NOT NULL CHECK (low_stock_threshold >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    seller_name VARCHAR(120) NOT NULL,
    sold_date DATE NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    encoded_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity_sold INTEGER NOT NULL CHECK (quantity_sold > 0),
    price_sold NUMERIC(12, 2) NOT NULL CHECK (price_sold >= 0),
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0)
);

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    movement_type VARCHAR(40) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    previous_stock INTEGER NOT NULL CHECK (previous_stock >= 0),
    new_stock INTEGER NOT NULL CHECK (new_stock >= 0),
    reason VARCHAR(500),
    reference_id VARCHAR(100),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_products_model_code ON products(model_code);
CREATE INDEX idx_sales_sold_date ON sales(sold_date);
CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);

-- Placeholder admin account. Replace this BCrypt hash with a generated production hash before real use.
INSERT INTO users (username, password_hash, role, active, created_at)
VALUES ('admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi7chQOD0SCXU41FvU28GCzVFDXEPXW', 'ADMIN', TRUE, CURRENT_TIMESTAMP);

-- Library table
CREATE TABLE libraries (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    opening_hours VARCHAR(200),
    total_floors INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Copy inventory table
CREATE TABLE copy_inventories (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    book_id VARCHAR(36) NOT NULL,
    library_id VARCHAR(36) NOT NULL,
    library_code VARCHAR(20) NOT NULL,
    total_copies INTEGER DEFAULT 0,
    available_copies INTEGER DEFAULT 0,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uq_inventory_book_library UNIQUE (book_id, library_id)
);

-- Book copies table
CREATE TABLE book_copies (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    inventory_id VARCHAR(36) NOT NULL,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    library_code VARCHAR(10),
    floor INTEGER,
    zone VARCHAR(10),
    aisle VARCHAR(10),
    shelf VARCHAR(10),
    position VARCHAR(10),
    location_code VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    acquisition_date DATE,
    acquisition_method VARCHAR(50),
    acquisition_cost DECIMAL(10, 2),
    condition VARCHAR(20),
    last_borrowed_date DATE,
    borrow_count INTEGER DEFAULT 0,
    damaged_date DATE,
    damage_description VARCHAR(500),
    lost_date DATE,
    lost_reason VARCHAR(200),
    removed_date DATE,
    removal_reason VARCHAR(200),
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_copy_inventory FOREIGN KEY (inventory_id) REFERENCES copy_inventories(id)
);

-- Indexes
CREATE INDEX idx_copy_inventories_book_id ON copy_inventories(book_id);
CREATE INDEX idx_copy_inventories_library_id ON copy_inventories(library_id);
CREATE INDEX idx_copy_inventories_available ON copy_inventories(available_copies);
CREATE INDEX idx_book_copies_inventory_id ON book_copies(inventory_id);
CREATE INDEX idx_book_copies_status ON book_copies(status);
CREATE INDEX idx_book_copies_barcode ON book_copies(barcode);
CREATE INDEX idx_libraries_active ON libraries(is_active);

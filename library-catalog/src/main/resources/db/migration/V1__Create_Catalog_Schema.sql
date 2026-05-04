-- Catalog Context Schema
-- Books table
CREATE TABLE books (
    book_id VARCHAR(36) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    publication_date DATE,
    page_count INTEGER,
    language VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    publisher_id VARCHAR(36),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (book_id)
);

CREATE UNIQUE INDEX idx_books_isbn ON books (isbn);
CREATE INDEX idx_books_status ON books (status);
CREATE INDEX idx_books_title ON books (title);

-- Authors table
CREATE TABLE authors (
    author_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    biography TEXT,
    birth_date DATE,
    death_date DATE,
    nationality VARCHAR(100),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (author_id)
);

CREATE INDEX idx_authors_name ON authors (name);

-- Book-Author association table
CREATE TABLE book_authors (
    book_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    author_name VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (book_id, author_id)
);

-- Publishers table
CREATE TABLE publishers (
    publisher_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    address VARCHAR(500),
    phone VARCHAR(50),
    email VARCHAR(200),
    website VARCHAR(500),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (publisher_id)
);

CREATE INDEX idx_publishers_name ON publishers (name);

-- Categories table
CREATE TABLE categories (
    category_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    level INTEGER NOT NULL DEFAULT 0,
    parent_id VARCHAR(36),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (category_id),
    FOREIGN KEY (parent_id) REFERENCES categories (category_id)
);

CREATE INDEX idx_categories_parent ON categories (parent_id);
CREATE INDEX idx_categories_name ON categories (name);

-- Book-Category association table
CREATE TABLE book_categories (
    book_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (book_id, category_id)
);

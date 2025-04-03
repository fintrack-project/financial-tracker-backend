CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assets (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    asset_name VARCHAR(100) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    purchase_price DECIMAL(18, 4) NOT NULL,
    purchase_date DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    asset_id INT NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    transaction_price DECIMAL(18, 4) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);
-- 创建数据库
CREATE DATABASE IF NOT EXISTS financial_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE financial_management;

-- 创建资产表
CREATE TABLE IF NOT EXISTS asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticker VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    quantity DOUBLE NOT NULL,
    avg_cost DOUBLE NOT NULL,
    purchase_date DATETIME,
    current_price DOUBLE,
    last_updated DATETIME,
    notes TEXT
);

-- 创建资产流水表
CREATE TABLE IF NOT EXISTS asset_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id BIGINT,
    ticker VARCHAR(50) NOT NULL,
    asset_name VARCHAR(255),
    asset_type VARCHAR(50),
    transaction_type VARCHAR(10) NOT NULL,
    quantity DOUBLE NOT NULL,
    price DOUBLE NOT NULL,
    total_amount DOUBLE NOT NULL,
    remaining_quantity DOUBLE,
    transaction_date DATETIME,
    notes TEXT,
    FOREIGN KEY (asset_id) REFERENCES asset(id) ON DELETE SET NULL
);

-- 创建投资组合表
CREATE TABLE IF NOT EXISTS portfolio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME
);

-- 创建投资组合资产关联表
CREATE TABLE IF NOT EXISTS portfolio_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES asset(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_asset_ticker ON asset(ticker);
CREATE INDEX idx_asset_type ON asset(type);
CREATE INDEX idx_asset_transaction_ticker ON asset_transaction(ticker);
CREATE INDEX idx_asset_transaction_date ON asset_transaction(transaction_date);
CREATE INDEX idx_portfolio_asset_portfolio ON portfolio_asset(portfolio_id);
CREATE INDEX idx_portfolio_asset_asset ON portfolio_asset(asset_id);

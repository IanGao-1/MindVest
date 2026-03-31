USE financial_management;

INSERT INTO asset (ticker, name, type, quantity, buy_price, purchase_date, current_price, last_updated, notes) VALUES
('600519.SH', 'Kweichow Moutai', 'STOCK', 20, 1680.00, '2025-01-15 10:00:00', 1768.50, '2026-03-30 09:00:00', 'Leading Chinese consumer staple company'),
('000858.SZ', 'Wuliangye Yibin', 'STOCK', 35, 132.50, '2025-02-10 11:30:00', 141.20, '2026-03-30 09:05:00', 'Chinese liquor sector allocation'),
('601318.SH', 'Ping An Insurance', 'STOCK', 60, 41.80, '2025-03-05 14:20:00', 46.35, '2026-03-30 09:10:00', 'Chinese financial services holding'),
('600036.SH', 'China Merchants Bank', 'STOCK', 80, 33.60, '2025-04-12 09:45:00', 37.10, '2026-03-30 09:15:00', 'Core China banking position'),
('0700.HK', 'Tencent Holdings', 'STOCK', 40, 298.00, '2025-05-20 13:15:00', 325.60, '2026-03-30 09:20:00', 'China internet and gaming exposure'),
('AAPL', 'Apple Inc.', 'STOCK', 50, 168.50, '2025-06-18 10:10:00', 182.30, '2026-03-30 09:25:00', 'Long-term holding in technology sector'),
('MSFT', 'Microsoft Corporation', 'STOCK', 30, 352.10, '2025-07-08 15:00:00', 401.25, '2026-03-30 09:30:00', 'Core portfolio software position'),
('GOOGL', 'Alphabet Inc.', 'STOCK', 20, 142.80, '2025-08-14 20:00:00', 156.40, '2026-03-30 09:35:00', 'Search and AI exposure'),
('AMZN', 'Amazon.com Inc.', 'STOCK', 15, 155.60, '2025-09-03 18:40:00', 174.90, '2026-03-30 09:40:00', 'E-commerce and cloud allocation'),
('TSLA', 'Tesla Inc.', 'STOCK', 12, 210.00, '2025-10-21 10:50:00', 198.75, '2026-03-30 09:45:00', 'Higher volatility growth asset');

INSERT INTO portfolio (name, description, created_at) VALUES
('Growth Portfolio', 'Focused on long-term capital appreciation through growth equities.', '2025-01-01 09:00:00'),
('Income Portfolio', 'Balanced holdings designed to generate steady income.', '2025-01-05 09:30:00'),
('Retirement Portfolio', 'Diversified retirement-oriented investment basket.', '2025-01-10 10:00:00'),
('Tech Leaders Portfolio', 'Concentrated allocation to large-cap technology companies.', '2025-01-15 10:30:00'),
('Defensive Portfolio', 'Lower-risk assets with emphasis on preservation.', '2025-01-20 11:00:00'),
('Crypto Opportunity Portfolio', 'Exposure to selected digital assets.', '2025-01-25 11:30:00'),
('Global Diversified Portfolio', 'Mix of domestic and international instruments.', '2025-02-01 12:00:00'),
('ETF Allocation Portfolio', 'Portfolio built primarily with ETFs.', '2025-02-06 12:30:00'),
('Balanced Portfolio', 'Moderate risk allocation across multiple asset classes.', '2025-02-12 13:00:00'),
('High Conviction Portfolio', 'Selective positions with strong upside expectations.', '2025-02-18 13:30:00');

INSERT INTO portfolio_asset (portfolio_id, asset_id) VALUES
(1, 1),
(1, 2),
(2, 6),
(3, 1),
(4, 2),
(4, 3),
(5, 6),
(6, 8),
(7, 10),
(8, 7);

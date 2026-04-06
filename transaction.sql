DESC transactions;
alter table transactions rename column tansactionDate to transactionDate;

INSERT INTO transaction 
(transactionDate, transactionAmount, transactionType, propertyId, sellerId, seekerId, agentId)
VALUES

-- Property 4
('2024-07-20', 6450000, 'sale', 4, 70, 1, 40),

-- Property 6
('2023-10-31', 6340000, 'rent', 6, 63, 2, 37),

-- Property 7
('2024-02-19', 3960000, 'rent', 7, 74, 3, 44),

-- Property 8
('2024-02-08', 6435000, 'sale', 8, 49, 4, 36),

-- Property 16
('2024-04-22', 4650000, 'sale', 16, 71, 5, 45),

-- Property 17
('2024-02-18', 3550000, 'sale', 17, 60, 6, 43),

-- Property 23
('2023-12-25', 3400000, 'sale', 23, 47, 7, 39),

-- Property 25
('2023-10-31', 4250000, 'rent', 25, 52, 8, 45),

-- Property 26
('2023-11-14', 6400000, 'rent', 26, 66, 9, 36),

-- Property 28
('2024-04-14', 7920000, 'rent', 28, 61, 10, 44),

-- Property 29
('2024-07-01', 9800000, 'sale', 29, 54, 11, 36),

-- Property 35
('2024-06-03', 6430000, 'sale', 35, 65, 12, 38),

-- Property 36
('2023-11-15', 6400000, 'rent', 36, 74, 13, 38),

-- Property 37
('2024-02-18', 9150000, 'rent', 37, 49, 14, 39),

-- Property 44
('2024-03-20', 4450000, 'sale', 44, 50, 15, 45),

-- Property 50
('2024-04-30', 3650000, 'rent', 50, 55, 16, 42),

-- Property 53
('2024-07-26', 4180000, 'sale', 53, 47, 17, 41),

-- Property 57
('2024-08-17', 5450000, 'sale', 57, 51, 18, 42),

-- Property 58
('2024-06-29', 7280000, 'sale', 58, 66, 19, 44),

-- Property 63
('2024-03-07', 7950000, 'rent', 63, 67, 20, 42),

-- Property 65
('2024-08-19', 6730000, 'sale', 65, 73, 21, 42),

-- Property 78
('2023-12-19', 5600000, 'sale', 78, 60, 22, 40),

-- Property 80
('2023-11-24', 2180000, 'sale', 80, 47, 23, 37),

-- Property 81
('2024-03-25', 5200000, 'sale', 81, 72, 24, 40),

-- Property 82
('2024-06-14', 7820000, 'sale', 82, 55, 25, 43),

-- Property 83
('2024-08-19', 3450000, 'sale', 83, 69, 26, 38);


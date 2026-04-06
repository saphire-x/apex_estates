-- (d)
--   SELECT 
--     agentId, 
--     SUM(transactionAmount) AS worth
-- FROM transactions
-- WHERE YEAR(transactionDate) = 2023
-- GROUP BY agentId
-- ORDER BY worth DESC


-- (f)
-- select property_id,price from property where listing_type = 'sale' order by price desc;
-- select property_id,price from property where listing_type = 'rent' order by price desc;




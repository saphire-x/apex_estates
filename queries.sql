-- query (b)
SELECT property_id,house_no,house_name,locality_name,city
from property
where price between 2000000 and 6000000;


-- query (d)
  SELECT 
    agentId, 
    SUM(transactionAmount) AS worth
FROM transactions
WHERE YEAR(transactionDate) = 2023
GROUP BY agentId
ORDER BY worth DESC;



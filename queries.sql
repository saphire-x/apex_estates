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


-- query 3
select property_id,house_no,house_name,locality_name,city
from property
where locality_name = 'AHOM GAON' and listing_type='rent' and price<15000 and bhk>=2;

-- query 5
SELECT p.agentID, AVG(t.transactionAmount) AS avg_sellingPprice,AVG(DATEDIFF(t.transactionDate, p.listed_on)) as avg_days_on_market
from property as p
join transactions as t on p.property_id = t.propertyId
where t.transactionType = 'sale'
and year(t.transactionDate)=2024
group by p.agentID;

-- query 2
SELECT property_id,house_no,house_name,locality_name,city
from property
where price between 2000000 and 6000000;

-- query 3
select property_id,house_no,house_name,locality_name,city
from property
where locality_name = 'AHOM GAON' and listing_type='rent' and price<15000 and bhk>=2;

-- query 5

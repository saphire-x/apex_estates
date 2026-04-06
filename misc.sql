update property set price = price/1000 + 10000 where listing_type = 'rent';
update property set price = price - 5000 where listing_type = 'rent' and price<=14500;

UPDATE users
SET passwordHash = SHA2(passwordHash, 256);
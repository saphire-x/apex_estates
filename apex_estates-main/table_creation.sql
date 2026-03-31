USE apex_estates;
CREATE TABLE users(
	userId INT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(50) NOT NULL,
	email VARCHAR(64) NOT NULL UNIQUE,
	phoneNumber CHAR(10) UNIQUE,
	passwordHash VARCHAR(255) NOT NULL,
	CHECK(name<>''),
	CHECK(email LIKE '%@%.%'),
	CHECK(phoneNumber IS NULL OR (phoneNumber>=1000000000 AND phoneNumber<=9999999999))
);

CREATE TABLE property_seeker(
	userId INT PRIMARY KEY,
	aadharNumber CHAR(12) NOT NULL UNIQUE,
	budgetMin INT NOT NULL,
	budgetMax INT NOT NULL,
	preferredLocality VARCHAR(100),
	propertyTypePreference VARCHAR(40),
	bhkNeed DECIMAL(2,1),
	FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE,
	CHECK(budgetMin>=0),
	CHECK(budgetMax>=budgetMin),
	CHECK(aadharNumber>=100000000000 AND aadharNumber<=999999999999),
	CHECK(bhkNeed IN (1,1.5,2,2.5,3,3.5,4,4.5,5,5.5))
);

CREATE TABLE seller(
	userId INT PRIMARY KEY,
	totalProperties INT DEFAULT 0,
	rating DECIMAL(2,1),
	verificationStatus BOOLEAN NOT NULL DEFAULT FALSE,
	FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE,
	CHECK(totalProperties>=0),
	CHECK(rating>=0 AND rating<=5)
);

CREATE TABLE agent(
	userId INT PRIMARY KEY,
	rating DECIMAL(2,1),
	experienceYears INT NOT NULL,
	salary INT,
	dealCount INT DEFAULT 0,
	rentCount INT DEFAULT 0,
	FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE,
	CHECK(rating>=0 AND rating<=5),
	CHECK(experienceYears>=0),
	CHECK(salary>=0),
	CHECK(dealCount>=0),
	CHECK(rentCount>=0)
);

CREATE TABLE indSeller(
	userId INT PRIMARY KEY,
	aadharNumber CHAR(12) NOT NULL UNIQUE,
	panNumber CHAR(10) NOT NULL UNIQUE,
	FOREIGN KEY(userId) REFERENCES seller(userId) ON DELETE CASCADE,
	CHECK(aadharNumber>=100000000000 AND aadharNumber<=999999999999)
);

CREATE TABLE orgSeller(
	userId INT PRIMARY KEY,
	organizationName VARCHAR(100) NOT NULL,
	panNumber CHAR(10) NOT NULL UNIQUE,
	registrationNumber VARCHAR(50) NOT NULL UNIQUE,
	officeAddress VARCHAR(255) NOT NULL,
	contactNumber CHAR(10) NOT NULL,
	websiteUrl VARCHAR(100),
	FOREIGN KEY(userId) REFERENCES seller(userId) ON DELETE CASCADE,
	CHECK(contactNumber>=1000000000 AND contactNumber<=9999999999)
);

CREATE TABLE property(
	property_id INT PRIMARY KEY AUTO_INCREMENT,
	agentID INT,
	sellerID INT NOT NULL,
	title VARCHAR(100) NOT NULL,
	property_type ENUM('villa','flat','apartment','bungalow','mansion','duplex','triplex','house') NOT NULL,
	listing_type ENUM('rent','sale') NOT NULL,
	price INT NOT NULL CHECK(price>=1000 AND price<=1000000000),
	area_sqft DECIMAL(8,2) NOT NULL,
	bhk DECIMAL(2,1),
	house_no VARCHAR(20) NOT NULL,
	house_name VARCHAR(50),
	locality_name VARCHAR(50) NOT NULL,
	city VARCHAR(50) DEFAULT 'Guwahati',
	pincode CHAR(6) NOT NULL CHECK(pincode REGEXP '^[0-9]{6}$'),
	status ENUM('available','sold','rented') DEFAULT 'available',
	listed_on DATE DEFAULT (CURDATE()),
	updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	year_built INT CHECK(year_built>=1800 AND year_built<=2026),
	FOREIGN KEY(agentID) REFERENCES agent(userId) ON DELETE SET NULL,
	FOREIGN KEY(sellerID) REFERENCES seller(userId) ON DELETE CASCADE
);


CREATE TABLE transactions(
	transactionId INT PRIMARY KEY AUTO_INCREMENT,
	tansactionDate DATE NOT NULL DEFAULT (curdate()),
	transactionAmount INT NOT NULL,
	transactionType VARCHAR(10) NOT NULL,
	propertyId INT NOT NULL,
	sellerId INT NOT NULL,
	seekerId INT NOT NULL,
	agentId INT,
	FOREIGN KEY(propertyId) REFERENCES property(property_id) ON DELETE CASCADE,
	FOREIGN KEY(sellerId) REFERENCES seller(userId) ON DELETE CASCADE,
	FOREIGN KEY(seekerId) REFERENCES property_seeker(userId) ON DELETE CASCADE,
	FOREIGN KEY(agentId) REFERENCES agent(userId) ON DELETE SET NULL,
	CHECK(transactionAmount>=0),
	CHECK(transactionType IN('rent','sale'))
);

-- Triggers:

-- for increasing the dealcount and rentcount when it is hit in the transaction
DELIMITER $
CREATE TRIGGER updateAgentCounts
AFTER INSERT ON transactions
FOR EACH ROW
BEGIN
	IF NEW.agentId IS NOT NULL THEN
		IF NEW.transactionType='sale' THEN
			UPDATE agent
			SET dealCount=dealCount+1
			WHERE userId=NEW.agentId;
		ELSEIF NEW.transactionType='rent' THEN
			UPDATE agent
			SET rentCount=rentCount+1
			WHERE userId=NEW.agentId;
		END IF;
	END IF;
END$$
DELIMITER ;

-- for changing the status of the property in the properties entity after property added in the transaction entity
DELIMITER $$

CREATE TRIGGER updatePropertyStatus
AFTER INSERT ON transactions
FOR EACH ROW
BEGIN
	IF NEW.transactionType='sale' THEN
		UPDATE property
		SET status='sold'
		WHERE property_id=NEW.propertyId;
	ELSEIF NEW.transactionType='rent' THEN
		UPDATE property		SET status='rented'
		WHERE property_id=NEW.propertyId;
	END IF;
END$$

DELIMITER ;

-- for updating the updated_on attribute in the properties 
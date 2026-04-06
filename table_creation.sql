-- Active: 1774687611691@@127.0.0.1@3306@apex_estates
USE apex_estates;

select * from agent;

ALTER TABLE property
MODIFY listed_on DATE;

alter TABLE users
MODIFY passwordHash VARCHAR(64) NOT NULL;	
CREATE TABLE users(
	userId INT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(50) NOT NULL,
	email VARCHAR(64) NOT NULL UNIQUE,
	phoneNumber CHAR(10) UNIQUE,
	passwordHash VARCHAR(64) NOT NULL,
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

-- for updating total properties count for the seller if propert added

DELIMITER$$

CREATE TRIGGER newPropertyAddedInSeller
AFTER INSERT ON property
FOR EACH ROW
BEGIN
	UPDATE seller
	SET totalProperties= totalProperties+1
	WHERE userId=NEW.sellerID;
END$$
DELIMITER ;

-- for updating total properties count for the seller if property is deleted

DELIMITER $$

CREATE TRIGGER propertyDeletedInSeller
AFTER DELETE ON property
FOR EACH ROW
BEGIN
	UPDATE seller
	SET totalProperties = totalProperties-1;
	WHERE userId = OLD.sellerID;
END$$
DELIMITER ;


-- if the agent id becomes null in the properties table then for auto assigning the agent to the 
-- property with least number of deals and rents

DELIMITER $$
CREATE TRIGGER addAgent
BEFORE INSERT ON property
FOR EACH ROW 
BEGIN
	IF NEW.agentId is NULL THEN
		SELECT userId INTO NEW.agentId
		FROM agent
		ORDER BY (dealCount+rentCount) ASC
		LIMIT 1;
	END IF;
END$$
DELIMITER ;

-- trigger for incresing the rating of the agent for successful transaction
-- DELIMITER $$
-- CREATE TRIGGER rating_increment
-- BEFORE UPDATE ON transactions
-- FOR EACH ROW
-- BEGIN


-- trigger for checking if the property is already sold or rented before inserting into transactions
DELIMITER $$

CREATE TRIGGER checkPropertyAvailability
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    DECLARE currentStatus VARCHAR(20);

    SELECT status INTO currentStatus
    FROM property
    WHERE property_id = NEW.propertyId;

    IF currentStatus IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Property does not exist';

    ELSEIF currentStatus != 'available' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Property already sold or rented';

    END IF;
END$$

DELIMITER ;

-- trigger for checking listing type before proceeding for transaction
DELIMITER $$

CREATE TRIGGER checkListingTypeMatch
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    DECLARE listingType VARCHAR(10);

    SELECT listing_type INTO listingType
    FROM property
    WHERE property_id = NEW.propertyId;

    IF listingType != NEW.transactionType THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Transaction type does not match property listing type';
    END IF;
END$$

DELIMITER ;

--trigger for checking that the seller is not transacting with himself/herself
DELIMITER $$

CREATE TRIGGER preventSelfTransaction
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    DECLARE ownerId INT;

    SELECT sellerID INTO ownerId
    FROM property
    WHERE property_id = NEW.propertyId;

    IF ownerId = NEW.seekerId THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Seller cannot buy/rent their own property';
    END IF;
END$$

DELIMITER ;

drop trigger checkPropertyAvailability;
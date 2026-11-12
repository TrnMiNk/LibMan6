-- MySQL 8.0.16+ / InnoDB. No invented fine tariffs.
CREATE DATABASE IF NOT EXISTS LibMan6 CHARACTER SET utf8mb4;
USE LibMan6;

CREATE TABLE Reader6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  fullName VARCHAR(120) NOT NULL,
  phone VARCHAR(30) NULL,
  email VARCHAR(150) NULL,
  classOrDepartment VARCHAR(120) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE LibraryStaff6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  fullName VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL,
  isActive BOOLEAN NOT NULL,
  CONSTRAINT ck_LibraryStaff6_role CHECK (role IN ('LIBRARIAN','MANAGER'))
) ENGINE=InnoDB;

CREATE TABLE Account6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(80) NOT NULL,
  passwordHash VARCHAR(255) NOT NULL,
  accountType VARCHAR(20) NOT NULL,
  isActive BOOLEAN NOT NULL,
  readerId BIGINT NULL,
  staffId BIGINT NULL,
  CONSTRAINT uq_Account6_readerId UNIQUE (readerId),
  CONSTRAINT fk_Account6_readerId FOREIGN KEY (readerId) REFERENCES Reader6(id) ON DELETE RESTRICT,
  CONSTRAINT uq_Account6_staffId UNIQUE (staffId),
  CONSTRAINT fk_Account6_staffId FOREIGN KEY (staffId) REFERENCES LibraryStaff6(id) ON DELETE RESTRICT,
  CONSTRAINT uq_Account6_username UNIQUE (username),
  CONSTRAINT ck_Account6_owner CHECK ((accountType='READER' AND staffId IS NULL) OR (accountType='STAFF' AND staffId IS NOT NULL AND readerId IS NULL))
) ENGINE=InnoDB;

CREATE TABLE Supplier6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  supplierName VARCHAR(150) NOT NULL,
  phone VARCHAR(30) NULL,
  email VARCHAR(150) NULL
) ENGINE=InnoDB;

CREATE TABLE Document6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  category VARCHAR(30) NOT NULL,
  author VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE LibraryCard6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  status VARCHAR(20) NOT NULL,
  registeredAt DATETIME NOT NULL,
  readerId BIGINT NOT NULL,
  CONSTRAINT uq_LibraryCard6_readerId UNIQUE (readerId),
  CONSTRAINT fk_LibraryCard6_readerId FOREIGN KEY (readerId) REFERENCES Reader6(id) ON DELETE RESTRICT,
  CONSTRAINT ck_LibraryCard6_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE BorrowRecord6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  borrowDate DATE NOT NULL,
  libraryCardId BIGINT NOT NULL,
  CONSTRAINT fk_BorrowRecord6_libraryCardId FOREIGN KEY (libraryCardId) REFERENCES LibraryCard6(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE BorrowDetail6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  borrowQuantity INT NOT NULL,
  dueDate DATE NOT NULL,
  borrowRecordId BIGINT NOT NULL,
  documentId BIGINT NOT NULL,
  CONSTRAINT fk_BorrowDetail6_borrowRecordId FOREIGN KEY (borrowRecordId) REFERENCES BorrowRecord6(id) ON DELETE RESTRICT,
  CONSTRAINT fk_BorrowDetail6_documentId FOREIGN KEY (documentId) REFERENCES Document6(id) ON DELETE RESTRICT,
  CONSTRAINT ck_BorrowDetail6_quantity CHECK (borrowQuantity > 0),
  CONSTRAINT uq_BorrowDetail6_line UNIQUE (borrowRecordId,documentId)
) ENGINE=InnoDB;

CREATE TABLE ReturnSlip6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  returnDate DATE NOT NULL,
  fineSettled BOOLEAN NOT NULL,
  requestKey VARCHAR(36) NOT NULL,
  libraryCardId BIGINT NOT NULL,
  staffId BIGINT NOT NULL,
  CONSTRAINT fk_ReturnSlip6_libraryCardId FOREIGN KEY (libraryCardId) REFERENCES LibraryCard6(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ReturnSlip6_staffId FOREIGN KEY (staffId) REFERENCES LibraryStaff6(id) ON DELETE RESTRICT,
  CONSTRAINT uq_ReturnSlip6_requestKey UNIQUE (requestKey)
) ENGINE=InnoDB;

CREATE TABLE ReturnDetail6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  returnedQuantity INT NOT NULL,
  damagedQuantity INT NOT NULL,
  conditionNote VARCHAR(300) NULL,
  fineAmount DECIMAL(15,2) NOT NULL,
  returnSlipId BIGINT NOT NULL,
  borrowDetailId BIGINT NOT NULL,
  CONSTRAINT fk_ReturnDetail6_returnSlipId FOREIGN KEY (returnSlipId) REFERENCES ReturnSlip6(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ReturnDetail6_borrowDetailId FOREIGN KEY (borrowDetailId) REFERENCES BorrowDetail6(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ReturnDetail6_quantity CHECK (returnedQuantity>0 AND damagedQuantity>=0 AND damagedQuantity<=returnedQuantity),
  CONSTRAINT ck_ReturnDetail6_fine CHECK (fineAmount>=0),
  CONSTRAINT uq_ReturnDetail6_line UNIQUE (returnSlipId,borrowDetailId)
) ENGINE=InnoDB;

CREATE TABLE DocumentImport6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  importDate DATE NOT NULL,
  supplierId BIGINT NOT NULL,
  staffId BIGINT NOT NULL,
  CONSTRAINT fk_DocumentImport6_supplierId FOREIGN KEY (supplierId) REFERENCES Supplier6(id) ON DELETE RESTRICT,
  CONSTRAINT fk_DocumentImport6_staffId FOREIGN KEY (staffId) REFERENCES LibraryStaff6(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE DocumentImportDetail6 (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  quantity INT NOT NULL,
  documentImportId BIGINT NOT NULL,
  documentId BIGINT NOT NULL,
  CONSTRAINT fk_DocumentImportDetail6_documentImportId FOREIGN KEY (documentImportId) REFERENCES DocumentImport6(id) ON DELETE RESTRICT,
  CONSTRAINT fk_DocumentImportDetail6_documentId FOREIGN KEY (documentId) REFERENCES Document6(id) ON DELETE RESTRICT,
  CONSTRAINT ck_DocumentImportDetail6_quantity CHECK (quantity>0),
  CONSTRAINT uq_DocumentImportDetail6_line UNIQUE (documentImportId,documentId)
) ENGINE=InnoDB;

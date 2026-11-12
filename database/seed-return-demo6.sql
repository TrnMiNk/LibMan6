-- CHAY THU CONG tren DB hien co. Khong chay schema.sql, khong sua du lieu ban doc that.
-- Moi bo demo duoc nhan dien bang username/ten rieng; chay lai khong tao them phieu muon.
-- passwordHash la gia tri khoa dang nhap, KHONG phai hash hop le de xac thuc.
USE LibMan6;
START TRANSACTION;

SET @newStaff6 = NOT EXISTS (SELECT 1 FROM Account6 WHERE username='staff_return_demo6');
INSERT INTO LibraryStaff6(fullName,role,isActive)
SELECT '[DEMO RETURN6] Nhan vien thu vien','LIBRARIAN',TRUE WHERE @newStaff6;
SET @staff6 = IF(@newStaff6, LAST_INSERT_ID(),
  (SELECT s.id FROM Account6 a JOIN LibraryStaff6 s ON s.id=a.staffId
   WHERE a.username='staff_return_demo6' AND a.accountType='STAFF' AND s.fullName='[DEMO RETURN6] Nhan vien thu vien'));
INSERT INTO Account6(username,passwordHash,accountType,isActive,readerId,staffId)
SELECT 'staff_return_demo6','!DEMO_ONLY_LOGIN_DISABLED!','STAFF',TRUE,NULL,@staff6
WHERE @newStaff6 AND @staff6 IS NOT NULL;

SET @newReader6 = NOT EXISTS (SELECT 1 FROM Account6 WHERE username='reader_return_demo6');
INSERT INTO Reader6(fullName,phone,email,classOrDepartment)
SELECT '[DEMO RETURN6] Ban doc rieng',NULL,NULL,'Lop demo nhan tra' WHERE @newReader6;
SET @reader6 = IF(@newReader6, LAST_INSERT_ID(),
  (SELECT r.id FROM Account6 a JOIN Reader6 r ON r.id=a.readerId
   WHERE a.username='reader_return_demo6' AND a.accountType='READER' AND r.fullName='[DEMO RETURN6] Ban doc rieng'));
INSERT INTO Account6(username,passwordHash,accountType,isActive,readerId,staffId)
SELECT 'reader_return_demo6','!DEMO_ONLY_LOGIN_DISABLED!','READER',TRUE,@reader6,NULL
WHERE @newReader6 AND @reader6 IS NOT NULL;
INSERT INTO LibraryCard6(status,registeredAt,readerId)
SELECT 'ACTIVE',NOW(),@reader6 WHERE @reader6 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM LibraryCard6 WHERE readerId=@reader6);
SET @card6 = (SELECT id FROM LibraryCard6 WHERE readerId=@reader6);

INSERT INTO Document6(title,category,author)
SELECT '[DEMO RETURN6] Tai lieu dung han','DEMO','LibMan6'
WHERE NOT EXISTS (SELECT 1 FROM Document6 WHERE title='[DEMO RETURN6] Tai lieu dung han' AND category='DEMO' AND author='LibMan6');
INSERT INTO Document6(title,category,author)
SELECT '[DEMO RETURN6] Tai lieu qua han','DEMO','LibMan6'
WHERE NOT EXISTS (SELECT 1 FROM Document6 WHERE title='[DEMO RETURN6] Tai lieu qua han' AND category='DEMO' AND author='LibMan6');
SET @onTime6 = (SELECT MIN(id) FROM Document6 WHERE title='[DEMO RETURN6] Tai lieu dung han' AND category='DEMO' AND author='LibMan6');
SET @late6 = (SELECT MIN(id) FROM Document6 WHERE title='[DEMO RETURN6] Tai lieu qua han' AND category='DEMO' AND author='LibMan6');
SET @newBorrow6 = @card6 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM BorrowRecord6 WHERE libraryCardId=@card6);
INSERT INTO BorrowRecord6(borrowDate,libraryCardId) SELECT CURDATE()-INTERVAL 14 DAY,@card6 WHERE @newBorrow6;
SET @borrow6 = IF(@newBorrow6,LAST_INSERT_ID(),NULL);
INSERT INTO BorrowDetail6(borrowQuantity,dueDate,borrowRecordId,documentId)
SELECT 3,CURDATE()+INTERVAL 14 DAY,@borrow6,@onTime6 WHERE @borrow6 IS NOT NULL;
INSERT INTO BorrowDetail6(borrowQuantity,dueDate,borrowRecordId,documentId)
SELECT 4,CURDATE()-INTERVAL 3 DAY,@borrow6,@late6 WHERE @borrow6 IS NOT NULL;
COMMIT;

-- Ma the de nhap vao man hinh nhan tra. NULL: username da co nhung khong khop bo demo,
-- can kiem tra thu cong, KHONG ghi de tai khoan hien co.
SELECT @card6 AS demoCardId, @staff6 AS demoStaffId,
  'staff_return_demo6' AS staffUsername, 'reader_return_demo6' AS readerUsername;

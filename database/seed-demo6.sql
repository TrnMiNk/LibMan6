-- Chỉ chạy thủ công trên máy local nếu cần. Không ghi đè tài khoản có sẵn.
-- Giá trị đánh dấu này KHÔNG phải hash mật khẩu hợp lệ; tài khoản chỉ dùng demo,
-- phải bị từ chối bởi module đăng nhập tương lai cho đến khi đặt hash thật.
USE LibMan6;
INSERT INTO Account6(username, passwordHash, accountType, isActive, readerId, staffId)
SELECT 'reader_demo6', '!DEMO_ONLY_LOGIN_DISABLED!', 'READER', TRUE, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM Account6 WHERE username = 'reader_demo6');

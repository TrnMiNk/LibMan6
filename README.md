# LibMan6 — Bản chạy local

JSP/Servlet `javax.servlet`, JDBC, MySQL và Tomcat 9. Có đăng nhập/đăng xuất,
đăng ký thẻ cho bạn đọc và nhận trả/in phiếu cho nhân viên. Không đổi schema,
không thêm dependency, không triển khai CRUD sách, mượn sách, thống kê hay thanh toán online.

## Chạy ngay

Mở **http://localhost:8080/LibMan6/**. Trang gốc dẫn tới đăng nhập hoặc trang chủ
đúng vai trò khi đã đăng nhập. Bản cuối đã triển khai vào `C:\tools\apache-tomcat-9.0.123`.

Tài khoản **chỉ dùng local** đã được tạo bằng `tools/setup-demo6.ps1`:

| Tên đăng nhập | Vai trò / dữ liệu |
| --- | --- |
| ban_doc01 | READER, chưa có hồ sơ/thẻ trước khi người dùng tự đăng ký |
| ban_doc02 | READER thứ hai, độc lập, chưa có hồ sơ/thẻ |
| thu_thu01 | STAFF, nhân viên Nguyễn Thu Hà, LIBRARIAN |
| ban_doc_muon | READER Trần Minh Anh, có thẻ và tài liệu mượn để thử nhận trả |

Mật khẩu do người chạy setup tự nhập local (ít nhất 12 ký tự), không ghi trong repo.
DB lưu hash PBKDF2 có salt, không lưu mật khẩu rõ. Không dùng bộ này trên môi trường công khai.
Setup chỉ thêm khi username chưa có; không đổi mật khẩu hoặc ghi đè dữ liệu đã tồn tại.
Dữ liệu mẫu cần giữ để người dùng thao tác, không bị dọn như dữ liệu test tự động.
Tài khoản local đã có tiếp tục dùng mật khẩu đã thiết lập trước đây.

### Một luồng đăng ký

Đăng nhập `ban_doc01` → Đăng ký thẻ → nhập họ tên, lớp/đơn vị, điện thoại/email nếu có
→ Add. Thẻ liên kết riêng với tài khoản đang đăng nhập. Đăng xuất rồi dùng `ban_doc02`
để thử bạn đọc khác. Tài khoản đã có thẻ hiển thị lại thẻ cũ; refresh không INSERT lại.
Hai tài khoản này được để chưa có thẻ để người dùng tự demo.

### Một luồng nhận trả

Đăng nhập `thu_thu01` → Nhận trả tài liệu → nhập **mã thẻ 18** (bộ mẫu tạo lần này)
→ Tìm kiếm → chọn dòng → nhập số lượng trả/hỏng → Xem trước → chọn thanh toán →
Xác nhận → In / In lại. Có tài liệu đúng hạn và quá hạn, cho phép trả một phần.
Nếu chạy setup ở database khác, dùng mã in từ công cụ hoặc SELECT:

```sql
SELECT c.id AS cardId
FROM Account6 a JOIN LibraryCard6 c ON c.readerId=a.readerId
WHERE a.username='ban_doc_muon';
```

Phiếu đã lưu đọc bằng `/staff6/return?action=receipt&slipId=MA_PHIEU`, chỉ nhân viên
sở hữu phiếu được mở lại. Refresh/in lại chỉ GET, không thay giao dịch.
Nếu muốn in lại sau đăng xuất, đăng nhập lại nhân viên đã lập phiếu rồi mở URL đó.
Không coi mở/đóng hộp thoại in là bằng chứng đã in thành công.
Đã giữ phiếu mẫu **#19**, phạt **23000 VND**, của nhân viên `thu_thu01`:
<http://localhost:8080/LibMan6/staff6/return?action=receipt&slipId=19>.
Phiếu mẫu đã trả một phần, vẫn còn số lượng để người dùng tiếp tục demo với thẻ 18.

## Chính sách phạt minh họa

Giữ `FinePolicy6` và `ConfiguredFinePolicy6`. Cấu hình hiện tại trong
`src/main/resources/fine-policy.properties` được cho phép riêng cho lần demo này:

```properties
dailyRate=1000
damageRate=20000
```

Đơn vị VND. **Đây là đơn giá minh họa, không phải chính sách chính thức của đề/thư viện.**

- Phạt quá hạn = max(0, số ngày quá hạn theo ngày server) × số lượng trả × dailyRate.
- Phạt hỏng = số lượng hỏng × damageRate.
- Tổng phạt = tổng hai khoản của tất cả dòng.

Ví dụ quá hạn 3 ngày, trả 1 tài liệu, hỏng 1: 23000 VND. BigDecimal, tối đa 2 chữ số
thập phân, giới hạn DECIMAL(15,2). Cho phép cấu hình 0 tường minh; thiếu đơn giá cho
khoản phát sinh sẽ chặn xử lý, ghi chi tiết log server và báo UI ngắn gọn.
Tổng 0 hiển thị “Không phát sinh phạt” độc lập với boolean thanh toán đã lưu;
không UPDATE các phiếu cũ để sửa hiển thị. Chọn thanh toán lúc xác nhận, chưa có
module thu tiền/cập nhật thanh toán sau lưu. Thay cấu hình cần build/triển khai lại.

## Đăng nhập và kiểm soát truy cập

- `PasswordHash6`: định dạng `pbkdf2_sha256$iterations$salt$hash`, PBKDF2WithHmacSHA256,
  600000 vòng, salt ngẫu nhiên 16 byte, hash 256 bit, so sánh constant-time.
  Các marker DEMO_ONLY_LOGIN_DISABLED/DISABLED_DEMO_LOGIN không được xác thực.
- `AccountDAO6.authenticate` xác thực hash và trạng thái tài khoản. STAFF phải liên kết
  nhân viên đang hoạt động, role LIBRARIAN/MANAGER; READER không có staffId.
- Session dùng `authenticatedAccountId6` (Long) và `account6` (Account6, không chứa hash).
  Đăng nhập đổi session ID, xóa bản xem trước/dữ liệu danh tính trước, đổi CSRF token.
  Logout POST có CSRF, hủy session. Cookie HttpOnly, chỉ tracking bằng cookie.
- `AccessFilter6` kiểm tra lại quyền/trạng thái ở server trên cả REQUEST/FORWARD,
  bảo vệ toàn bộ Controller và JSP dưới reader6/staff6, dựa trên servletPath đã chuẩn hóa.
  READER không được gọi STAFF bằng URL hoặc POST; nhân viên không đăng ký thay bạn đọc.
- Cơ chế tự chọn username demo đã bị gỡ khỏi hai Controller. Hai file cấu hình cũ
  `registration.properties`, `staff-demo.properties` chỉ còn để tương thích lịch sử,
  không dùng để gán danh tính và có demo.enabled=false.
- Lỗi đăng nhập trả thông báo chung. UI/bản in không hiển thị SQLException, stack trace,
  đường dẫn, thông tin kết nối, properties, transaction hoặc hướng dẫn triển khai.
  Trang lỗi chung che chi tiết lỗi ngoài dự kiến; server vẫn log lỗi để xử lý.

## Bảo toàn nghiệp vụ

Đăng ký giữ transaction Reader + Card + Account, khóa tài khoản, rollback khi lỗi,
PreparedStatement và PRG. Module nhận trả giữ kiểm tra thẻ ACTIVE/quyền sở hữu,
số lượng/ghi chú, preview không INSERT; xác nhận READ COMMITTED, khóa nhân viên/thẻ
và BorrowDetail theo ID tăng dần rồi đọc tổng đã trả mới nhất, tính lại phạt.
Dữ liệu/ngày/phạt thay đổi yêu cầu xem trước lại. Phiếu và chi tiết cùng transaction,
rollback toàn bộ khi lỗi; UNIQUE requestKey chống lưu lặp và kiểm tra ngữ cảnh nhân viên/thẻ.
Số đã trả và tổng phạt vẫn là thuộc tính dẫn xuất; không có cột tồn kho mới.

JSP/include khai báo UTF-8 riêng. Nhãn/radio cùng hàng, input text giữ bố cục cũ.
CSS in chỉ hiện `#receipt-content`, ẩn điều hướng/nút/thông báo/hướng dẫn.
Tiền tố `[DEMO RETURN6] ` trong dữ liệu mẫu cũ chỉ được bỏ khi hiển thị,
không sửa hàng loạt dữ liệu thật, username hoặc ID. Bộ mẫu mới có tên tự nhiên.

## Build, setup và triển khai

Giữ `src/main/resources/db.properties` hiện có, không in/commit tệp này.
Không chạy lại `database/schema.sql` hoặc xóa database.

Khi lấy mã nguồn trên máy mới, tạo database bằng `database/schema.sql` **chỉ khi
chưa có các bảng**, rồi sao chép file cấu hình mẫu và điền kết nối riêng:

```powershell
Copy-Item .\src\main\resources\db.properties.example .\src\main\resources\db.properties
```

`db.properties.example` không chứa mật khẩu thật. `.gitignore` loại trừ cấu hình
thật, `.env`, log, `target/` và WAR; không đưa các file đó lên GitHub.

```powershell
mvn "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean package
powershell -ExecutionPolicy Bypass -File .\tools\setup-demo6.ps1
Copy-Item -LiteralPath .\target\LibMan6.war -Destination C:\tools\apache-tomcat-9.0.123\webapps\LibMan6.war -Force
```

Tomcat đang bật autoDeploy, không cần kill Java hoặc restart tiến trình khác.
Nếu chưa chạy, khởi động đúng instance trong PowerShell riêng:

```powershell
$env:CATALINA_HOME = 'C:\tools\apache-tomcat-9.0.123'
$env:CATALINA_BASE = $env:CATALINA_HOME
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

Các SQL seed cũ chỉ dành cho phiên bản demo chưa có đăng nhập; không cần chạy chúng
cho bản cuối. `setup-demo6.ps1` tạo tài khoản băm đúng và bộ mượn riêng trong transaction.
Script hỏi mật khẩu bằng đầu vào ẩn, truyền tạm qua `LIBMAN6_DEMO_PASSWORD` và
khôi phục biến môi trường sau khi chạy. Không đưa giá trị vào file hoặc dòng lệnh.
Khi chạy `DeploymentCheck6` riêng, cung cấp biến này bằng mật khẩu local đã thiết lập;
không đưa mật khẩu vào commit hoặc tài liệu công khai.

## Kiểm tra

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\check-registration6.ps1
powershell -ExecutionPolicy Bypass -File .\tools\check-return6.ps1
```

Đã chạy MySQL thật, HTTP/JSP trên Tomcat 9 ở cổng tạm: đăng nhập đúng/sai/inactive/
marker/CSRF, đổi session ID, logout, chặn READER với cả JSP/POST nhân viên, STAFF/MANAGER
và liên kết nhân viên ngừng hoạt động; hai READER tạo hồ sơ/thẻ riêng, dữ liệu sai không
INSERT, rollback đăng ký, đăng ký đồng thời, PRG/refresh. Nhận trả: preview chỉ đọc,
phạt quá hạn/hỏng, thiếu/0/âm/vượt giới hạn tiền, trả một phần, sai thẻ, vượt/trùng dòng,
ghi chú dài, nhân viên inactive, rollback khi chi tiết thứ hai lỗi, stale preview,
requestKey lặp/sai ngữ cảnh, trả đồng thời không vượt; UTF-8 include/response, escape HTML,
radio, phạt 0, vùng in, GET lặp cùng slipId và số bản ghi DB không đổi.
Dữ liệu test tạm được dọn đúng các ID đã tạo, dữ liệu demo được giữ.

WAR đã build và triển khai đúng instance Tomcat 9 cổng 8080, đối chiếu SHA-256 source WAR
và WAR triển khai. `tools/DeploymentCheck6.java` đã kiểm tra sau deploy: URL gốc,
đăng nhập hai READER (giữ chưa có thẻ), chặn sai vai trò cả đường dẫn chuẩn hóa,
tra thẻ 18, preview không INSERT, xác nhận/trả lại cùng requestKey cùng phiếu,
phạt quá hạn/hỏng 23000 VND, UTF-8 và không có thông báo kỹ thuật trên UI,
vùng in, refresh phiếu 19 không thêm phiếu/chi tiết, đăng xuất mất quyền.
Kiểm tra trực quan qua Browser Use không thực hiện được: browser host
không sẵn sàng, công cụ native dừng do không xác định chắc chắn URL của Brave.
Không tiếp tục thao tác UI sau khi bị dừng; không tuyên bố đã kiểm tra browser/print preview
hoặc máy in. HTTP/JSP và nội dung/CSS in đã kiểm tra, còn máy quét vật lý và bản in giấy chưa kiểm tra.

## Phần bổ sung so với UML nghiệp vụ

Không đổi diagram. Các lớp/phương thức mới để cập nhật báo cáo sau:

- `Account6`: danh tính session, constructor/getter (không giữ passwordHash).
- `AccountDAO6.authenticate(username, password)`, `findActiveAccount(id)`.
- `PasswordHash6.hash`, `verify`: helper bảo vệ mật khẩu.
- `AuthController6.doGet/doPost`, `prepare`, `home`: đăng nhập/đăng xuất và điều hướng.
- `AccessFilter6.doFilter`: kiểm soát quyền chung cho Controller/JSP.
- `LoginUI6.jsp`, `ErrorUI6.jsp`, `WEB-INF/Header6.jspf`, `WEB-INF/web.xml`.
- `DemoSetup6`/setup script và `AuthTestSupport6`: công cụ local/test, không phải lớp nghiệp vụ UML.
- `DeploymentCheck6`: kiểm tra sau deploy; chỉ lưu tối đa một phiếu thuộc bộ demo riêng.
- Đã có từ module nhận trả: `LibraryStaffDAO6.findByAccountId/requireActive`,
  `ConfiguredFinePolicy6` (implementation cần thiết của interface),
  `LibraryCardDAO6.searchCard` và các thực thể liên kết theo schema.

Không viết báo cáo hay triển khai thêm module ngoài hai luồng trên.

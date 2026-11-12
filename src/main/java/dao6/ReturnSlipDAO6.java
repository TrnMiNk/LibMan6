package dao6;

import model6.*;
import policy6.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReturnSlipDAO6 extends DAO6 {
    private final FinePolicy6 policy;

    public ReturnSlipDAO6() { this(new ConfiguredFinePolicy6()); }
    public ReturnSlipDAO6(FinePolicy6 policy) { this.policy = Objects.requireNonNull(policy); }

    public ReturnSlip6 previewReturn(long cardId, List<ReturnDetail6> items) throws SQLException {
        try (Connection connection = getConnection()) {
            LibraryCard6 card = LibraryCardDAO6.searchCard(connection, cardId, false);
            requireActive(card);
            return calculate(card, items, BorrowDetailDAO6.getBorrowedItems(connection, cardId), LocalDate.now());
        }
    }

    private static void requireActive(LibraryCard6 card) {
        if (!"ACTIVE".equals(card.getStatus())) throw new IllegalArgumentException("Thẻ thư viện không còn hoạt động.");
    }

    private ReturnSlip6 calculate(LibraryCard6 card, List<ReturnDetail6> items,
                                  List<BorrowDetail6> borrowed, LocalDate date) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Chọn ít nhất một tài liệu để trả.");
        Map<Long, BorrowDetail6> available = new HashMap<>();
        borrowed.forEach(item -> available.put(item.getId(), item));
        Set<Long> seen = new HashSet<>();
        List<ReturnDetail6> details = new ArrayList<>();
        for (ReturnDetail6 input : items) {
            if (input == null || input.getBorrowDetail() == null || !seen.add(input.getBorrowDetail().getId())) {
                throw new IllegalArgumentException("Dòng trả không hợp lệ hoặc bị trùng mã chi tiết mượn.");
            }
            BorrowDetail6 source = available.get(input.getBorrowDetail().getId());
            if (source == null) throw new IllegalArgumentException("Tài liệu không thuộc thẻ hoặc đã được trả hết. Hãy tra cứu lại.");
            if (input.getReturnedQuantity() <= 0 || input.getReturnedQuantity() > source.getRemainingQuantity()) {
                throw new IllegalArgumentException(input.getReturnedQuantity() <= 0 ? "Số lượng trả phải lớn hơn 0." : "Số lượng trả vượt quá số lượng còn mượn.");
            }
            if (input.getDamagedQuantity() < 0 || input.getDamagedQuantity() > input.getReturnedQuantity()) {
                throw new IllegalArgumentException("Số lượng hỏng phải từ 0 đến số lượng trả.");
            }
            String note = input.getConditionNote() == null ? "" : input.getConditionNote().strip();
            if (note.length() > 300) throw new IllegalArgumentException("Ghi chú tình trạng tối đa 300 ký tự.");
            ReturnDetail6 detail = new ReturnDetail6();
            detail.setBorrowDetail(source); detail.setReturnedQuantity(input.getReturnedQuantity());
            detail.setDamagedQuantity(input.getDamagedQuantity()); detail.setConditionNote(note);
            try {
                detail.setFineAmount(policy.calculateFine(source, detail, date));
                ConfiguredFinePolicy6.checkMoney(detail.getFineAmount());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Không thể tính tiền phạt. Vui lòng liên hệ người quản trị.",exception);
            }
            details.add(detail);
        }
        details.sort(Comparator.comparingLong(item -> item.getBorrowDetail().getId()));
        ReturnSlip6 slip = new ReturnSlip6();
        slip.setLibraryCard(card); slip.setReturnDate(date); slip.setDetails(List.copyOf(details));
        slip.setRequestKey(UUID.randomUUID().toString());
        try { ConfiguredFinePolicy6.checkMoney(slip.getTotalFine()); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Không thể tính tiền phạt. Vui lòng liên hệ người quản trị.",exception); }
        return slip;
    }

    public ReturnSlip6 confirmReturnTransaction(ReturnSlip6 request, long staffId) throws SQLException {
        if (request == null || request.getLibraryCard() == null || request.getStaff() == null
                || request.getStaff().getId() != staffId || request.getRequestKey() == null
                || !UUID.fromString(request.getRequestKey()).toString().equals(request.getRequestKey())) {
            throw new IllegalArgumentException("Bản xem trước không hợp lệ. Vui lòng xem trước lại.");
        }
        long cardId = request.getLibraryCard().getId();
        try (Connection connection = getConnection()) {
            // READ COMMITTED: sau khi chờ khóa phải đọc SUM mới nhất, không dùng snapshot cũ.
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                LibraryStaff6 staff = LibraryStaffDAO6.requireActive(connection, staffId);
                LibraryCard6 card = LibraryCardDAO6.searchCard(connection, cardId, true);
                Long savedId = findByRequestKey(connection, request.getRequestKey());
                if (savedId != null) {
                    ReturnSlip6 saved = getSavedReceipt(connection, savedId);
                    if (saved.getStaff().getId() != staffId || saved.getLibraryCard().getId() != cardId) {
                        throw new IllegalArgumentException("Mã yêu cầu không thuộc ngữ cảnh nhân viên/thẻ này.");
                    }
                    connection.commit();
                    return saved;
                }
                requireActive(card);
                List<ReturnDetail6> selected = new ArrayList<>(request.getDetails());
                if (selected.isEmpty() || selected.stream().anyMatch(item -> item == null || item.getBorrowDetail() == null)) {
                    throw new IllegalArgumentException("Chọn ít nhất một dòng trả hợp lệ.");
                }
                selected.sort(Comparator.comparingLong(item -> item.getBorrowDetail().getId()));
                Set<Long> locked = new HashSet<>();
                for (ReturnDetail6 item : selected) {
                    long id = item.getBorrowDetail().getId();
                    if (!locked.add(id)) throw new IllegalArgumentException("Trùng dòng tài liệu trả.");
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT b.id FROM BorrowDetail6 b JOIN BorrowRecord6 br ON br.id=b.borrowRecordId "
                                    + "WHERE b.id=? AND br.libraryCardId=? FOR UPDATE")) {
                        statement.setLong(1, id); statement.setLong(2, cardId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) throw new IllegalArgumentException("Tài liệu không thuộc thẻ đang xử lý.");
                        }
                    }
                }
                ReturnSlip6 fresh = calculate(card, selected, BorrowDetailDAO6.getBorrowedItems(connection, cardId), LocalDate.now());
                if (!fresh.getReturnDate().equals(request.getReturnDate()) || fresh.getTotalFine().compareTo(request.getTotalFine()) != 0) changed();
                for (int i = 0; i < fresh.getDetails().size(); i++) {
                    ReturnDetail6 now = fresh.getDetails().get(i), before = selected.get(i);
                    BorrowDetail6 a = now.getBorrowDetail(), b = before.getBorrowDetail();
                    if (a.getBorrowQuantity() != b.getBorrowQuantity() || a.getReturnedQuantity() != b.getReturnedQuantity()
                            || !a.getDueDate().equals(b.getDueDate()) || now.getFineAmount().compareTo(before.getFineAmount()) != 0) changed();
                }
                fresh.setRequestKey(request.getRequestKey()); fresh.setStaff(staff); fresh.setFineSettled(request.isFineSettled());
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO ReturnSlip6(returnDate,fineSettled,requestKey,libraryCardId,staffId) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setDate(1, java.sql.Date.valueOf(fresh.getReturnDate())); statement.setBoolean(2, fresh.isFineSettled());
                    statement.setString(3, fresh.getRequestKey()); statement.setLong(4, cardId); statement.setLong(5, staffId);
                    statement.executeUpdate(); fresh.setId(generatedId(statement));
                }
                for (ReturnDetail6 item : fresh.getDetails()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO ReturnDetail6(returnedQuantity,damagedQuantity,conditionNote,fineAmount,returnSlipId,borrowDetailId) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        statement.setInt(1, item.getReturnedQuantity()); statement.setInt(2, item.getDamagedQuantity());
                        statement.setString(3, item.getConditionNote()); statement.setBigDecimal(4, item.getFineAmount());
                        statement.setLong(5, fresh.getId()); statement.setLong(6, item.getBorrowDetail().getId());
                        statement.executeUpdate(); item.setId(generatedId(statement));
                    }
                }
                connection.commit();
                return fresh;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
                throw exception;
            }
        }
    }

    private static void changed() { throw new IllegalArgumentException("Dữ liệu đã thay đổi. Vui lòng kiểm tra lại."); }

    private static long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Missing generated key");
            return keys.getLong(1);
        }
    }

    private static Long findByRequestKey(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM ReturnSlip6 WHERE requestKey=?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getLong(1) : null; }
        }
    }

    public ReturnSlip6 getSavedReceipt(long slipId) throws SQLException {
        try (Connection connection = getConnection()) { return getSavedReceipt(connection, slipId); }
    }

    private static ReturnSlip6 getSavedReceipt(Connection connection, long slipId) throws SQLException {
        ReturnSlip6 slip = new ReturnSlip6();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT rs.*,s.fullName,s.role,s.isActive FROM ReturnSlip6 rs JOIN LibraryStaff6 s ON s.id=rs.staffId WHERE rs.id=?")) {
            statement.setLong(1, slipId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Không tìm thấy phiếu trả đã lưu.");
                slip.setId(slipId); slip.setReturnDate(result.getDate("returnDate").toLocalDate());
                slip.setRequestKey(result.getString("requestKey")); slip.setFineSettled(result.getBoolean("fineSettled"));
                slip.setLibraryCard(LibraryCardDAO6.searchCard(connection, result.getLong("libraryCardId"), false));
                LibraryStaff6 staff = new LibraryStaff6(); staff.setId(result.getLong("staffId"));
                staff.setFullName(result.getString("fullName")); staff.setRole(result.getString("role")); staff.setActive(result.getBoolean("isActive"));
                slip.setStaff(staff);
            }
        }
        List<ReturnDetail6> details = new ArrayList<>();
        String sql = "SELECT rd.id AS detailId,rd.returnedQuantity AS quantity,rd.damagedQuantity,rd.conditionNote,rd.fineAmount,"
                + "b.id,b.borrowQuantity,b.dueDate,b.borrowRecordId,br.borrowDate,d.id AS documentId,d.title,d.author,d.category,"
                + "COALESCE((SELECT SUM(x.returnedQuantity) FROM ReturnDetail6 x WHERE x.borrowDetailId=b.id),0) AS returnedQuantity "
                + "FROM ReturnDetail6 rd JOIN BorrowDetail6 b ON b.id=rd.borrowDetailId JOIN BorrowRecord6 br ON br.id=b.borrowRecordId "
                + "JOIN Document6 d ON d.id=b.documentId WHERE rd.returnSlipId=? ORDER BY b.id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slipId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ReturnDetail6 detail = new ReturnDetail6(); detail.setId(result.getLong("detailId"));
                    detail.setReturnedQuantity(result.getInt("quantity")); detail.setDamagedQuantity(result.getInt("damagedQuantity"));
                    detail.setConditionNote(result.getString("conditionNote")); detail.setFineAmount(result.getBigDecimal("fineAmount"));
                    detail.setBorrowDetail(BorrowDetailDAO6.read(result, slip.getLibraryCard().getId())); details.add(detail);
                }
            }
        }
        slip.setDetails(List.copyOf(details));
        return slip;
    }
}

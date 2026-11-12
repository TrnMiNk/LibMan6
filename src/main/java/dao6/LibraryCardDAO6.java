package dao6;

import model6.LibraryCard6;
import model6.Reader6;
import java.sql.*;
import java.time.LocalDateTime;

public class LibraryCardDAO6 extends DAO6 {
    public LibraryCard6 searchCard(long cardId) throws SQLException {
        try (Connection connection = getConnection()) { return searchCard(connection, cardId, false); }
    }

    static LibraryCard6 searchCard(Connection connection, long cardId, boolean lock) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT c.id,c.status,c.registeredAt,r.id AS readerId,r.fullName,r.phone,r.email,r.classOrDepartment "
                + "FROM LibraryCard6 c JOIN Reader6 r ON r.id=c.readerId WHERE c.id=?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setLong(1, cardId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Mã thẻ không hợp lệ.");
                Reader6 reader = new Reader6(result.getString("fullName"), result.getString("phone"),
                        result.getString("email"), result.getString("classOrDepartment"));
                reader.setId(result.getLong("readerId"));
                return new LibraryCard6(result.getLong("id"), result.getString("status"),
                        result.getTimestamp("registeredAt").toLocalDateTime(), reader);
            }
        }
    }

    public LibraryCard6 registerCard(Reader6 reader, long accountId) throws SQLException {
        if (!new ReaderDAO6().checkRegistrationInfo(reader)) {
            throw new IllegalArgumentException("Thông tin đăng ký không hợp lệ.");
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT accountType, isActive, readerId, staffId FROM Account6 WHERE id = ? FOR UPDATE")) {
                    statement.setLong(1, accountId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || !"READER".equals(result.getString("accountType"))
                                || !result.getBoolean("isActive") || result.getObject("staffId") != null) {
                            throw new IllegalStateException("Tài khoản không đủ điều kiện đăng ký thẻ.");
                        }
                        if (result.getObject("readerId") != null) {
                            throw new IllegalStateException("Tài khoản đã đăng ký bạn đọc. Không thể đăng ký thêm thẻ.");
                        }
                    }
                }
                long readerId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Reader6(fullName, phone, email, classOrDepartment) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, reader.getFullName());
                    statement.setString(2, reader.getPhone().isEmpty() ? null : reader.getPhone());
                    statement.setString(3, reader.getEmail().isEmpty() ? null : reader.getEmail());
                    statement.setString(4, reader.getClassOrDepartment());
                    statement.executeUpdate();
                    readerId = generatedId(statement);
                }
                LocalDateTime registeredAt = LocalDateTime.now().withNano(0);
                LibraryCard6 card = new LibraryCard6();
                card.setStatus("ACTIVE");
                card.setRegisteredAt(registeredAt);
                card.setReader(reader);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO LibraryCard6(status, registeredAt, readerId) VALUES ('ACTIVE', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setTimestamp(1, Timestamp.valueOf(registeredAt));
                    statement.setLong(2, readerId);
                    statement.executeUpdate();
                    card.setId(generatedId(statement));
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE Account6 SET readerId = ? WHERE id = ? AND readerId IS NULL")) {
                    statement.setLong(1, readerId);
                    statement.setLong(2, accountId);
                    if (statement.executeUpdate() != 1) throw new SQLException("Account link was not updated.");
                }
                connection.commit();
                reader.setId(readerId);
                return card;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); }
                catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        }
    }

    private static long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Missing generated key.");
            return keys.getLong(1);
        }
    }

    public LibraryCard6 findByAccountId(long accountId) throws SQLException {
        String sql = "SELECT c.id, c.status, c.registeredAt, r.id AS readerId, r.fullName, r.phone, r.email, r.classOrDepartment "
                + "FROM Account6 a JOIN Reader6 r ON r.id = a.readerId JOIN LibraryCard6 c ON c.readerId = r.id "
                + "WHERE a.id = ? AND a.accountType = 'READER' AND a.isActive = TRUE AND a.staffId IS NULL";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                Reader6 reader = new Reader6(result.getString("fullName"), result.getString("phone"),
                        result.getString("email"), result.getString("classOrDepartment"));
                reader.setId(result.getLong("readerId"));
                return new LibraryCard6(result.getLong("id"), result.getString("status"),
                        result.getTimestamp("registeredAt").toLocalDateTime(), reader);
            }
        }
    }
}

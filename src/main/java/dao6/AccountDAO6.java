package dao6;

import model6.Account6;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDAO6 extends DAO6 {
    public Account6 authenticate(String username, char[] password) throws SQLException {
        String encoded = null;
        long id = 0;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id,passwordHash FROM Account6 WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) { id = result.getLong("id"); encoded = result.getString("passwordHash"); }
            }
        }
        return PasswordHash6.verify(password, encoded) ? findActiveAccount(id) : null;
    }

    public Account6 findActiveAccount(long id) throws SQLException {
        String sql = "SELECT a.id,a.username,a.accountType,COALESCE(s.fullName,r.fullName,a.username) AS displayName "
                + "FROM Account6 a LEFT JOIN LibraryStaff6 s ON s.id=a.staffId LEFT JOIN Reader6 r ON r.id=a.readerId "
                + "WHERE a.id=? AND a.isActive=TRUE AND ((a.accountType='READER' AND a.staffId IS NULL) "
                + "OR (a.accountType='STAFF' AND a.readerId IS NULL AND s.isActive=TRUE AND s.role IN ('LIBRARIAN','MANAGER')))";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1,id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                String name = result.getString("displayName");
                if (name.startsWith("[DEMO RETURN6] ")) name = name.substring("[DEMO RETURN6] ".length());
                return new Account6(result.getLong("id"),result.getString("username"),result.getString("accountType"),name);
            }
        }
    }

    // Chỉ dùng username từ cấu hình server cho demo local; không xác thực mật khẩu.
    public long findDemoAccountId(String username) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM Account6 WHERE username = ? AND accountType = 'READER' AND isActive = TRUE AND staffId IS NULL")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong(1);
            }
        }
        throw new IllegalStateException("Chưa có tài khoản demo READER đang hoạt động. Hãy kiểm tra cấu hình và seed riêng.");
    }
}

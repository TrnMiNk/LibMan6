package dao6;

import model6.LibraryStaff6;
import java.sql.*;

public class LibraryStaffDAO6 extends DAO6 {
    public LibraryStaff6 findByAccountId(long accountId) throws SQLException {
        try (Connection connection = getConnection()) {
            return find(connection, "a.id = ?", accountId, null, false);
        }
    }

    public LibraryStaff6 findDemoStaff(String username) throws SQLException {
        try (Connection connection = getConnection()) {
            return find(connection, "a.username = ?", 0, username, false);
        }
    }

    static LibraryStaff6 requireActive(Connection connection, long staffId) throws SQLException {
        return find(connection, "s.id = ?", staffId, null, true);
    }

    private static LibraryStaff6 find(Connection connection, String condition, long id, String username, boolean lock) throws SQLException {
        String sql = "SELECT s.id,s.fullName,s.role FROM Account6 a JOIN LibraryStaff6 s ON s.id=a.staffId "
                + "WHERE " + condition + " AND a.accountType='STAFF' AND a.isActive=TRUE AND a.readerId IS NULL "
                + "AND s.isActive=TRUE AND s.role IN ('LIBRARIAN','MANAGER')" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (username == null) statement.setLong(1, id); else statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Tài khoản không được phép thực hiện thao tác này.");
                LibraryStaff6 staff = new LibraryStaff6();
                staff.setId(result.getLong("id")); staff.setFullName(result.getString("fullName"));
                staff.setRole(result.getString("role")); staff.setActive(true);
                return staff;
            }
        }
    }
}

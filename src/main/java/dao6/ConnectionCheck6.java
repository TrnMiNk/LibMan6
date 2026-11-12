package dao6;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ConnectionCheck6 {

    public static void main(String[] args) throws Exception {
        DAO6 dao = new DAO6() {};

        String sql = """
                SELECT COUNT(*) AS tableCount
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                """;

        try (Connection connection = dao.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("Kết nối MySQL thành công!");
            System.out.println(
                    "Database: " + connection.getCatalog()
            );

            if (result.next()) {
                System.out.println(
                        "Số bảng: " + result.getInt("tableCount")
                );
            }
        }
    }
}
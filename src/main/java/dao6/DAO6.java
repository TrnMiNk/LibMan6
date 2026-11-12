package dao6;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public abstract class DAO6 {

    protected final DataSource dataSource;

    protected DAO6() {
        Properties properties = new Properties();

        try (InputStream input = DAO6.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "Không tìm thấy db.properties trong src/main/resources."
                );
            }

            properties.load(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không đọc được cấu hình kết nối CSDL.", e
            );
        }

        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");

        if (url == null || url.isBlank()
                || user == null || user.isBlank()) {
            throw new IllegalStateException(
                    "Cần cấu hình db.url và db.user trong db.properties."
            );
        }

        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setURL(url);
        mysqlDataSource.setUser(user);
        mysqlDataSource.setPassword(
                properties.getProperty("db.password", "")
        );

        this.dataSource = mysqlDataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
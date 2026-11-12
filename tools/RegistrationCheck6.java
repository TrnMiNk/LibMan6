import dao6.*;
import model6.*;
import org.apache.catalina.startup.Tomcat;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Kiểm tra tích hợp opt-in, chỉ tạo/xóa dữ liệu thử của chính lần chạy. */
public class RegistrationCheck6 {
    private static final DAO6 DB = new DAO6() {};
    private static final String PREFIX = "test6_" + UUID.randomUUID().toString().replace("-", "");
    private static final List<Long> ACCOUNTS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Path config = Path.of("target/LibMan6/WEB-INF/classes/registration.properties");
        byte[] original = Files.readAllBytes(config);
        Tomcat tomcat = new Tomcat();
        try {
            long rollbackAccount = seed(PREFIX + "_rollback");
            Reader6 rollbackReader = reader("rollback");
            LibraryCardDAO6 failingDAO = new LibraryCardDAO6() {
                @Override public Connection getConnection() throws SQLException {
                    Connection real = super.getConnection();
                    return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),
                            new Class<?>[]{Connection.class}, (proxy, method, values) -> {
                        if (method.getName().equals("prepareStatement") && values[0] instanceof String
                                && ((String) values[0]).startsWith("INSERT INTO LibraryCard6")) {
                            throw new SQLException("Intentional card insert failure for rollback test");
                        }
                        try { return method.invoke(real, values); }
                        catch (InvocationTargetException exception) { throw exception.getCause(); }
                    });
                }
            };
            try { failingDAO.registerCard(rollbackReader, rollbackAccount); throw new AssertionError("Expected SQL failure"); }
            catch (SQLException expected) { check(countReaders() == 0, "Rollback left a Reader row"); }
            check(linkedReader(rollbackAccount) == 0, "Rollback linked account");
            System.out.println("PASS: real MySQL rollback after Reader INSERT");

            long concurrentAccount = seed(PREFIX + "_concurrent");
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Callable<Boolean> register = () -> {
                    try { new LibraryCardDAO6().registerCard(reader("concurrent"), concurrentAccount); return true; }
                    catch (IllegalStateException expected) { return false; }
                };
                List<Future<Boolean>> results = executor.invokeAll(List.of(register, register));
                int successes = 0;
                for (Future<Boolean> result : results) if (result.get()) successes++;
                check(successes == 1 && countReaders() == 1, "Concurrent registration was not serialized");
            } finally { executor.shutdownNow(); }
            System.out.println("PASS: concurrent registration produces one Reader/Card");

            String username = PREFIX + "_http";
            long httpAccount = seed(username);
            Files.writeString(config, "demo.enabled=true\ndemo.username=" + username + "\n");
            tomcat.setBaseDir(Path.of("target/integration-tomcat6").toAbsolutePath().toString());
            tomcat.setPort(0);
            tomcat.getConnector().setProperty("address", "127.0.0.1");
            tomcat.addWebapp("/LibMan6", Path.of("target/LibMan6").toAbsolutePath().toString());
            tomcat.start();
            String base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/LibMan6";
            HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            check(get(client,base+"/reader6/RegistrationUI6.jsp").statusCode()==302,"Anonymous direct JSP allowed");
            String loginToken=AuthTestSupport6.token(get(client,base+"/auth").body());
            check(AuthTestSupport6.send(client,base+"/auth",Map.of("username",username,"password",AuthTestSupport6.PASSWORD)).statusCode()==403,"Login CSRF not enforced");
            check(AuthTestSupport6.send(client,base+"/auth",Map.of("csrfToken",loginToken,"username","","password","")).body().contains("Vui lòng nhập tên đăng nhập và mật khẩu."),"Missing login inputs");
            AuthTestSupport6.assertDeniedLogin(client,base,username,"wrong-password");
            try(Connection c=DB.getConnection();PreparedStatement s=c.prepareStatement("UPDATE Account6 SET isActive=FALSE WHERE id=?")){s.setLong(1,rollbackAccount);s.executeUpdate();}
            AuthTestSupport6.assertDeniedLogin(client,base,PREFIX+"_rollback",AuthTestSupport6.PASSWORD);
            try(Connection c=DB.getConnection();PreparedStatement s=c.prepareStatement("UPDATE Account6 SET passwordHash='!DEMO_ONLY_LOGIN_DISABLED!' WHERE id=?")){s.setLong(1,rollbackAccount);s.executeUpdate();}
            check(new AccountDAO6().authenticate(PREFIX+"_rollback","!DEMO_ONLY_LOGIN_DISABLED!".toCharArray())==null,"Disabled marker accepted");
            CookieManager cookies=(CookieManager)client.cookieHandler().orElseThrow();
            String beforeSession=cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
            AuthTestSupport6.login(client,base,username);
            String afterSession=cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
            check(!beforeSession.equals(afterSession),"Session ID not rotated");
            check(get(client,base+"/staff6/StaffHomeUI6.jsp").statusCode()==403,"Reader reached staff JSP");
            check(AuthTestSupport6.send(client,base+"/staff6/return",Map.of("action","confirm")).statusCode()==403,"Reader POST reached staff controller");
            check(get(client, base + "/reader6/PortalUI6.jsp").statusCode() == 200, "Portal JSP failed");
            HttpResponse<String> form = get(client, base + "/reader6/register");
            check(form.statusCode() == 200 && form.body().contains("name=\"fullName\""), "Form JSP failed");
            var matcher = java.util.regex.Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(form.body());
            check(matcher.find(), "Missing CSRF token");
            String token = matcher.group(1);
            Map<String, String> data = new LinkedHashMap<>();
            data.put("fullName", PREFIX + " <script>alert(1)</script>");
            data.put("classOrDepartment", "D22 — Thư viện");
            data.put("phone", ""); data.put("email", "");
            check(post(client, base, data).statusCode() == 403, "Missing CSRF accepted");
            check(linkedReader(httpAccount) == 0, "CSRF failure inserted data");
            data.put("csrfToken", token);
            data.put("email", "invalid-email");
            HttpResponse<String> invalid = post(client, base, data);
            check(invalid.statusCode() == 200 && invalid.body().contains("invalid-email")
                    && invalid.body().contains("&lt;script&gt;") && !invalid.body().contains("<script>"), "Validation / escaping failed");
            check(linkedReader(httpAccount) == 0 && countReaders() == 1, "Invalid form inserted data");
            data.put("email", "");
            data.put("accountId", Long.toString(rollbackAccount)); // Phải bị bỏ qua.
            HttpResponse<String> submitted = post(client, base, data);
            check(submitted.statusCode() == 303, "Expected PRG redirect, got " + submitted.statusCode());
            check(linkedReader(httpAccount) != 0 && linkedReader(rollbackAccount) == 0, "Trusted account selection failed");
            LibraryCard6 card = new LibraryCardDAO6().findByAccountId(httpAccount);
            check(card != null && card.getReader().getId() == linkedReader(httpAccount)
                    && "ACTIVE".equals(card.getStatus()) && card.getRegisteredAt() != null, "Wrong saved card");
            String location = submitted.headers().firstValue("Location").orElseThrow();
            for (int i = 0; i < 2; i++) {
                HttpResponse<String> success = get(client, "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + location);
                check(success.statusCode() == 200 && success.body().contains("ACTIVE")
                        && success.body().contains("&lt;script&gt;"), "Success JSP failed");
            }
            check(countReaders() == 2, "Refresh inserted duplicate rows");
            post(client, base, data);
            check(countReaders() == 2, "Duplicate POST inserted rows");
            long secondAccount=seed(PREFIX+"_second");
            HttpClient second=AuthTestSupport6.client();AuthTestSupport6.login(second,base,PREFIX+"_second");
            data.put("csrfToken",AuthTestSupport6.token(get(second,base+"/reader6/register").body()));
            check(post(second,base,data).statusCode()==303 && linkedReader(secondAccount)!=linkedReader(httpAccount) && countReaders()==3,"Two readers did not receive separate profiles/cards");
            check(new LibraryCardDAO6().findByAccountId(secondAccount).getId()!=card.getId(),"Two readers share same card");
            String logoutToken=AuthTestSupport6.token(get(second,base+"/reader6/register").body());
            check(AuthTestSupport6.send(second,base+"/auth",Map.of("action","logout","csrfToken",logoutToken)).statusCode()==303,"Logout failed");
            check(get(second,base+"/reader6/register").statusCode()==302,"Logout retained authorization");
            System.out.println("PASS: login correct/wrong/inactive/marker/CSRF, rotated session, role checks on JSP and POST, two reader accounts/cards, logout");
            System.out.println("PASS: HTTP/JSP UTF-8, CSRF, validation, escaping, optional contacts, trusted account, PRG/refresh, duplicate POST");
        } finally {
            try { tomcat.stop(); tomcat.destroy(); }
            finally { Files.write(config, original); cleanup(); }
        }
        System.out.println("PASS: isolated test data cleaned up");
    }

    private static Reader6 reader(String suffix) { return new Reader6(PREFIX + suffix, "", "", "D22"); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static long seed(String username) throws SQLException {
        try (Connection c = DB.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO Account6(username,passwordHash,accountType,isActive) VALUES (?, ?, 'READER', TRUE)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, username); s.setString(2,PasswordHash6.hash(AuthTestSupport6.PASSWORD.toCharArray())); s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) { r.next(); long id = r.getLong(1); ACCOUNTS.add(id); return id; }
        }
    }
    private static long linkedReader(long id) throws SQLException {
        try (Connection c = DB.getConnection(); PreparedStatement s = c.prepareStatement("SELECT readerId FROM Account6 WHERE id=?")) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }
    private static int countReaders() throws SQLException {
        try (Connection c = DB.getConnection(); PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM Reader6 WHERE fullName LIKE ?")) {
            s.setString(1, PREFIX + "%"); try (ResultSet r = s.executeQuery()) { r.next(); return r.getInt(1); }
        }
    }
    private static void cleanup() throws SQLException {
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (long id : ACCOUNTS) {
                    long readerId = 0;
                    try (PreparedStatement s = c.prepareStatement("SELECT readerId FROM Account6 WHERE id=?")) {
                        s.setLong(1, id); try (ResultSet r = s.executeQuery()) { if (r.next()) readerId = r.getLong(1); }
                    }
                    execute(c, "DELETE FROM Account6 WHERE id=?", id);
                    if (readerId > 0) {
                        execute(c, "DELETE FROM LibraryCard6 WHERE readerId=?", readerId);
                        execute(c, "DELETE FROM Reader6 WHERE id=?", readerId);
                    }
                }
                c.commit();
            } catch (SQLException exception) { c.rollback(); throw exception; }
        }
    }
    private static void execute(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); s.executeUpdate(); }
    }
    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private static HttpResponse<String> post(HttpClient client, String base, Map<String, String> data) throws Exception {
        StringJoiner body = new StringJoiner("&");
        data.forEach((k, v) -> body.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));
        return client.send(HttpRequest.newBuilder(URI.create(base + "/reader6/register"))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

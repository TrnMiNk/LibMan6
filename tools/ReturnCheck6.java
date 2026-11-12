import dao6.*;
import model6.*;
import policy6.*;
import org.apache.catalina.startup.Tomcat;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Opt-in: dữ liệu thử riêng, không chạy seed demo, không thay đổi dữ liệu thật. */
public class ReturnCheck6 {
    private static final DAO6 DB = new DAO6() {};
    private static final String PREFIX = "returntest6_" + UUID.randomUUID().toString().replace("-", "");
    private static final Map<String, List<Long>> CREATED = new LinkedHashMap<>();
    private static final Properties RATES = new Properties();
    private static final ReturnSlipDAO6 DAO;
    static { RATES.setProperty("dailyRate", "2.50"); RATES.setProperty("damageRate", "10.00"); DAO = new ReturnSlipDAO6(new ConfiguredFinePolicy6(RATES)); }
    private static long staffId, staff2Id, cardId, otherCard, timely, overdue, concurrent, foreign;

    public static void main(String[] args) throws Exception {
        Path config = Path.of("target/LibMan6/WEB-INF/classes/staff-demo.properties");
        Path fineConfig = config.resolveSibling("fine-policy.properties");
        byte[] original = Files.readAllBytes(config), originalRates = Files.readAllBytes(fineConfig);
        Tomcat tomcat = new Tomcat();
        try {
            seed();
            check(new LibraryStaffDAO6().findDemoStaff(PREFIX).getId() == staffId, "Staff resolution failed");
            ReturnDetail6 normal = item(timely, 1, 0);
            ConfiguredFinePolicy6 missing = new ConfiguredFinePolicy6(new Properties());
            check(missing.calculateFine(borrow(timely), normal, LocalDate.now()).signum() == 0, "Timely fine must be zero");
            reject(() -> missing.calculateFine(borrow(overdue), item(overdue, 1, 0), LocalDate.now()));
            reject(() -> missing.calculateFine(borrow(timely), item(timely, 1, 1), LocalDate.now()));
            Properties zeros = new Properties(); zeros.setProperty("dailyRate", "0"); zeros.setProperty("damageRate", "0");
            check(new ConfiguredFinePolicy6(zeros).calculateFine(borrow(overdue), item(overdue, 1, 1), LocalDate.now()).signum() == 0, "Explicit zero rejected");
            Properties negative = new Properties(); negative.setProperty("damageRate", "-1");
            reject(() -> new ConfiguredFinePolicy6(negative).calculateFine(borrow(timely), item(timely,1,1), LocalDate.now()));
            Properties overflow = new Properties(); overflow.setProperty("dailyRate", "9999999999999.99");
            reject(() -> new ConfiguredFinePolicy6(overflow).calculateFine(borrow(overdue), item(overdue,1,0), LocalDate.now()));
            reject(() -> DAO.previewReturn(cardId, List.of()));
            reject(() -> DAO.previewReturn(cardId, List.of(item(foreign,1,0))));
            reject(() -> DAO.previewReturn(cardId, List.of(item(timely,4,0))));
            reject(() -> DAO.previewReturn(cardId, List.of(item(timely,1,2))));
            reject(() -> DAO.previewReturn(cardId, List.of(item(timely,1,0),item(timely,1,0))));
            ReturnDetail6 longNote = item(timely,1,0); longNote.setConditionNote("x".repeat(301));
            reject(() -> DAO.previewReturn(cardId, List.of(longNote)));
            ReturnSlip6 first = preview(List.of(item(timely,1,0),item(overdue,1,1)), staffId);
            ReturnSlip6 stale = preview(List.of(item(timely,1,0)), staffId);
            check(first.getTotalFine().toPlainString().equals("15.00") && slipCount()==0, "Preview wrote data or wrong fine");
            System.out.println("PASS: preview is read-only; policy configuration, money bounds, ownership, quantities, duplicate lines, note length");

            update("UPDATE Account6 SET isActive=FALSE WHERE staffId=?", staffId);
            reject(() -> DAO.confirmReturnTransaction(first,staffId));
            update("UPDATE Account6 SET isActive=TRUE WHERE staffId=?", staffId);
            ReturnSlipDAO6 failing = new ReturnSlipDAO6(new ConfiguredFinePolicy6(RATES)) {
                @Override public Connection getConnection() throws SQLException {
                    Connection real = super.getConnection(); int[] inserts = {0};
                    return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, values) -> {
                        if (method.getName().equals("prepareStatement") && values[0] instanceof String
                                && ((String)values[0]).startsWith("INSERT INTO ReturnDetail6") && ++inserts[0]==2) throw new SQLException("Intentional second detail failure");
                        try { return method.invoke(real,values); } catch (InvocationTargetException exception) { throw exception.getCause(); }
                    });
                }
            };
            try { failing.confirmReturnTransaction(first,staffId); throw new AssertionError("Expected SQL failure"); }
            catch (SQLException expected) { check(slipCount()==0 && returned(timely)==0 && returned(overdue)==0,"Rollback left partial records"); }
            ReturnSlip6 saved = DAO.confirmReturnTransaction(first,staffId);
            check(saved.getId()>0 && returned(timely)==1 && returned(overdue)==1 && borrow(timely).getRemainingQuantity()==2, "Partial return failed");
            check(DAO.confirmReturnTransaction(first,staffId).getId()==saved.getId() && slipCount()==1,"Request key inserted duplicate");
            reject(() -> DAO.confirmReturnTransaction(stale,staffId));
            ReturnSlip6 wrongContext = preview(List.of(item(concurrent,1,0)),staff2Id);
            wrongContext.setRequestKey(first.getRequestKey());
            reject(() -> DAO.confirmReturnTransaction(wrongContext,staff2Id));
            System.out.println("PASS: inactive staff blocked; SQL failure rolls back slip and all details; partial return, stale preview, idempotency and request-key context");

            ReturnSlip6 a = preview(List.of(item(concurrent,2,0)),staffId);
            ReturnSlip6 b = preview(List.of(item(concurrent,2,0)),staff2Id);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch gate = new CountDownLatch(1);
            try {
                Future<Boolean> f1 = executor.submit(() -> { gate.await(); return confirm(a,staffId); });
                Future<Boolean> f2 = executor.submit(() -> { gate.await(); return confirm(b,staff2Id); });
                gate.countDown();
                check((f1.get()?1:0)+(f2.get()?1:0)==1 && returned(concurrent)==2,"Concurrent requests exceeded borrowed quantity");
            } finally { executor.shutdownNow(); }
            for(int i=0;i<2;i++) check(DAO.getSavedReceipt(saved.getId()).getId()==saved.getId(),"Receipt read failed");
            check(slipCount()==2,"Reprint inserted slip");
            System.out.println("PASS: two staff concurrent returns cannot over-return; reading/reprinting same receipt is read-only");

            Files.writeString(config,"demo.enabled=true\ndemo.username="+PREFIX+"\n");
            Files.writeString(fineConfig,"dailyRate=2.50\ndamageRate=10.00\n");
            tomcat.setBaseDir(Path.of("target/return-tomcat6").toAbsolutePath().toString()); tomcat.setPort(0);
            tomcat.getConnector().setProperty("address","127.0.0.1");
            tomcat.addWebapp("/LibMan6",Path.of("target/LibMan6").toAbsolutePath().toString()); tomcat.start();
            String origin="http://127.0.0.1:"+tomcat.getConnector().getLocalPort(), base=origin+"/LibMan6";
            HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();
            check(get(client,base+"/staff6/ReturnReceiptUI6.jsp").statusCode()==302,"Anonymous staff JSP allowed");
            update("UPDATE LibraryStaff6 SET isActive=FALSE WHERE id=?",staffId);
            AuthTestSupport6.assertDeniedLogin(client,base,PREFIX,AuthTestSupport6.PASSWORD);
            update("UPDATE LibraryStaff6 SET isActive=TRUE,role='MANAGER' WHERE id=?",staffId);
            AuthTestSupport6.login(client,base,PREFIX);
            check(get(client,base+"/reader6/register").statusCode()==403,"Staff reached reader registration");
            check(get(client,base+"/staff6/StaffHomeUI6.jsp").statusCode()==200,"Staff home JSP failed");
            HttpResponse<String> form=get(client,base+"/staff6/return?cardId="+cardId);
            check(form.statusCode()==200 && form.body().contains("name=\"q"+timely+"\""),"Return search JSP failed");
            String token=extract(form.body(),"name=\"csrfToken\" value=\"([^\"]+)\"");
            Map<String,String> data=new LinkedHashMap<>(); data.put("action","preview"); data.put("cardId",Long.toString(cardId)); data.put("selected",Long.toString(timely));
            data.put("q"+timely,"1"); data.put("d"+timely,"0"); data.put("n"+timely,"<script>thử UTF-8</script>");
            check(post(client,base,data).statusCode()==403,"CSRF accepted");
            Map<String,String> paidPreview=new LinkedHashMap<>(data);
            paidPreview.put("csrfToken",token); paidPreview.put("selected",Long.toString(overdue));
            paidPreview.put("q"+overdue,"1"); paidPreview.put("d"+overdue,"0");
            HttpResponse<String> withFine=post(client,base,paidPreview);
            check(withFine.body().contains("class=\"radio-option\"") && withFine.body().contains("type=\"radio\"")
                    && withFine.body().contains("<span>Chưa thanh toán</span>"),"Payment radio markup failed");
            checkVietnamese(withFine);
            data.put("csrfToken",token); data.put("q"+timely,"999");
            HttpResponse<String> bad=post(client,base,data);
            check(bad.body().contains("role=\"alert\"") && bad.body().contains("value=\"999\""),"Invalid quantities not preserved");
            data.put("q"+timely,"1");
            HttpResponse<String> view=post(client,base,data);
            check(view.statusCode()==200 && view.body().contains("&lt;script&gt;") && view.body().contains("thử UTF-8") && slipCount()==2,"Preview JSP or read-only failed");
            checkVietnamese(view);
            check(view.body().contains("Không phát sinh phạt") && !view.body().contains("type=\"radio\""),"Zero fine payment display failed");
            String key=extract(view.body(),"name=\"requestKey\" value=\"([^\"]+)\"");
            check(get(client,base+"/staff6/return?action=edit").body().contains("&lt;script&gt;"),"Back/edit did not preserve input");
            Map<String,String> command=new LinkedHashMap<>();command.put("action","confirm");command.put("csrfToken",token);command.put("requestKey",key);command.put("fineSettled","true");
            command.put("staffId",Long.toString(staff2Id)); command.put("returnedQuantity","999"); command.put("fineAmount","999");
            HttpResponse<String> confirmation=post(client,base,command);
            check(confirmation.statusCode()==303,"Confirm did not redirect");
            String receipt=confirmation.headers().firstValue("Location").orElseThrow();
            long receiptId=Long.parseLong(receipt.substring(receipt.lastIndexOf('=')+1));
            long detailCount=scalar("SELECT COUNT(*) FROM ReturnDetail6 rd JOIN ReturnSlip6 rs ON rs.id=rd.returnSlipId WHERE rs.libraryCardId=?",cardId);
            for(int i=0;i<2;i++) {
                HttpResponse<String> page=get(client,origin+receipt);
                check(page.statusCode()==200 && page.body().contains("window.print()") && page.body().contains("&lt;script&gt;"),"Receipt JSP failed");
                checkVietnamese(page);
                check(page.uri().toString().equals(origin+receipt) && page.body().contains("Phiếu trả tài liệu #"+receiptId),"Receipt slipId changed");
                check(page.body().contains("Không phát sinh phạt") && !page.body().contains("Chưa thanh toán"),"Zero-fine receipt display failed");
                String printRegion=extract(page.body(),"<article id=\"receipt-content\">([\\s\\S]*?)</article>");
                check(!printRegion.contains("window.print") && !printRegion.contains("class=\"success\"")
                        && !printRegion.contains("class=\"notice\"") && !printRegion.contains("triển khai")
                        && !printRegion.contains("<button") && !printRegion.contains("<a "),"Print region contains controls or technical guidance");
            }
            check(slipCount()==3 && scalar("SELECT COUNT(*) FROM ReturnDetail6 rd JOIN ReturnSlip6 rs ON rs.id=rd.returnSlipId WHERE rs.libraryCardId=?",cardId)==detailCount,"Receipt GET inserted slip/detail");
            String css=get(client,base+"/assets/css/style6.css").body();
            check(css.contains("input:not([type=radio]):not([type=checkbox])") && css.contains(".payment-options .radio-option { display: inline-flex")
                    && css.contains("body #receipt-content, body #receipt-content * { visibility: visible; }"),"Radio/print CSS missing");
            check(post(client,base,command).statusCode()==303 && slipCount()==3 && returned(timely)==2,"Duplicate confirmation or receipt refresh inserted data");
            check(get(client,base+"/staff6/ReturnReceiptUI6.jsp").statusCode()==302,"Direct JSP did not redirect");
            System.out.println("PASS: HTTP/JSP search, CSRF, preserved errors, UTF-8/escaping, preview/edit, trusted draft/staff, PRG, duplicate POST, receipt/reprint GET, direct-JSP guard");
            System.out.println("PASS: UTF-8 include labels and response charset; zero/nonzero fine presentation; radio markup/CSS; isolated print region; same slipId and DB counts on repeat GET");
        } finally {
            try { tomcat.stop(); tomcat.destroy(); }
            finally { Files.write(config,original); Files.write(fineConfig,originalRates); cleanup(); }
        }
        System.out.println("PASS: test data cleaned up; source config and WAR unchanged");
    }

    private static ReturnDetail6 item(long id,int qty,int damaged) { BorrowDetail6 b=new BorrowDetail6();b.setId(id);ReturnDetail6 d=new ReturnDetail6();d.setBorrowDetail(b);d.setReturnedQuantity(qty);d.setDamagedQuantity(damaged);d.setConditionNote("");return d; }
    private static ReturnSlip6 preview(List<ReturnDetail6> items,long id) throws Exception {ReturnSlip6 slip=DAO.previewReturn(cardId,items);LibraryStaff6 staff=new LibraryStaff6();staff.setId(id);slip.setStaff(staff);return slip;}
    private static boolean confirm(ReturnSlip6 slip,long id) throws Exception {try {DAO.confirmReturnTransaction(slip,id);return true;}catch(IllegalArgumentException expected){return false;}}
    private static BorrowDetail6 borrow(long id) throws Exception {return new BorrowDetailDAO6().getBorrowedItems(cardId).stream().filter(b->b.getId()==id).findFirst().orElseThrow();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @FunctionalInterface interface Attempt {void run() throws Exception;}
    private static void reject(Attempt attempt) throws Exception {try{attempt.run();throw new AssertionError("Invalid operation accepted");}catch(IllegalArgumentException expected){}}
    private static long insert(String table,String sql,Object...args)throws SQLException {
        try(Connection c=DB.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(s,args);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();long id=r.getLong(1);CREATED.computeIfAbsent(table,k->new ArrayList<>()).add(id);return id;}}
    }
    private static void bind(PreparedStatement s,Object...args)throws SQLException{for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);}
    private static void update(String sql,Object...args)throws SQLException{try(Connection c=DB.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,args);s.executeUpdate();}}
    private static long scalar(String sql,Object...args)throws SQLException{try(Connection c=DB.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,args);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private static long slipCount()throws SQLException{return scalar("SELECT COUNT(*) FROM ReturnSlip6 WHERE libraryCardId=?",cardId);}
    private static long returned(long id)throws SQLException{return scalar("SELECT COALESCE(SUM(returnedQuantity),0) FROM ReturnDetail6 WHERE borrowDetailId=?",id);}
    private static long makeStaff(String username)throws SQLException{
        long staff=insert("LibraryStaff6","INSERT INTO LibraryStaff6(fullName,role,isActive) VALUES (?,'LIBRARIAN',TRUE)",username);
        insert("Account6","INSERT INTO Account6(username,passwordHash,accountType,isActive,staffId) VALUES (?,?,'STAFF',TRUE,?)",username,PasswordHash6.hash(AuthTestSupport6.PASSWORD.toCharArray()),staff);return staff;
    }
    private static long makeCard(String name)throws SQLException{
        long reader=insert("Reader6","INSERT INTO Reader6(fullName,classOrDepartment) VALUES (?,'Test only')",name);
        return insert("LibraryCard6","INSERT INTO LibraryCard6(status,registeredAt,readerId) VALUES ('ACTIVE',NOW(),?)",reader);
    }
    private static long makeBorrow(long record,int qty,int days)throws SQLException{
        long doc=insert("Document6","INSERT INTO Document6(title,category,author) VALUES (?,'TEST','Test')",PREFIX+UUID.randomUUID());
        return insert("BorrowDetail6","INSERT INTO BorrowDetail6(borrowQuantity,dueDate,borrowRecordId,documentId) VALUES (?,?,?,?)",qty,java.sql.Date.valueOf(LocalDate.now().plusDays(days)),record,doc);
    }
    private static void seed()throws SQLException{
        staffId=makeStaff(PREFIX);staff2Id=makeStaff(PREFIX+"b");cardId=makeCard(PREFIX);otherCard=makeCard(PREFIX+"b");
        long record=insert("BorrowRecord6","INSERT INTO BorrowRecord6(borrowDate,libraryCardId) VALUES (CURDATE(),?)",cardId);
        timely=makeBorrow(record,3,14);overdue=makeBorrow(record,4,-2);concurrent=makeBorrow(record,2,14);
        long other=insert("BorrowRecord6","INSERT INTO BorrowRecord6(borrowDate,libraryCardId) VALUES (CURDATE(),?)",otherCard);foreign=makeBorrow(other,2,14);
    }
    private static void cleanup()throws SQLException{
        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            try{
                for(long card:CREATED.getOrDefault("LibraryCard6",List.of())){
                    try(PreparedStatement s=c.prepareStatement("DELETE rd FROM ReturnDetail6 rd JOIN ReturnSlip6 rs ON rs.id=rd.returnSlipId WHERE rs.libraryCardId=?")){s.setLong(1,card);s.executeUpdate();}
                    try(PreparedStatement s=c.prepareStatement("DELETE FROM ReturnSlip6 WHERE libraryCardId=?")){s.setLong(1,card);s.executeUpdate();}
                }
                for(String table:List.of("Account6","BorrowDetail6","BorrowRecord6","Document6","LibraryCard6","Reader6","LibraryStaff6"))
                    for(long id:CREATED.getOrDefault(table,List.of()))try(PreparedStatement s=c.prepareStatement("DELETE FROM "+table+" WHERE id=?")){s.setLong(1,id);s.executeUpdate();}
                c.commit();
            }catch(SQLException e){c.rollback();throw e;}
        }
    }
    private static String extract(String text,String regex){var m=java.util.regex.Pattern.compile(regex).matcher(text);check(m.find(),"Missing HTML field: "+regex);return m.group(1);}
    private static void checkVietnamese(HttpResponse<String> response){
        check(response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("charset=utf-8"),"Wrong response charset");
        for(String label:List.of("Tài liệu","Số lượng trả","Số lượng hỏng","Ghi chú","Tiền phạt","Chi tiết mượn","Tổng phạt"))
            check(response.body().contains(label),"Corrupted UTF-8 include label: "+label);
    }
    private static HttpResponse<String> get(HttpClient c,String url)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}
    private static HttpResponse<String> post(HttpClient c,String base,Map<String,String> data)throws Exception{
        StringJoiner body=new StringJoiner("&");data.forEach((k,v)->body.add(URLEncoder.encode(k,StandardCharsets.UTF_8)+"="+URLEncoder.encode(v,StandardCharsets.UTF_8)));
        return c.send(HttpRequest.newBuilder(URI.create(base+"/staff6/return")).header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

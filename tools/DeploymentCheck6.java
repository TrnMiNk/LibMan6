import dao6.*;
import model6.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Kiểm tra sau deploy; giữ lại tối đa một phiếu mẫu thuộc bộ setup riêng. */
public class DeploymentCheck6 {
    static final DAO6 DB=new DAO6(){};
    static final String BASE="http://localhost:8080/LibMan6";
    public static void main(String[] args)throws Exception{
        HttpClient reader=AuthTestSupport6.client();
        check(AuthTestSupport6.get(reader,BASE+"/").headers().firstValue("Location").orElse("").endsWith("/auth"),"Root route failed");
        check(AuthTestSupport6.get(reader,BASE+"/staff6/StaffHomeUI6.jsp").statusCode()==302,"Anonymous staff JSP allowed");
        for(String username:List.of("ban_doc01","ban_doc02")){
            reader=AuthTestSupport6.client();login(reader,username);
            var form=AuthTestSupport6.get(reader,BASE+"/reader6/register");
            check(form.statusCode()==200 && form.body().contains("name=\"fullName\"") && !form.body().contains("role=\"alert\""),"Reader form unavailable");
            check(AuthTestSupport6.get(reader,BASE+"/staff6/return").statusCode()==403,"Reader reached staff controller");
            check(AuthTestSupport6.get(reader,BASE+"/staff6;check/StaffHomeUI6.jsp").statusCode()==403,"Normalized path role bypass");
            cleanUi(form.body());
        }
        long card=scalar("SELECT c.id FROM Account6 a JOIN LibraryCard6 c ON c.readerId=a.readerId WHERE a.username='ban_doc_muon'");
        HttpClient staff=AuthTestSupport6.client();login(staff,"thu_thu01");
        var search=AuthTestSupport6.get(staff,BASE+"/staff6/return?cardId="+card);
        check(search.statusCode()==200 && search.body().contains("Trần Minh Anh"),"Staff lookup failed");cleanUi(search.body());
        long slip=scalar("SELECT COALESCE(MAX(rs.id),0) FROM ReturnSlip6 rs JOIN Account6 a ON a.staffId=rs.staffId WHERE rs.libraryCardId=? AND a.username='thu_thu01'",card);
        if(slip==0){
            List<BorrowDetail6> borrowed=new BorrowDetailDAO6().getBorrowedItems(card);
            check(borrowed.size()>=2,"Missing sample loans");
            List<Map.Entry<String,String>> values=new ArrayList<>();
            values.add(Map.entry("action","preview"));values.add(Map.entry("csrfToken",AuthTestSupport6.token(search.body())));values.add(Map.entry("cardId",Long.toString(card)));
            for(BorrowDetail6 b:borrowed){
                values.add(Map.entry("selected",Long.toString(b.getId())));values.add(Map.entry("q"+b.getId(),"1"));
                values.add(Map.entry("d"+b.getId(),b.getDueDate().isBefore(java.time.LocalDate.now())?"1":"0"));
                values.add(Map.entry("n"+b.getId(),b.getDueDate().isBefore(java.time.LocalDate.now())?"Bìa bị rách":"Tài liệu còn tốt"));
            }
            long before=scalar("SELECT COUNT(*) FROM ReturnSlip6 WHERE libraryCardId=?",card);
            var preview=post(staff,values);check(preview.statusCode()==200 && preview.body().contains("Xem trước phiếu trả"),"Preview failed");
            check(scalar("SELECT COUNT(*) FROM ReturnSlip6 WHERE libraryCardId=?",card)==before,"Preview wrote DB");cleanUi(preview.body());
            String key=extract(preview.body(),"name=\"requestKey\" value=\"([^\"]+)\"");
            Map<String,String> confirmation=Map.of("action","confirm","csrfToken",AuthTestSupport6.token(preview.body()),"requestKey",key,"fineSettled","false");
            var saved=AuthTestSupport6.send(staff,BASE+"/staff6/return",confirmation);
            check(saved.statusCode()==303,"Confirm failed");String location=saved.headers().firstValue("Location").orElseThrow();
            slip=Long.parseLong(location.substring(location.lastIndexOf('=')+1));
            check(AuthTestSupport6.send(staff,BASE+"/staff6/return",confirmation).headers().firstValue("Location").orElse("").equals(location),"Repeated POST changed slip");
        }
        long slipsBefore=scalar("SELECT COUNT(*) FROM ReturnSlip6"),detailsBefore=scalar("SELECT COUNT(*) FROM ReturnDetail6");
        for(int i=0;i<2;i++){
            var receipt=AuthTestSupport6.get(staff,BASE+"/staff6/return?action=receipt&slipId="+slip);
            check(receipt.statusCode()==200 && receipt.body().contains("Phiếu trả tài liệu #"+slip),"Receipt ID changed");
            for(String label:List.of("Tài liệu","Số lượng trả","Số lượng hỏng","Ghi chú","Tiền phạt","Chi tiết mượn","Tổng phạt"))check(receipt.body().contains(label),"Corrupt UTF-8 label");
            cleanUi(receipt.body());
            String print=extract(receipt.body(),"<article id=\"receipt-content\">([\\s\\S]*?)</article>");
            check(!print.contains("<nav") && !print.contains("<button") && !print.contains("class=\"success\""),"Print controls leaked");
        }
        check(scalar("SELECT COUNT(*) FROM ReturnSlip6")==slipsBefore && scalar("SELECT COUNT(*) FROM ReturnDetail6")==detailsBefore,"Receipt GET wrote DB");
        ReturnSlip6 saved=new ReturnSlipDAO6().getSavedReceipt(slip);check(saved.getTotalFine().signum()>0,"Missing overdue/damage fine");
        String logoutToken=AuthTestSupport6.token(AuthTestSupport6.get(staff,BASE+"/staff6/return").body());
        check(AuthTestSupport6.send(staff,BASE+"/auth",Map.of("action","logout","csrfToken",logoutToken)).statusCode()==303,"Logout failed");
        check(AuthTestSupport6.get(staff,BASE+"/staff6/return").statusCode()==302,"Logout retained rights");
        System.out.println("PASS deployed: root/login, two reader forms, role/normalized-path protection, staff lookup, preview read-only, confirm/replay, UTF-8/clean UI, print region, repeated GET unchanged, logout");
        System.out.println("Kept sample cardId="+card+" slipId="+slip+" totalFine="+saved.getTotalFine().toPlainString());
    }
    static void login(HttpClient client,String username)throws Exception{
        String password=System.getenv("LIBMAN6_DEMO_PASSWORD");
        if(password==null || password.isEmpty())throw new IllegalStateException("Set LIBMAN6_DEMO_PASSWORD to the local sample account password before checking deployment.");
        String token=AuthTestSupport6.token(AuthTestSupport6.get(client,BASE+"/auth").body());
        check(AuthTestSupport6.send(client,BASE+"/auth",Map.of("username",username,"password",password,"csrfToken",token)).statusCode()==303,"Deployed login failed");
    }
    static void cleanUi(String html){
        String text=html.replaceAll("(?s)<[^>]*>"," ");
        for(String forbidden:List.of("[DEMO RETURN6]","demo","xác thực","transaction","commit","requestKey","properties","triển khai","SQLException","CSDL"))check(!text.contains(forbidden),"Technical UI text: "+forbidden);
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static String extract(String text,String regex){var m=java.util.regex.Pattern.compile(regex).matcher(text);check(m.find(),"HTML field missing");return m.group(1);}
    static long scalar(String sql,Object...args)throws SQLException{try(Connection c=DB.getConnection();PreparedStatement s=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}}
    static HttpResponse<String> post(HttpClient client,List<Map.Entry<String,String>> data)throws Exception{
        StringJoiner body=new StringJoiner("&");data.forEach(e->body.add(URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)));
        return client.send(HttpRequest.newBuilder(URI.create(BASE+"/staff6/return")).header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

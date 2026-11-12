import dao6.*;
import java.sql.*;
import java.util.Arrays;

/** Chỉ thêm bộ tài khoản/dữ liệu riêng cho demo local; không ghi đè tài khoản có sẵn. */
public class DemoSetup6 {
    private static final DAO6 DB = new DAO6() {};
    private static final char[] PASSWORD = demoPassword();
    private static char[] demoPassword() {
        String value = System.getenv("LIBMAN6_DEMO_PASSWORD");
        if (value == null || value.length() < 12) throw new IllegalStateException("Set LIBMAN6_DEMO_PASSWORD with at least 12 characters before running setup.");
        return value.toCharArray();
    }
    public static void main(String[] args) throws Exception {
        try (Connection c=DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                reader(c,"ban_doc01"); reader(c,"ban_doc02");
                if (account(c,"thu_thu01")==0) {
                    long staff=insert(c,"INSERT INTO LibraryStaff6(fullName,role,isActive) VALUES (?,'LIBRARIAN',TRUE)","Nguyễn Thu Hà");
                    insert(c,"INSERT INTO Account6(username,passwordHash,accountType,isActive,staffId) VALUES (?,?,'STAFF',TRUE,?)","thu_thu01",PasswordHash6.hash(PASSWORD),staff);
                    System.out.println("Created: thu_thu01");
                } else System.out.println("Preserved existing: thu_thu01");
                if (account(c,"ban_doc_muon")==0) {
                    long reader=insert(c,"INSERT INTO Reader6(fullName,classOrDepartment) VALUES (?,?)","Trần Minh Anh","D22CN01");
                    insert(c,"INSERT INTO Account6(username,passwordHash,accountType,isActive,readerId) VALUES (?,?,'READER',TRUE,?)","ban_doc_muon",PasswordHash6.hash(PASSWORD),reader);
                    long card=insert(c,"INSERT INTO LibraryCard6(status,registeredAt,readerId) VALUES ('ACTIVE',NOW(),?)",reader);
                    long record=insert(c,"INSERT INTO BorrowRecord6(borrowDate,libraryCardId) VALUES (CURDATE()-INTERVAL 10 DAY,?)",card);
                    long doc1=insert(c,"INSERT INTO Document6(title,category,author) VALUES (?,?,?)","Phân tích và thiết kế hệ thống thông tin","Giáo trình","Bộ môn Công nghệ thông tin");
                    long doc2=insert(c,"INSERT INTO Document6(title,category,author) VALUES (?,?,?)","Cơ sở dữ liệu","Giáo trình","Bộ môn Công nghệ thông tin");
                    insert(c,"INSERT INTO BorrowDetail6(borrowQuantity,dueDate,borrowRecordId,documentId) VALUES (5,CURDATE()+INTERVAL 14 DAY,?,?)",record,doc1);
                    insert(c,"INSERT INTO BorrowDetail6(borrowQuantity,dueDate,borrowRecordId,documentId) VALUES (4,CURDATE()-INTERVAL 3 DAY,?,?)",record,doc2);
                    System.out.println("Created: ban_doc_muon; cardId="+card);
                } else System.out.println("Preserved existing: ban_doc_muon");
                c.commit();
                try(PreparedStatement s=c.prepareStatement("SELECT c.id FROM Account6 a JOIN LibraryCard6 c ON c.readerId=a.readerId WHERE a.username='ban_doc_muon'");ResultSet r=s.executeQuery()) {
                    if(r.next())System.out.println("Demo return cardId="+r.getLong(1));
                }
            } catch(Exception e){c.rollback();throw e;}
        } finally { Arrays.fill(PASSWORD,'\0'); }
    }
    private static void reader(Connection c,String username)throws SQLException{
        if(account(c,username)!=0){System.out.println("Preserved existing: "+username);return;}
        insert(c,"INSERT INTO Account6(username,passwordHash,accountType,isActive) VALUES (?,?,'READER',TRUE)",username,PasswordHash6.hash(PASSWORD));
        System.out.println("Created reader without card: "+username);
    }
    private static long account(Connection c,String username)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("SELECT id FROM Account6 WHERE username=?")){s.setString(1,username);try(ResultSet r=s.executeQuery()){return r.next()?r.getLong(1):0;}}
    }
    private static long insert(Connection c,String sql,Object...args)throws SQLException{
        try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Missing key");return r.getLong(1);}}
    }
}

package controller6;

import dao6.AccountDAO6;
import model6.Account6;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/auth")
public class AuthController6 extends HttpServlet {
    private final AccountDAO6 accounts = new AccountDAO6();
    public static final String GENERIC_ERROR = "Không thể xử lý yêu cầu. Vui lòng thử lại.";

    public static void prepare(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=UTF-8"); response.setHeader("Cache-Control","no-store");
        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute("csrfToken6") == null) session.setAttribute("csrfToken6", UUID.randomUUID().toString());
        }
    }
    public static String home(Account6 account) { return "STAFF".equals(account.getAccountType()) ? "/staff6/StaffHomeUI6.jsp" : "/reader6/PortalUI6.jsp"; }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        prepare(request,response);
        Object id = request.getSession().getAttribute("authenticatedAccountId6");
        try {
            Account6 account = id instanceof Long ? accounts.findActiveAccount((Long)id) : null;
            if (account != null) { request.getSession().setAttribute("account6",account); response.sendRedirect(request.getContextPath()+home(account)); return; }
        } catch (SQLException exception) { log("Read login account failed",exception); request.setAttribute("error",GENERIC_ERROR); }
        show(request,response);
    }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); prepare(request,response);
        if (!request.getSession().getAttribute("csrfToken6").equals(request.getParameter("csrfToken"))) {
            response.setStatus(403); request.setAttribute("error",GENERIC_ERROR); show(request,response); return;
        }
        if ("logout".equals(request.getParameter("action"))) {
            request.getSession().invalidate(); response.setStatus(303); response.setHeader("Location",request.getContextPath()+"/auth"); return;
        }
        String username = Objects.toString(request.getParameter("username"),"").strip();
        char[] password = Objects.toString(request.getParameter("password"),"").toCharArray();
        request.setAttribute("username",username);
        try {
            if (username.isBlank() || password.length == 0) request.setAttribute("error","Vui lòng nhập tên đăng nhập và mật khẩu.");
            else {
                Account6 account = username.length() <= 80 && password.length <= 1024 ? accounts.authenticate(username,password) : null;
                if (account == null) request.setAttribute("error","Tên đăng nhập hoặc mật khẩu không đúng.");
                else {
                    HttpSession session = request.getSession();
                    // Xóa bản xem trước/dữ liệu của danh tính trước, rồi đổi ID phiên.
                    for (String key : Collections.list(session.getAttributeNames())) session.removeAttribute(key);
                    request.changeSessionId(); session.setMaxInactiveInterval(30*60);
                    session.setAttribute("authenticatedAccountId6",account.getId()); session.setAttribute("account6",account);
                    session.setAttribute("csrfToken6",UUID.randomUUID().toString());
                    response.setStatus(303); response.setHeader("Location",request.getContextPath()+home(account)); return;
                }
            }
        } catch (SQLException exception) { log("Login database failure",exception); request.setAttribute("error",GENERIC_ERROR); }
        finally { Arrays.fill(password,'\0'); }
        show(request,response);
    }
    private void show(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        request.setAttribute("loginView6",true); request.getRequestDispatcher("/LoginUI6.jsp").forward(request,response);
    }
}

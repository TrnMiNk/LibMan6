package controller6;

import dao6.AccountDAO6;
import model6.Account6;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebFilter(urlPatterns="/*", dispatcherTypes={DispatcherType.REQUEST,DispatcherType.FORWARD})
public class AccessFilter6 implements Filter {
    private final AccountDAO6 accounts = new AccountDAO6();
    @Override public void doFilter(ServletRequest input,ServletResponse output,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest request=(HttpServletRequest)input; HttpServletResponse response=(HttpServletResponse)output;
        request.setCharacterEncoding("UTF-8"); response.setCharacterEncoding("UTF-8");
        String path=request.getServletPath();
        String required=path.startsWith("/staff6/") ? "STAFF" : path.startsWith("/reader6/") ? "READER" : null;
        if (required != null) {
            response.setHeader("Cache-Control","no-store");
            HttpSession session=request.getSession(false);
            Object id=session==null ? null : session.getAttribute("authenticatedAccountId6");
            try {
                Account6 account=id instanceof Long ? accounts.findActiveAccount((Long)id) : null;
                if(account==null) {
                    if(session!=null)session.invalidate();
                    if("GET".equals(request.getMethod()))response.sendRedirect(request.getContextPath()+"/auth");
                    else { response.setStatus(401); response.setContentType("text/html;charset=UTF-8"); response.getWriter().write("Vui lòng đăng nhập để tiếp tục."); }
                    return;
                }
                if(!required.equals(account.getAccountType())) {
                    response.setStatus(403); request.setAttribute("error","Bạn không có quyền thực hiện thao tác này.");
                    request.getRequestDispatcher("/ErrorUI6.jsp").forward(request,response); return;
                }
                session.setAttribute("account6",account);
            } catch(SQLException exception) {
                request.getServletContext().log("Access account lookup failed",exception);
                response.setStatus(503); request.setAttribute("error",AuthController6.GENERIC_ERROR);
                request.getRequestDispatcher("/ErrorUI6.jsp").forward(request,response); return;
            }
        }
        chain.doFilter(input,output);
    }
}

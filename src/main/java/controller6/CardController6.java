package controller6;

import dao6.LibraryCardDAO6;
import dao6.ReaderDAO6;
import model6.LibraryCard6;
import model6.Reader6;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.time.format.DateTimeFormatter;

@WebServlet("/reader6/register")
public class CardController6 extends HttpServlet {
    private ReaderDAO6 readerDAO;
    private LibraryCardDAO6 cardDAO;

    @Override
    public void init() throws ServletException {
        readerDAO = new ReaderDAO6();
        cardDAO = new LibraryCardDAO6();
    }

    private long accountId(HttpServletRequest request) throws SQLException {
        Object id = request.getSession().getAttribute("authenticatedAccountId6");
        model6.Account6 account = (model6.Account6) request.getSession().getAttribute("account6");
        if (id instanceof Long && account != null && account.getId() == (Long)id && "READER".equals(account.getAccountType())) return (Long)id;
        throw new IllegalStateException("Vui lòng đăng nhập để tiếp tục.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        prepare(request, response);
        try {
            LibraryCard6 card = cardDAO.findByAccountId(accountId(request));
            if (card != null) {
                request.setAttribute("card", card);
                request.setAttribute("registeredAtText", card.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            }
        } catch (IllegalStateException exception) {
            request.setAttribute("error", exception.getMessage());
        } catch (SQLException exception) {
            log("Không đọc được thông tin thẻ thư viện.", exception);
            request.setAttribute("error", AuthController6.GENERIC_ERROR);
        }
        showForm(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        prepare(request, response);
        submitRegistrationInfo(request, response);
    }

    protected void submitRegistrationInfo(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Reader6 reader = new Reader6(request.getParameter("fullName"), request.getParameter("phone"),
                request.getParameter("email"), request.getParameter("classOrDepartment"));
        request.setAttribute("reader", reader);
        String expected = (String) request.getSession().getAttribute("csrfToken6");
        if (!expected.equals(request.getParameter("csrfToken"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", "Phiên làm việc đã hết hạn. Vui lòng tải lại trang.");
        } else if (!readerDAO.checkRegistrationInfo(reader)) {
            request.setAttribute("error", reader.getFullName().isBlank() || reader.getClassOrDepartment().isBlank()
                    ? "Vui lòng nhập đầy đủ thông tin bắt buộc." : "Vui lòng kiểm tra họ tên, lớp/đơn vị, số điện thoại và email.");
        } else {
            try {
                cardDAO.registerCard(reader, accountId(request));
                response.setStatus(HttpServletResponse.SC_SEE_OTHER);
                response.setHeader("Location", request.getContextPath() + "/reader6/register");
                return;
            } catch (IllegalStateException exception) {
                request.setAttribute("error", exception.getMessage());
            } catch (SQLException exception) {
                log("Giao dịch đăng ký thẻ thất bại.", exception);
                request.setAttribute("error", AuthController6.GENERIC_ERROR);
            }
        }
        showForm(request, response);
    }

    private void prepare(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute("csrfToken6") == null) {
                session.setAttribute("csrfToken6", UUID.randomUUID().toString());
            }
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("registrationView6", true);
        request.getRequestDispatcher("/reader6/RegistrationUI6.jsp").forward(request, response);
    }
}

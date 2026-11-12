package controller6;

import dao6.*;
import model6.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/staff6/return")
public class ReturnController6 extends HttpServlet {
    private LibraryCardDAO6 cardDAO;
    private BorrowDetailDAO6 borrowDAO;
    private ReturnSlipDAO6 slipDAO;
    private LibraryStaffDAO6 staffDAO;

    @Override public void init() throws ServletException {
        cardDAO = new LibraryCardDAO6(); borrowDAO = new BorrowDetailDAO6();
        slipDAO = new ReturnSlipDAO6(); staffDAO = new LibraryStaffDAO6();
    }

    public LibraryCard6 processReturnRequest(long cardId) throws SQLException {
        LibraryCard6 card = cardDAO.searchCard(cardId);
        if (!"ACTIVE".equals(card.getStatus())) throw new IllegalArgumentException("Thẻ thư viện không còn hoạt động.");
        return card;
    }
    public ReturnSlip6 previewReturn(long cardId, List<ReturnDetail6> items) throws SQLException { return slipDAO.previewReturn(cardId, items); }
    public ReturnSlip6 confirmReturnTransaction(ReturnSlip6 request, long staffId) throws SQLException { return slipDAO.confirmReturnTransaction(request, staffId); }
    public ReturnSlip6 getSavedReceipt(long slipId) throws SQLException { return slipDAO.getSavedReceipt(slipId); }

    private LibraryStaff6 staff(HttpServletRequest request) throws SQLException {
        Object account = request.getSession().getAttribute("authenticatedAccountId6");
        if (account instanceof Long && (Long) account > 0) return staffDAO.findByAccountId((Long) account);
        throw new IllegalArgumentException("Vui lòng đăng nhập để tiếp tục.");
    }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        prepare(request, response);
        try {
            LibraryStaff6 staff = staff(request); request.setAttribute("staff", staff);
            String action = request.getParameter("action");
            if ("receipt".equals(action)) {
                ReturnSlip6 saved = getSavedReceipt(positiveId(request.getParameter("slipId")));
                if (saved.getStaff().getId() != staff.getId()) throw new IllegalArgumentException("Phiếu không thuộc nhân viên hiện tại.");
                request.setAttribute("slip", saved); show(request, response, "ReturnReceiptUI6.jsp"); return;
            }
            Map<String, String> values = "edit".equals(action) ? input(request.getSession()) : new HashMap<>();
            if (!"edit".equals(action)) values.put("cardId", Objects.toString(request.getParameter("cardId"), ""));
            request.setAttribute("values", values);
            loadCard(request, values.get("cardId"));
        } catch (IllegalArgumentException exception) { validationError(request, exception); }
        catch (SQLException exception) { dbError(request, exception); }
        show(request, response, "ReturnUI6.jsp");
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); prepare(request, response);
        Map<String, String> values = input(request.getSession());
        try {
            if (!request.getSession().getAttribute("csrfToken6").equals(request.getParameter("csrfToken"))) {
                response.setStatus(403); throw new IllegalArgumentException("Phiên làm việc đã hết hạn. Vui lòng tải lại trang.");
            }
            LibraryStaff6 staff = staff(request); request.setAttribute("staff", staff);
            if ("preview".equals(request.getParameter("action"))) {
                values = capture(request); request.getSession().setAttribute("returnInput6", Map.copyOf(values));
                List<ReturnDetail6> items = new ArrayList<>();
                String[] selected = request.getParameterValues("selected");
                if (selected != null) for (String idText : selected) {
                    long id = positiveId(idText);
                    BorrowDetail6 borrowed = new BorrowDetail6(); borrowed.setId(id);
                    ReturnDetail6 item = new ReturnDetail6(); item.setBorrowDetail(borrowed);
                    item.setReturnedQuantity(quantity(values.get("q" + id)));
                    item.setDamagedQuantity(quantity(values.get("d" + id)));
                    item.setConditionNote(values.get("n" + id)); items.add(item);
                }
                ReturnSlip6 draft = previewReturn(positiveId(values.get("cardId")), items);
                draft.setStaff(staff);
                request.getSession().setAttribute("returnDraft6", draft);
                request.setAttribute("slip", draft); show(request, response, "ReturnPreviewUI6.jsp"); return;
            }
            if (!"confirm".equals(request.getParameter("action"))) throw new IllegalArgumentException("Thao tác không hợp lệ.");
            ReturnSlip6 draft = (ReturnSlip6) request.getSession().getAttribute("returnDraft6");
            if (draft == null || !draft.getRequestKey().equals(request.getParameter("requestKey")) || draft.getStaff().getId() != staff.getId()) {
                throw new IllegalArgumentException("Bản xem trước đã hết hiệu lực. Vui lòng xem trước lại.");
            }
            String settled = request.getParameter("fineSettled");
            if (!"true".equals(settled) && !"false".equals(settled)) throw new IllegalArgumentException("Chọn tình trạng thanh toán phạt.");
            // Không sửa draft dùng chung trong session khi hai POST đến đồng thời.
            ReturnSlip6 command = new ReturnSlip6(); command.setLibraryCard(draft.getLibraryCard());
            command.setStaff(draft.getStaff()); command.setReturnDate(draft.getReturnDate());
            command.setDetails(draft.getDetails()); command.setRequestKey(draft.getRequestKey()); command.setFineSettled(Boolean.parseBoolean(settled));
            ReturnSlip6 saved = confirmReturnTransaction(command, staff.getId());
            response.setStatus(303);
            response.setHeader("Location", request.getContextPath() + "/staff6/return?action=receipt&slipId=" + saved.getId());
            return;
        } catch (IllegalArgumentException exception) { validationError(request, exception); }
        catch (SQLException exception) { dbError(request, exception); }
        request.setAttribute("values", values);
        if (request.getAttribute("staff") != null) {
            try { loadCard(request, values.get("cardId")); }
            catch (IllegalArgumentException exception) { request.setAttribute("lookupError", exception.getMessage()); }
            catch (SQLException exception) { dbError(request, exception); }
        }
        show(request, response, "ReturnUI6.jsp");
    }

    private void loadCard(HttpServletRequest request, String text) throws SQLException {
        if (text == null || text.isBlank()) return;
        long cardId = positiveId(text);
        request.setAttribute("card", processReturnRequest(cardId));
        List<BorrowDetail6> borrowed = borrowDAO.getBorrowedItems(cardId);
        request.setAttribute("borrowed", borrowed);
        if (borrowed.isEmpty()) request.setAttribute("lookupError", "Bạn đọc không có tài liệu đang mượn.");
    }

    private static Map<String, String> capture(HttpServletRequest request) {
        Map<String, String> values = new HashMap<>();
        values.put("cardId", Objects.toString(request.getParameter("cardId"), ""));
        request.getParameterMap().forEach((key, value) -> {
            if (key.matches("[qdn][0-9]+")) values.put(key, value[0]);
        });
        String[] selected = request.getParameterValues("selected");
        if (selected != null) for (String id : selected) values.put("s" + id, "true");
        return values;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> input(HttpSession session) {
        Object value = session.getAttribute("returnInput6");
        return value instanceof Map ? new HashMap<>((Map<String, String>) value) : new HashMap<>();
    }
    private static long positiveId(String value) {
        try { long id = Long.parseLong(value == null ? "" : value.strip()); if (id > 0) return id; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mã thẻ không hợp lệ.");
    }
    private static int quantity(String value) {
        try { return Integer.parseInt(value == null ? "" : value.strip()); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Số lượng trả và hỏng phải là số nguyên trong giới hạn cho phép."); }
    }
    private void dbError(HttpServletRequest request, SQLException exception) {
        log("Xử lý nhận trả tài liệu thất bại.", exception);
        request.setAttribute("error", AuthController6.GENERIC_ERROR);
    }
    private void validationError(HttpServletRequest request, IllegalArgumentException exception) {
        if (exception.getCause() != null) log("Return validation/policy failure",exception);
        request.setAttribute("error",exception.getMessage());
    }
    private static void prepare(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=UTF-8"); response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute("csrfToken6") == null) session.setAttribute("csrfToken6", UUID.randomUUID().toString());
        }
    }
    private void show(HttpServletRequest request, HttpServletResponse response, String jsp) throws ServletException, IOException {
        request.setAttribute("returnView6", true);
        request.getRequestDispatcher("/staff6/" + jsp).forward(request, response);
    }
}

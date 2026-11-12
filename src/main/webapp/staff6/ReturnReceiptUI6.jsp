<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:if test="${not returnView6 or empty slip}"><c:redirect url="/staff6/return" /></c:if>
<!DOCTYPE html><html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>LibMan6 — Phiếu trả ${slip.id}</title>
<%@ include file="/WEB-INF/Styles6.jspf" %>
</head><body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main class="wide receipt">
<div class="no-print"><a href="${pageContext.request.contextPath}/staff6/return">← Nhận trả tài liệu</a><p class="success">Nhận trả tài liệu thành công.</p></div>
<article id="receipt-content">
<p class="brand">LibMan6 · Thư viện đại học</p><h1>Phiếu trả tài liệu #<c:out value="${slip.id}" /></h1>
<p>Ngày trả: <c:out value="${slip.returnDate}" /> · Mã thẻ: <c:out value="${slip.libraryCard.id}" /></p>
<p>Bạn đọc: <c:out value="${fn:replace(slip.libraryCard.reader.fullName, '[DEMO RETURN6] ', '')}" /> · Lớp / đơn vị: <c:out value="${slip.libraryCard.reader.classOrDepartment}" /></p>
<p>Nhân viên: <c:out value="${fn:replace(slip.staff.fullName, '[DEMO RETURN6] ', '')}" /> (#<c:out value="${slip.staff.id}" />)</p>
<%@ include file="ReturnLines6.jspf" %>
<p>Thanh toán phạt: <strong><c:choose><c:when test="${slip.totalFine.signum() == 0}">Không phát sinh phạt</c:when><c:when test="${slip.fineSettled}">Đã thanh toán</c:when><c:otherwise>Chưa thanh toán</c:otherwise></c:choose></strong></p>
</article>
<div class="no-print"><button type="button" onclick="window.print()">In / In lại</button>
</div>
</main></body></html>

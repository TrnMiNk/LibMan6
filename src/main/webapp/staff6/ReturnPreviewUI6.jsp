<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:if test="${not returnView6 or empty slip}"><c:redirect url="/staff6/return" /></c:if>
<!DOCTYPE html><html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>LibMan6 — Xem trước phiếu trả</title>
<%@ include file="/WEB-INF/Styles6.jspf" %>
</head><body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main class="wide">
<h1>Xem trước phiếu trả</h1><p>Vui lòng kiểm tra thông tin trước khi xác nhận.</p>
<p>Ngày trả: <c:out value="${slip.returnDate}" /> · Mã thẻ: <c:out value="${slip.libraryCard.id}" /></p>
<p>Bạn đọc: <c:out value="${fn:replace(slip.libraryCard.reader.fullName, '[DEMO RETURN6] ', '')}" /> · Nhân viên: <c:out value="${fn:replace(slip.staff.fullName, '[DEMO RETURN6] ', '')}" /></p>
<%@ include file="ReturnLines6.jspf" %>
<form method="post" action="${pageContext.request.contextPath}/staff6/return">
<input type="hidden" name="action" value="confirm">
<input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken6}' />">
<input type="hidden" name="requestKey" value="<c:out value='${slip.requestKey}' />">
<fieldset><legend>Tình trạng thanh toán phạt</legend>
<c:choose><c:when test="${slip.totalFine.signum() == 0}">
<p>Không phát sinh phạt</p>
<input type="hidden" name="fineSettled" value="false">
</c:when><c:otherwise>
<div class="payment-options">
<label class="radio-option"><input type="radio" name="fineSettled" value="false" checked><span>Chưa thanh toán</span></label>
<label class="radio-option"><input type="radio" name="fineSettled" value="true"><span>Đã thanh toán</span></label>
</div>
</c:otherwise></c:choose>
</fieldset>
<a class="button secondary" href="${pageContext.request.contextPath}/staff6/return?action=edit">Quay lại sửa</a>
<button type="submit">Xác nhận</button>
</form></main></body></html>

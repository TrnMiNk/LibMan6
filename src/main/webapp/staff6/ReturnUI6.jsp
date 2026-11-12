<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not returnView6}"><c:redirect url="/staff6/return" /></c:if>
<!DOCTYPE html><html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>LibMan6 — Nhận trả tài liệu</title>
<%@ include file="/WEB-INF/Styles6.jspf" %>
</head><body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main class="wide">

<h1>Nhận trả tài liệu</h1>
<c:if test="${not empty error}"><p class="error" role="alert"><c:out value="${error}" /></p></c:if>
<c:if test="${not empty lookupError}"><p class="notice" role="status"><c:out value="${lookupError}" /></p></c:if>
<form method="get" action="${pageContext.request.contextPath}/staff6/return">
<label for="cardId">Nhập / quét mã thẻ</label>
<input id="cardId" name="cardId" inputmode="numeric" pattern="[0-9]+" required autofocus autocomplete="off" value="<c:out value='${values.cardId}' />">
<button type="submit">Tìm kiếm</button>
</form>
<c:if test="${not empty card}">
<h2>Bạn đọc: <c:out value="${fn:replace(card.reader.fullName, '[DEMO RETURN6] ', '')}" /></h2>
<p>Mã thẻ: <c:out value="${card.id}" /> · Lớp / đơn vị: <c:out value="${card.reader.classOrDepartment}" /></p>
<c:if test="${not empty borrowed}">
<form method="post" action="${pageContext.request.contextPath}/staff6/return">
<input type="hidden" name="action" value="preview">
<input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken6}' />">
<input type="hidden" name="cardId" value="<c:out value='${card.id}' />">
<div class="table-scroll"><table>
<thead><tr><th>Chọn trả</th><th>Tài liệu / hạn trả</th><th>Còn mượn</th><th>Số lượng trả</th><th>Số lượng hỏng</th><th>Tình trạng</th></tr></thead>
<tbody><c:forEach var="b" items="${borrowed}">
<c:set var="qkey" value="q${b.id}" /><c:set var="dkey" value="d${b.id}" /><c:set var="nkey" value="n${b.id}" /><c:set var="skey" value="s${b.id}" />
<tr><td><input type="checkbox" name="selected" value="${b.id}" aria-label="Chọn chi tiết ${b.id}" <c:if test="${values[skey] == 'true'}">checked</c:if>></td>
<td><c:out value="${fn:replace(b.document.title, '[DEMO RETURN6] ', '')}" /><br><small>Chi tiết #${b.id} · Hạn: <c:out value="${b.dueDate}" /></small></td>
<td><c:out value="${b.remainingQuantity}" /></td>
<td><input type="number" name="q${b.id}" min="1" max="${b.remainingQuantity}" step="1" aria-label="Số lượng trả ${b.id}" value="<c:out value='${empty values[qkey] ? 1 : values[qkey]}' />"></td>
<td><input type="number" name="d${b.id}" min="0" max="${b.remainingQuantity}" step="1" aria-label="Số lượng hỏng ${b.id}" value="<c:out value='${empty values[dkey] ? 0 : values[dkey]}' />"></td>
<td><input name="n${b.id}" maxlength="300" aria-label="Ghi chú ${b.id}" value="<c:out value='${values[nkey]}' />"></td></tr>
</c:forEach></tbody></table></div>
<button type="submit">Xem trước</button>
</form></c:if></c:if>
</main></body></html>

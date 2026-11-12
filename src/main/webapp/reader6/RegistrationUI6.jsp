<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not registrationView6}"><c:redirect url="/reader6/register" /></c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>LibMan6 — Đăng ký thẻ</title>
    <%@ include file="/WEB-INF/Styles6.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main>
    <a href="${pageContext.request.contextPath}/reader6/PortalUI6.jsp">← Cổng bạn đọc</a>
    <h1>Đăng ký thẻ thư viện</h1>
    <c:if test="${not empty error}"><p class="error" role="alert"><c:out value="${error}" /></p></c:if>
    <c:choose>
        <c:when test="${not empty card}">
            <p class="success" role="status">Đăng ký thẻ thành công.</p>
            <dl>
                <dt>Mã thẻ</dt><dd><c:out value="${card.id}" /></dd>
                <dt>Họ và tên</dt><dd><c:out value="${fn:replace(card.reader.fullName, '[DEMO RETURN6] ', '')}" /></dd>
                <dt>Điện thoại</dt><dd><c:out value="${card.reader.phone}" default="—" /></dd>
                <dt>Email</dt><dd><c:out value="${card.reader.email}" default="—" /></dd>
                <dt>Lớp / đơn vị</dt><dd><c:out value="${card.reader.classOrDepartment}" /></dd>
                <dt>Ngày đăng ký</dt><dd><c:out value="${registeredAtText}" /></dd>
                <dt>Trạng thái</dt><dd><c:out value="${card.status}" /> — <c:choose><c:when test="${card.status == 'ACTIVE'}">Đang hoạt động</c:when><c:otherwise>Ngừng hoạt động</c:otherwise></c:choose></dd>
            </dl>
        </c:when>
        <c:otherwise>
            <p>Họ tên và lớp/đơn vị là thông tin bắt buộc (*). Điện thoại và email có thể để trống.</p>
            <form method="post" action="${pageContext.request.contextPath}/reader6/register">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken6}' />">
                <label for="fullName">Họ và tên *</label>
                <input id="fullName" name="fullName" maxlength="120" required autocomplete="name" value="<c:out value='${reader.fullName}' />">
                <label for="phone">Điện thoại</label>
                <input id="phone" name="phone" type="tel" maxlength="30" autocomplete="tel" value="<c:out value='${reader.phone}' />">
                <label for="email">Email</label>
                <input id="email" name="email" type="email" maxlength="150" autocomplete="email" value="<c:out value='${reader.email}' />">
                <label for="classOrDepartment">Lớp / đơn vị *</label>
                <input id="classOrDepartment" name="classOrDepartment" maxlength="120" required value="<c:out value='${reader.classOrDepartment}' />">
                <button type="submit">Add</button>
            </form>
        </c:otherwise>
    </c:choose>
</main></body>
</html>

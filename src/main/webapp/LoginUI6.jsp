<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not loginView6}"><c:redirect url="/auth" /></c:if>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>LibMan6 — Đăng nhập</title><%@ include file="/WEB-INF/Styles6.jspf" %></head>
<body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main><p class="brand">LibMan6 · Thư viện đại học</p><h1>Đăng nhập</h1>
<c:if test="${not empty error}"><p class="error" role="alert"><c:out value="${error}" /></p></c:if>
<form method="post" action="${pageContext.request.contextPath}/auth">
<input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken6}' />">
<label for="username">Tên đăng nhập</label><input id="username" name="username" maxlength="80" required autocomplete="username" value="<c:out value='${username}' />">
<label for="password">Mật khẩu</label><input id="password" name="password" type="password" maxlength="1024" required autocomplete="current-password">
<button type="submit">Đăng nhập</button></form></main></body></html>

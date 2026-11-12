<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>LibMan6 — Cổng bạn đọc</title>
    <%@ include file="/WEB-INF/Styles6.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main>
    <p class="brand">LibMan6 · Thư viện đại học</p>
    <h1>Cổng bạn đọc</h1>
    <p>Đăng ký thẻ để sử dụng dịch vụ của thư viện.</p>
    <a class="button" href="${pageContext.request.contextPath}/reader6/register">Đăng ký thẻ</a>
    
</main></body>
</html>

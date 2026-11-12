<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>LibMan6</title>
<%@ include file="/WEB-INF/Styles6.jspf" %></head><body>
<%@ include file="/WEB-INF/Header6.jspf" %>
<main>
<h1>LibMan6</h1><p class="error" role="alert"><c:out value="${error}" default="Không thể xử lý yêu cầu. Vui lòng thử lại." /></p>
<a href="${pageContext.request.contextPath}/">Về trang chủ</a></main></body></html>

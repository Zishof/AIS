<%@page import="org.json.JSONArray"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.action.servlet.api.LinimasaApi"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}
JSONObject jsonObject = new JSONObject();
jsonObject.put("token", tbmuser.getToken());
jsonObject.put("action", "linimasa");
jsonObject.put("refresh", "false");
jsonObject.put("sd", "5");
JSONObject hasil = LinimasaApi.linimasa(tbmuser, jsonObject, request);
JSONArray data = hasil.getJSONArray("data");
request.setAttribute("data", data);

%>


<div class="row gy-5 g-xl-10">
	<jsp:include
		page="/WEB-INF/o/ux/content/elearning/beranda_pertemuan.jsp">
		<jsp:param value="${data}" name="data" />
	</jsp:include>
	<jsp:include
		page="/WEB-INF/o/ux/content/elearning/beranda_tugas.jsp">
		<jsp:param value="${data}" name="data" />
	</jsp:include>
</div>
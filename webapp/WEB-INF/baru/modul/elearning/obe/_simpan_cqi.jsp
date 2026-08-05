<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%!
	private String esc(String s) {
		if (s == null) return "";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) { char c = s.charAt(i);
			if (c == '"') b.append("\\\""); else if (c == '\\') b.append("\\\\");
			else if (c == '\n') b.append("\\n"); else if (c == '\r') b.append("\\r"); else if (c == '\t') b.append("\\t");
			else if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
		}
		return b.toString();
	}
%><%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) { out.print("{\"status\":\"error\",\"message\":\"Sesi berakhir.\"}"); return; }
// Gate: hanya dosen atau admin
boolean bolehEdit = tbmuser.getDosen() != null || Common.getApakahAdminLain(tbmuser);
if (!bolehEdit) { out.print("{\"status\":\"error\",\"message\":\"Anda tidak memiliki hak untuk menyimpan CQI.\"}"); return; }

String idPerk = request.getParameter("perkuliahan");
String data = request.getParameter("data");
if (idPerk == null || idPerk.trim().isEmpty() || data == null) { out.print("{\"status\":\"error\",\"message\":\"Parameter tidak lengkap.\"}"); return; }

// Validasi JSON
String jsonSimpan;
try { jsonSimpan = new JSONArray(data).toString(); }
catch (Exception e) { out.print("{\"status\":\"error\",\"message\":\"Format data CQI tidak valid.\"}"); return; }

Session s = null; Transaction tx = null;
try {
	s = HibernateUtil.openSession();
	tx = s.beginTransaction();
	Perkuliahan p = (Perkuliahan) s.get(Perkuliahan.class, Long.parseLong(idPerk.trim()));
	if (p == null) { out.print("{\"status\":\"error\",\"message\":\"Perkuliahan tidak ditemukan.\"}"); tx.rollback(); return; }
	p.setCqiData(jsonSimpan);
	s.update(p);
	s.flush();
	tx.commit();
	try { Common.refreshUpdate(p); } catch (Exception e) {}
	out.print("{\"status\":\"success\",\"message\":\"" + esc(Common.getBahasaConfig("Data CQI berhasil disimpan.")) + "\"}");
} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_simpan_cqi.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (s != null) { try { if (s.isOpen()) s.close(); } catch (Exception ex) {} }
}
%>

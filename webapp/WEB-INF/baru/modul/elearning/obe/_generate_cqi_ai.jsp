<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.action.master.helper.ObeAiJspHelper"%>
<%@page import="ais.action.servlet.AiGenerateServlet"%>
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
String idPerk = request.getParameter("perkuliahan");
if (idPerk == null || idPerk.trim().isEmpty()) { out.print("{\"status\":\"error\",\"message\":\"Parameter perkuliahan tidak valid.\"}"); return; }

try {
	Perkuliahan p = (Perkuliahan) GeneralValueObject.ambilData(Perkuliahan.class, idPerk.trim(), true);
	if (p == null || p.getMatakuliah() == null) { out.print("{\"status\":\"error\",\"message\":\"Data perkuliahan/matakuliah tidak ditemukan.\"}"); return; }
	Matakuliah mk = p.getMatakuliah();

	StringBuilder daftar = new StringBuilder();
	String cpmkCsv = mk.getCapaianPembelajaranLulusan();
	if (cpmkCsv != null && !cpmkCsv.trim().isEmpty()) {
		for (String id : cpmkCsv.split(",")) {
			if (id == null || id.trim().isEmpty()) continue;
			CapaianPembelajaranLulusan cp = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, id.trim(), true);
			if (cp == null) continue;
			daftar.append(cp.getKode() != null ? cp.getKode() : ("CPMK" + cp.getId())).append(": ").append(ObeAiJspHelper.bersih(cp.getNama())).append("\n");
		}
	}
	if (daftar.length() == 0) { out.print("{\"status\":\"error\",\"message\":\"Belum ada CPMK pada mata kuliah ini.\"}"); return; }

	StringBuilder pr = new StringBuilder();
	pr.append("Untuk mata kuliah \"").append(ObeAiJspHelper.bersih(mk.getNama())).append("\", buat analisis CQI (Continuous Quality Improvement) per CPMK. ");
	pr.append("Untuk TIAP CPMK isi: masalah/gap ketercapaian yang umum terjadi, analisis penyebab (akar masalah), dan rencana tindak lanjut perbaikan. Bahasa Indonesia akademis, ringkas.\n\n");
	pr.append("Daftar CPMK:\n").append(daftar).append("\n");
	pr.append("Keluarkan HANYA JSON array valid (satu objek per CPMK sesuai kode), format:\n");
	pr.append("[{\"cpmk\":\"<kode>\",\"masalah\":\"...\",\"analisis\":\"...\",\"rencana\":\"...\"}]");

	String hasil = AiGenerateServlet.generateText(pr.toString(), 2500);
	JSONArray arr = ObeAiJspHelper.ekstrakArray(hasil);
	if (arr.length() == 0) { out.print("{\"status\":\"error\",\"message\":\"Tidak ada analisis dari respons AI. Coba lagi.\"}"); return; }

	out.print("{\"status\":\"success\",\"data\":" + arr.toString() + "}");
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_generate_cqi_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
}
%>

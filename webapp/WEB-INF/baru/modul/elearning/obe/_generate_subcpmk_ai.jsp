<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
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
String idCpmk = request.getParameter("idCpmk");
if (idCpmk == null || idCpmk.trim().isEmpty()) { out.print("{\"status\":\"error\",\"message\":\"Parameter idCpmk tidak valid.\"}"); return; }

Session s = null; Transaction tx = null;
int dibuat = 0;
try {
	CapaianPembelajaranLulusan cpmk = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, idCpmk.trim(), true);
	if (cpmk == null) { out.print("{\"status\":\"error\",\"message\":\"CPMK tidak ditemukan.\"}"); return; }
	String cpmkTeks = ObeAiJspHelper.bersih(cpmk.getNama());
	String mkNama = "";
	try { if (cpmk.getKhususBuatMk() != null) mkNama = ObeAiJspHelper.bersih(cpmk.getKhususBuatMk().getNama()); } catch (Exception e) {}

	StringBuilder p = new StringBuilder();
	p.append("Berdasarkan CPMK (Capaian Pembelajaran Matakuliah) berikut, buatkan 3 sampai 5 Sub-CPMK: kemampuan akhir yang SPESIFIK, TERUKUR, dan menyusun CPMK ini.\n");
	if (mkNama.length() > 0) p.append("Matakuliah: ").append(mkNama).append("\n");
	p.append("CPMK: ").append(cpmkTeks).append("\n\n");
	p.append("Keluarkan HANYA JSON array valid (tanpa teks/markdown lain), format persis:\n");
	p.append("[{\"kode\":\"Sub-CPMK1\",\"nama\":\"kemampuan akhir...\",\"bobot\":25}]\n");
	p.append("Total seluruh bobot = 100.");

	String hasil = AiGenerateServlet.generateText(p.toString(), 900);
	JSONArray gen = ObeAiJspHelper.ekstrakArray(hasil);
	if (gen.length() == 0) { out.print("{\"status\":\"error\",\"message\":\"Tidak ada Sub-CPMK dari respons AI. Coba lagi.\"}"); return; }

	// Gabungkan dengan formula yang sudah ada
	JSONArray formula = new JSONArray();
	try { if (cpmk.getFormula() != null && !cpmk.getFormula().trim().isEmpty()) formula = new JSONArray(cpmk.getFormula()); } catch (Exception e) {}
	long base = System.currentTimeMillis();
	for (int i = 0; i < gen.length(); i++) {
		JSONObject o = gen.optJSONObject(i);
		if (o == null) continue;
		String nama = o.optString("nama", "").trim();
		if (nama.isEmpty()) continue;
		long key = Math.abs(base + i * 7L + (long) (java.lang.Math.random() * 100000));
		JSONObject sub = new JSONObject();
		sub.put("key", String.valueOf(key));
		sub.put("id", String.valueOf(key));
		sub.put("kode", o.optString("kode", "Sub-CPMK" + (formula.length() + 1)));
		sub.put("nama", nama);
		sub.put("bobot", o.optDouble("bobot", 0.0));
		formula.put(sub);
		dibuat++;
	}
	if (dibuat == 0) { out.print("{\"status\":\"error\",\"message\":\"Sub-CPMK kosong.\"}"); return; }

	s = HibernateUtil.openSession();
	tx = s.beginTransaction();
	CapaianPembelajaranLulusan cpmkDb = (CapaianPembelajaranLulusan) s.get(CapaianPembelajaranLulusan.class, cpmk.getId());
	cpmkDb.setFormula(formula.toString());
	s.update(cpmkDb);
	s.flush();
	tx.commit();
	try { Common.refreshUpdate(cpmkDb); } catch (Exception e) {}

	out.print("{\"status\":\"success\",\"dibuat\":" + dibuat + ",\"message\":\"" + esc(dibuat + " Sub-CPMK berhasil dibuat via AI.") + "\"}");
} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_generate_subcpmk_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (s != null) { try { if (s.isOpen()) s.close(); } catch (Exception ex) {} }
}
%>

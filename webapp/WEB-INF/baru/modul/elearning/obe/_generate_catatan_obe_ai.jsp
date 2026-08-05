<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
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
String idKpm = request.getParameter("kurikulumPunyaMatakuliah");
if (idKpm == null || idKpm.trim().isEmpty()) { out.print("{\"status\":\"error\",\"message\":\"Parameter kpm tidak valid.\"}"); return; }

Session s = null; Transaction tx = null;
try {
	KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpm.trim(), true);
	if (kpm == null || kpm.getMatakuliah() == null) { out.print("{\"status\":\"error\",\"message\":\"Data matakuliah tidak ditemukan.\"}"); return; }
	Matakuliah mk = kpm.getMatakuliah();

	StringBuilder p = new StringBuilder();
	p.append(ObeAiJspHelper.bangunKonteks(kpm, mk)).append("\n");
	p.append("Buatkan DATA OBE lengkap untuk mata kuliah \"").append(ObeAiJspHelper.bersih(mk.getNama())).append("\" (kode ")
			.append(mk.getKode() != null ? mk.getKode() : "-").append("), tingkat perguruan tinggi, Bahasa Indonesia formal-akademis.\n\n");
	p.append("Keluarkan HANYA JSON OBJECT valid (tanpa teks/markdown lain) dengan kunci PERSIS berikut, dan IKUTI format contoh tiap nilai:\n");
	p.append("{\n");
	p.append("\"catatan\":\"<p>Catatan/keterangan tambahan RPS & daftar referensi (boleh HTML sederhana)</p>\",\n");
	p.append("\"cplBobot\":\"CPL-2:15,CPL-4:45,CPL-9:40\",\n");
	p.append("\"komponenPenilaian\":\"Kuis:10,Tugas:10,Keaktifan:10,UTS:30,UAS:40\",\n");
	p.append("\"teknikPerCpmk\":\"CPMK-1:Kuis,UTS\\nCPMK-2:Tugas,Unjuk Kerja,UAS\\nCPMK-3:Partisipasi,UTS,UAS\",\n");
	p.append("\"rubrikPenilaian\":\"#Rubrik Presentasi (40%)\\nKelengkapan Materi|15|Lengkap & mendalam|Lengkap kurang dalam|Kurang lengkap|Tidak lengkap\",\n");
	p.append("\"pemetaanSoalUts\":\"Sub CPMK 1.1|PG 1\\nSub CPMK 2.1|PG 8,12,17; Esai 4,5\",\n");
	p.append("\"pemetaanSoalUas\":\"Sub CPMK 2.4|PG 11,12,13\\nSub CPMK 4.1|PG 14-17; Esai 4,5\"\n");
	p.append("}\n");
	p.append("Total persen komponenPenilaian = 100; total cplBobot = 100. Gunakan \\n untuk baris baru dalam string.");

	String hasil = AiGenerateServlet.generateText(p.toString(), 2000);
	JSONObject o = ObeAiJspHelper.ekstrakObjek(hasil);
	if (o.length() == 0) { out.print("{\"status\":\"error\",\"message\":\"Respons AI tidak dikenali. Coba lagi.\"}"); return; }

	s = HibernateUtil.openSession();
	tx = s.beginTransaction();
	KurikulumPunyaMatakuliah kpmDb = (KurikulumPunyaMatakuliah) s.get(KurikulumPunyaMatakuliah.class, kpm.getId());
	int n = 0;
	if (o.has("catatan")) { kpmDb.setCatatan(o.optString("catatan", "")); n++; }
	if (o.has("cplBobot")) { kpmDb.setCplBobot(o.optString("cplBobot", "")); n++; }
	if (o.has("komponenPenilaian")) { kpmDb.setKomponenPenilaian(o.optString("komponenPenilaian", "")); n++; }
	if (o.has("teknikPerCpmk")) { kpmDb.setTeknikPerCpmk(o.optString("teknikPerCpmk", "")); n++; }
	if (o.has("rubrikPenilaian")) { kpmDb.setRubrikPenilaian(o.optString("rubrikPenilaian", "")); n++; }
	if (o.has("pemetaanSoalUts")) { kpmDb.setPemetaanSoalUts(o.optString("pemetaanSoalUts", "")); n++; }
	if (o.has("pemetaanSoalUas")) { kpmDb.setPemetaanSoalUas(o.optString("pemetaanSoalUas", "")); n++; }
	s.update(kpmDb);
	s.flush();
	tx.commit();
	try { Common.refreshUpdate(kpmDb); } catch (Exception e) {}

	out.print("{\"status\":\"success\",\"message\":\"" + esc(n + " bagian Data OBE (catatan, bobot, komponen, teknik, rubrik, pemetaan soal) berhasil dibuat via AI.") + "\"}");
} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_generate_catatan_obe_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (s != null) { try { if (s.isOpen()) s.close(); } catch (Exception ex) {} }
}
%>

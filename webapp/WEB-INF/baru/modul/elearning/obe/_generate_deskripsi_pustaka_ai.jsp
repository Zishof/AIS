<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.ObeAiJspHelper"%>
<%@page import="ais.action.servlet.AiGenerateServlet"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
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
int nBk = 0, nPu = 0, nPp = 0;
try {
	KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpm.trim(), true);
	if (kpm == null || kpm.getMatakuliah() == null) { out.print("{\"status\":\"error\",\"message\":\"Data matakuliah tidak ditemukan.\"}"); return; }
	Matakuliah mk = kpm.getMatakuliah();

	StringBuilder p = new StringBuilder();
	p.append(ObeAiJspHelper.bangunKonteks(kpm, mk)).append("\n");
	p.append("Buatkan konten RPS untuk mata kuliah \"").append(ObeAiJspHelper.bersih(mk.getNama())).append("\" (kode ")
			.append(mk.getKode() != null ? mk.getKode() : "-").append(", program studi ")
			.append(mk.getJurusan() != null ? ObeAiJspHelper.bersih(mk.getJurusan().getNama()) : "-").append("), Bahasa Indonesia formal-akademis.\n");
	p.append("Keluarkan HANYA JSON object valid (tanpa teks/markdown lain) dengan kunci PERSIS:\n");
	p.append("{\n\"deskripsi\":\"deskripsi singkat mata kuliah 1-2 paragraf\",\n");
	p.append("\"mitraPengembang\":\"pihak/mitra pengembang RPS (institusi/asosiasi relevan)\",\n");
	p.append("\"bahanKajian\":[\"materi/bahan kajian 1\",\"materi 2\",\"materi 3\"],\n");
	p.append("\"pustakaUtama\":[\"sitasi utama format Penulis. (Tahun). Judul. Penerbit.\"],\n");
	p.append("\"pustakaPendukung\":[\"sitasi pendukung 1\",\"sitasi 2\"]\n}");

	String hasil = AiGenerateServlet.generateText(p.toString(), 1800);
	JSONObject o = ObeAiJspHelper.ekstrakObjek(hasil);
	if (o.length() == 0) { out.print("{\"status\":\"error\",\"message\":\"Respons AI tidak dikenali. Coba lagi.\"}"); return; }

	PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
	s = HibernateUtil.openSession();
	tx = s.beginTransaction();
	Matakuliah mkDb = (Matakuliah) s.get(Matakuliah.class, mk.getId());
	KurikulumPunyaMatakuliah kpmDb = (KurikulumPunyaMatakuliah) s.get(KurikulumPunyaMatakuliah.class, kpm.getId());

	String deskripsi = o.optString("deskripsi", "").trim();
	if (deskripsi.length() > 0) kpmDb.setDeskripsiPembelajaran(deskripsi);
	String mitra = o.optString("mitraPengembang", "").trim();
	if (mitra.length() > 0) kpmDb.setMitraPengembang(mitra);

	JSONArray bk = o.optJSONArray("bahanKajian");
	if (bk != null) {
		String csv = mkDb.getBahanKajian();
		for (int i = 0; i < bk.length(); i++) {
			String nama = bk.optString(i, "").trim();
			if (nama.isEmpty()) continue;
			Long id = ObeAiJspHelper.buatBahanKajian(s, mkDb, pt, null, nama);
			csv = ObeAiJspHelper.appendId(csv, id); nBk++;
		}
		mkDb.setBahanKajian(csv);
	}
	JSONArray pu = o.optJSONArray("pustakaUtama");
	if (pu != null) {
		String csv = kpmDb.getPustaka();
		for (int i = 0; i < pu.length(); i++) {
			String nama = pu.optString(i, "").trim();
			if (nama.isEmpty()) continue;
			Long id = ObeAiJspHelper.buatReferensi(s, pt, nama);
			csv = ObeAiJspHelper.appendId(csv, id); nPu++;
		}
		kpmDb.setPustaka(csv);
	}
	JSONArray pp = o.optJSONArray("pustakaPendukung");
	if (pp != null) {
		String csv = kpmDb.getPustakaPendukung();
		for (int i = 0; i < pp.length(); i++) {
			String nama = pp.optString(i, "").trim();
			if (nama.isEmpty()) continue;
			Long id = ObeAiJspHelper.buatReferensi(s, pt, nama);
			csv = ObeAiJspHelper.appendId(csv, id); nPp++;
		}
		kpmDb.setPustakaPendukung(csv);
	}

	s.update(mkDb);
	s.update(kpmDb);
	s.flush();
	tx.commit();
	try { Common.refreshUpdate(mkDb); } catch (Exception e) {}
	try { Common.refreshUpdate(kpmDb); } catch (Exception e) {}

	out.print("{\"status\":\"success\",\"message\":\"" + esc("Deskripsi & mitra diperbarui; " + nBk + " bahan kajian, " + nPu + " pustaka utama, " + nPp + " pustaka pendukung dibuat via AI.") + "\"}");
} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_generate_deskripsi_pustaka_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (s != null) { try { if (s.isOpen()) s.close(); } catch (Exception ex) {} }
}
%>

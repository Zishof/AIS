<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
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
int nCocok = 0, nBaru = 0;
try {
	KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpm.trim(), true);
	if (kpm == null || kpm.getMatakuliah() == null) { out.print("{\"status\":\"error\",\"message\":\"Data matakuliah tidak ditemukan.\"}"); return; }
	Matakuliah mk = kpm.getMatakuliah();

	Map<String, Long> pool = new HashMap<String, Long>();
	StringBuilder daftar = new StringBuilder();
	s = HibernateUtil.openSession();
	try {
		List<?> list = s.createCriteria(CapaianLulusan.class)
				.add(mk.getJurusan() != null ? Restrictions.eq("jurusan", mk.getJurusan()) : Restrictions.isNull("jurusan"))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).list();
		for (Object o : list) {
			CapaianLulusan cp = (CapaianLulusan) o;
			if (cp.getAktif() != null && !cp.getAktif()) continue;
			String kode = cp.getKode() != null ? cp.getKode() : ("CPL" + cp.getId());
			pool.put(kode.trim().toUpperCase(), cp.getId());
			daftar.append(kode).append(" - ").append(ObeAiJspHelper.bersih(cp.getNama())).append("\n");
		}
	} finally { try { if (s.isOpen()) s.close(); } catch (Exception e) {} }
	if (daftar.length() == 0) daftar.append("(Belum ada CPL terdaftar)\n");

	StringBuilder p = new StringBuilder();
	p.append(ObeAiJspHelper.bangunKonteks(kpm, mk)).append("\n");
	p.append("Nama Matakuliah: ").append(ObeAiJspHelper.bersih(mk.getNama())).append("\n");
	p.append("Kode: ").append(mk.getKode() != null ? mk.getKode() : "-").append("\n\n");
	p.append("Daftar Capaian Pembelajaran Lulusan (CPL) yang tersedia di program studi ini:\n").append(daftar).append("\n");
	p.append("Tolong analisis: Capaian Pembelajaran Lulusan (CPL) mana yang paling cocok untuk matakuliah ini?\n");
	p.append("Juga usulkan Capaian Pembelajaran Lulusan (CPL) BARU jika relevan tapi belum ada di daftar.\n\n");
	p.append("Format jawaban WAJIB (jangan tambah teks lain di luar format ini):\n");
	p.append("COCOK: [kode1, kode2, ...]\n");
	p.append("ALASAN_COCOK: [alasan singkat mengapa cocok untuk matakuliah ini]\n");
	p.append("USUL_BARU:\n- [KODE_BARU]: [deskripsi singkat item baru yang disarankan]\n(tulis TIDAK ADA jika tidak ada usulan baru)");

	String hasil = AiGenerateServlet.generateText(p.toString(), 1500);
	ObeAiJspHelper.Seleksi sel = ObeAiJspHelper.parseSeleksi(hasil);

	PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
	s = HibernateUtil.openSession();
	tx = s.beginTransaction();
	Matakuliah mkDb = (Matakuliah) s.get(Matakuliah.class, mk.getId());
	String csv = mkDb.getCapaianLulusan();
	for (String kode : sel.cocok) {
		Long id = pool.get(kode.trim().toUpperCase());
		if (id != null) { String baru = ObeAiJspHelper.appendId(csv, id); if (!baru.equals(csv)) { csv = baru; nCocok++; } }
	}
	for (String[] ub : sel.baru) {
		if (ub[1] == null || ub[1].trim().isEmpty()) continue;
		Long id = ObeAiJspHelper.buatCapaianLulusan(s, mkDb, pt, ub[0], ub[1]);
		csv = ObeAiJspHelper.appendId(csv, id); nBaru++;
	}
	mkDb.setCapaianLulusan(csv);
	s.update(mkDb);
	s.flush();
	tx.commit();
	try { Common.refreshUpdate(mkDb); } catch (Exception e) {}

	out.print("{\"status\":\"success\",\"cocok\":" + nCocok + ",\"baru\":" + nBaru
			+ ",\"message\":\"" + esc(nCocok + " CPL cocok ditambahkan, " + nBaru + " CPL baru dibuat via AI.") + "\"}");
} catch (Exception e) {
	if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) {} }
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_generate_cpl_ai.jsp");
	out.print("{\"status\":\"error\",\"message\":\"" + esc(e.getMessage() != null ? e.getMessage() : "error") + "\"}");
} finally {
	if (s != null) { try { if (s.isOpen()) s.close(); } catch (Exception ex) {} }
}
%>

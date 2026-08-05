<%@page session="false"%>
<%--
  Endpoint JSON dasbor "Laporan Pergudangan" (Outlet & Gudang).
  Mengembalikan: jenis[], lokasi[], rekap[] (stok+2 versi nilai), tren[] (mutasi harian 30 hari).
  Reuse StokLokasiUtil (rekapStokLokasiLengkap + trenMutasiHarian) & LokasiKantinUtil.
  Sesi currentSession() (TAK ditutup). Read-only (aman untuk semua yang boleh melihat).
--%>
<%@page import="org.json.*"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.asset.JenisLokasi"%>
<%@page import="ais.database.model.asset.Lokasi"%>
<%@page import="ais.action.master.koperasi.helper.LokasiKantinUtil"%>
<%@page import="ais.action.master.inventory.StokLokasiUtil"%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
	Tbmuser tbmuser = Common.getCurrentUser(request);
	if (tbmuser == null || tbmuser.getUserId() == null) {
		result.put("status", "01"); result.put("message", "Sesi berakhir."); out.print(result.toString()); return;
	}
	// Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
	// JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
	Session session = HibernateUtil.currentNativeSession();
	Long jenisId = null, lokasiId = null;
	try { String s = request.getParameter("jenis"); if (s != null && s.trim().length() > 0) jenisId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_pergudangan/service.jsp:32");}
	try { String s = request.getParameter("lokasi"); if (s != null && s.trim().length() > 0) lokasiId = Long.valueOf(s.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_pergudangan/service.jsp:33");}
	String cari = request.getParameter("cari");

	// daftar jenis (untuk filter + preset)
	JSONArray jarr = new JSONArray();
	List<JenisLokasi> jenis = LokasiKantinUtil.daftarJenisLokasi(session, true);
	for (JenisLokasi j : jenis) {
		JSONObject o = new JSONObject();
		o.put("id", j.getId()); o.put("nama", j.getNama() == null ? "" : j.getNama());
		o.put("warna", j.getWarna() == null ? "" : j.getWarna());
		jarr.put(o);
	}
	// daftar lokasi (dibatasi jenis bila difilter)
	JSONArray larr = new JSONArray();
	List<Lokasi> lokasis = LokasiKantinUtil.daftarLokasi(session, jenisId, null, true);
	for (Lokasi l : lokasis) {
		JSONObject o = new JSONObject();
		o.put("id", l.getId()); o.put("nama", l.getNama() == null ? "" : l.getNama());
		larr.put(o);
	}
	// rekap stok (2 versi nilai)
	JSONArray rarr = new JSONArray();
	List<Object[]> rekap = StokLokasiUtil.rekapStokLokasiLengkap(session, jenisId, lokasiId, cari);
	for (Object[] r : rekap) {
		double qty = ((Number) r[8]).doubleValue();
		double avg = ((Number) r[11]).doubleValue();
		JSONObject o = new JSONObject();
		o.put("lokasiId", r[0]); o.put("lokasi", r[1] == null ? "" : r[1].toString());
		o.put("jenisId", r[2] == null ? "" : r[2]); o.put("jenis", r[3] == null ? "" : r[3].toString());
		o.put("jenisWarna", r[4] == null ? "" : r[4].toString());
		o.put("produkId", r[5]); o.put("kode", r[6] == null ? "" : r[6].toString()); o.put("nama", r[7] == null ? "" : r[7].toString());
		o.put("qty", qty); o.put("hargaBeli", ((Number) r[9]).doubleValue()); o.put("nilaiBeli", ((Number) r[10]).doubleValue());
		o.put("avgCost", avg); o.put("nilaiAvg", qty * avg);
		rarr.put(o);
	}
	// tren harian 30 hari
	JSONArray tarr = new JSONArray();
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	List<Object[]> tren = StokLokasiUtil.trenMutasiHarian(session, jenisId, 30);
	for (Object[] t : tren) {
		JSONObject o = new JSONObject();
		o.put("tgl", t[0] == null ? "" : df.format((java.util.Date) t[0]));
		o.put("masuk", ((Number) t[1]).doubleValue()); o.put("keluar", ((Number) t[2]).doubleValue());
		tarr.put(o);
	}

	// Fase 2: rollup per Gudang (hierarki pusat/cabang) -- TAMBAHAN, bukan pengganti rekap di atas
	// yang flat per Lokasi. Tidak disaring by lokasiId (rollup ini dimaksudkan lihat seluruh gudang
	// sekaligus). Reuse StokLokasiUtil.rekapStokPerGudang (sama persis dgn versi ZK, tak ada logika baru).
	JSONArray garr = new JSONArray();
	List<Object[]> perGudang = StokLokasiUtil.rekapStokPerGudang(session, jenisId, cari);
	for (Object[] g : perGudang) {
		JSONObject o = new JSONObject();
		o.put("gudangId", g[0] == null ? "" : g[0]);
		o.put("gudangNama", g[1] == null ? "" : g[1].toString());
		o.put("indukNama", g[2] == null ? "" : g[2].toString());
		o.put("jumlahLokasi", ((Number) g[3]).intValue());
		o.put("jumlahProduk", ((Number) g[4]).intValue());
		o.put("nilaiTotal", ((Number) g[5]).doubleValue());
		garr.put(o);
	}

	result.put("status", "00");
	result.put("jenis", jarr); result.put("lokasi", larr); result.put("rekap", rarr); result.put("tren", tarr);
	result.put("perGudang", garr);
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/laporan_pergudangan/service.jsp:82");
	try { result.put("status", "99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_pergudangan/service.jsp:83");}
}
out.print(result.toString());
%>

<%--
  DIAGNOSA (b): Pemetaan Akun Kantin -> Kelompok Laporan.
  Menampilkan akun yang BENAR-BENAR sudah diposting kantin (per Satuan Kerja kantin) lalu menandai
  mana yang SUDAH / BELUM terpetakan di akunting.kelompok_laporan_punya_akun. Akun yang BELUM
  dipetakan TIDAK akan muncul di Neraca/Laba Rugi/Arus Kas.
  Sumber Satuan Kerja: konfigurasi 'satuan_kerja_kantin' (sama dgn dipakai posting kantin).
  // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
  // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
  Sesi: HibernateUtil.currentNativeSession() (dikelola kerangka kerja) — sama pola dgn LaporanKantinUtil.
--%>
<%@page contentType="text/html;charset=UTF-8"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.rab.SatuanKerja"%>
<%@page import="ais.database.model.akunting.Akun"%>
<%@page import="ais.database.model.akunting.KelompokLaporan"%>
<%@page import="ais.database.model.akunting.KelompokLaporanPunyaAkun"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.ui.util.DashboardUiKit"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.List"%>
<%@page import="java.math.BigDecimal"%>
<%
Tbmuser uCP = Common.getCurrentUser(request);
if (uCP == null || uCP.getUserId() == null) { response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("Unauthorized"); return; }

// --- Satuan Kerja kantin (konfigurasi 'satuan_kerja_kantin') ---
Long satkerId = -1L;
String satkerNama = null;
try {
	String v = Common.getKonfigurasi("satuan_kerja_kantin", "").getNilai();
	if (v != null && v.trim().length() > 0) {
		satkerId = Long.valueOf(Long.parseLong(v.trim()));
		SatuanKerja sk = (SatuanKerja) ConstantValues.ambil(SatuanKerja.class.getName(), satkerId);
		if (sk != null) satkerNama = sk.getNama();
	}
} catch (Exception e) { satkerId = -1L; }

// --- Gerbang peran utk "Petakan Cepat" (kunci menu "pemetaan_akun", sama dgn PemetaanAkunHelper /
// PosApi cabang "pemetaan_akun_"). Sebelumnya HANYA cek login (uCP di atas) -- siapa pun yang sudah
// masuk, peran apa pun, dapat MENGUBAH STRUKTUR LAPORAN GLOBAL lewat blok ini. Admin global boleh;
// pengguna tanpa peran dianggap boleh (kompatibilitas akun lama), sama dgn PemetaanAkunHelper.
boolean bolehPetakanCepat;
if (ais.common.Common.getApakahAdminLain(uCP)) {
	bolehPetakanCepat = true;
} else {
	ais.database.model.Tbmrole peranCP = uCP.hakAkses();
	bolehPetakanCepat = peranCP == null || ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(
			peranCP.getEbisnisMenu(), peranCP.getRoleId(), "pemetaan_akun", "create");
}

// --- AKSI PETAKAN CEPAT: simpan mapping akun -> kelompok_laporan (MENGUBAH STRUKTUR LAPORAN GLOBAL) ---
// Aksi tulis hanya lewat POST (form di bawah dipindah dari GET) supaya tautan/gambar dari halaman
// lain tidak bisa memicunya (CSRF via GET); lihat juga gerbang peran bolehPetakanCepat di atas.
String savedMsg = null; boolean savedErr = false;
String aksi = request.getParameter("aksi");
if ("petakan".equals(aksi) && !"POST".equalsIgnoreCase(request.getMethod())) {
	response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	out.print("Metode tidak diizinkan, gunakan POST.");
	return;
} else if ("petakan".equals(aksi) && !bolehPetakanCepat) {
	response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	out.print("Anda tidak memiliki hak memetakan akun. Hubungi admin untuk mengaktifkannya pada Grup Pengguna.");
	return;
} else if ("petakan".equals(aksi)) {
	Long akunId = null, kelompokId = null;
	try { akunId = Long.valueOf(request.getParameter("akunId").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/cek_pemetaan_akun.jsp:46");}
	try { kelompokId = Long.valueOf(request.getParameter("kelompokId").trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/cek_pemetaan_akun.jsp:47");}
	if (akunId == null || kelompokId == null) { savedErr = true; savedMsg = "Akun / Kelompok Laporan tidak valid."; }
	else {
		try {
			Session s2 = HibernateUtil.currentNativeSession();
			Object dup = s2.createSQLQuery("select count(*) from akunting.kelompok_laporan_punya_akun where akun=:a and kelompok_laporan=:k")
				.setParameter("a", akunId).setParameter("k", kelompokId).uniqueResult();
			long jml = (dup == null ? 0 : ((Number) dup).longValue());
			if (jml > 0) { savedMsg = "Akun sudah terpetakan ke Kelompok Laporan tersebut."; }
			else {
				Akun ak = (Akun) s2.get(Akun.class, akunId);
				KelompokLaporan kl = (KelompokLaporan) s2.get(KelompokLaporan.class, kelompokId);
				if (ak == null || kl == null) { savedErr = true; savedMsg = "Akun / Kelompok Laporan tidak ditemukan."; }
				else {
					KelompokLaporanPunyaAkun map = new KelompokLaporanPunyaAkun();
					map.setAkun(ak); map.setKelompokLaporan(kl);
					Common.refreshSaveOrUpdate(s2, map);
					savedMsg = "Akun '" + (ak.getKode()==null?"":ak.getKode()) + " " + (ak.getNama()==null?"":ak.getNama())
						+ "' berhasil dipetakan ke '" + (kl.getKeterangan()==null?"":kl.getKeterangan()) + "'.";
				}
			}
		} catch (Exception e) { savedErr = true; savedMsg = "Gagal menyimpan: " + e.getMessage(); }
	}
}

java.text.DecimalFormat NF = new java.text.DecimalFormat("#,##0");

String sql =
  "select d.id as id, d.kode as kode, d.nama as nama, "
+ "  coalesce(sum(a.debet),0) as debet, coalesce(sum(a.kredit),0) as kredit, "
+ "  (select string_agg(distinct f.keterangan, ', ') "
+ "     from akunting.kelompok_laporan_punya_akun b "
+ "     join akunting.kelompok_laporan c on c.id = b.kelompok_laporan "
+ "     join akunting.jenis_laporan f on f.id = c.jenis_laporan "
+ "    where b.akun = d.id and (c.aktif is null or c.aktif)) as laporan "
+ "from akunting.transaksi a "
+ "join akunting.grup_transaksi a1 on a1.id = a.grup_transaksi "
+ "join akunting.akun d on d.id = a.akun "
+ "where a1.posting_history is not null "
+ "  and ( :satker = -1 or a1.satuan_kerja = :satker ) "
+ "group by d.id, d.kode, d.nama "
+ "order by laporan asc nulls first, d.kode";

List rows = null; String err = null;
try {
	Session sessionKantin = HibernateUtil.currentNativeSession();
	SQLQuery q = sessionKantin.createSQLQuery(sql);
	q.setParameter("satker", satkerId);
	rows = q.list();
} catch (Exception e) { err = String.valueOf(e.getMessage()); }

int total = (rows == null ? 0 : rows.size());
int belum = 0;
if (rows != null) for (Object o : rows) { Object[] r = (Object[]) o; if (r[5] == null) belum++; }

// opsi Kelompok Laporan untuk "Petakan Cepat" (hanya bila ada akun belum dipetakan)
String optHtml = "";
if (belum > 0) {
	try {
		List klOpts = HibernateUtil.currentNativeSession().createSQLQuery(
			"select c.id, c.keterangan, f.keterangan from akunting.kelompok_laporan c "
		  + "join akunting.jenis_laporan f on f.id=c.jenis_laporan "
		  + "where (c.aktif is null or c.aktif) order by f.keterangan, c.urut, c.keterangan").list();
		StringBuilder optSb = new StringBuilder();
		for (Object o : klOpts) { Object[] r = (Object[]) o;
			String id = String.valueOf(r[0]);
			String ket = r[1]==null?"":String.valueOf(r[1]);
			String jn = r[2]==null?"":String.valueOf(r[2]);
			optSb.append("<option value='").append(id).append("'>").append(DashboardUiKit.esc(jn + " › " + ket)).append("</option>");
		}
		optHtml = optSb.toString();
	} catch (Exception e) { optHtml = ""; }
}
%>
<style>
.cpa{max-width:1100px;margin:0 auto;padding:16px;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;color:#0f172a;}
.cpa h2{font-size:18px;font-weight:800;margin:0 0 2px;}
.cpa .sub{color:#64748b;font-size:13px;margin-bottom:12px;}
.cpa .cards{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:14px;}
.cpa .kpi{flex:1;min-width:150px;border:1px solid #e9eef5;border-radius:12px;padding:12px 14px;background:#fff;}
.cpa .kpi .n{font-size:22px;font-weight:800;}
.cpa .kpi .l{font-size:12px;color:#64748b;}
.cpa .kpi.bad{border-color:#fecaca;background:#fff1f2;} .cpa .kpi.bad .n{color:#dc2626;}
.cpa .kpi.ok{border-color:#bbf7d0;background:#f0fdf4;} .cpa .kpi.ok .n{color:#16a34a;}
.cpa .warn{border:1px solid #fde68a;background:#fffbeb;color:#92400e;border-radius:10px;padding:10px 12px;font-size:13px;margin-bottom:12px;}
.cpa .info{border:1px solid #bfdbfe;background:#eff6ff;color:#1e40af;border-radius:10px;padding:10px 12px;font-size:13px;margin-bottom:12px;}
.cpa table{width:100%;border-collapse:collapse;font-size:13px;background:#fff;border:1px solid #e9eef5;border-radius:10px;overflow:hidden;}
.cpa th,.cpa td{padding:7px 10px;border-bottom:1px solid #eef2f7;text-align:left;}
.cpa th{background:#f8fafc;font-weight:700;color:#475569;}
.cpa td.num{text-align:right;font-variant-numeric:tabular-nums;}
.cpa tr.no td{background:#fff7f7;}
.cpa .pill{display:inline-block;padding:1px 8px;border-radius:999px;font-size:11px;font-weight:700;}
.cpa .pill.y{background:#dcfce7;color:#166534;} .cpa .pill.n{background:#fee2e2;color:#991b1b;}
</style>
<div class="cpa">
  <h2><%=Common.getBahasaConfig("Diagnosa Pemetaan Akun Kantin")%></h2>
  <div class="sub">
    <%=Common.getBahasaConfig("Satuan Kerja")%>:
    <b><%= satkerNama != null ? satkerNama : (satkerId.longValue()==-1L ? "(belum diatur)" : ("#"+satkerId)) %></b>
    &middot; <%=Common.getBahasaConfig("akun yang sudah diposting ke jurnal dan status pemetaannya ke Kelompok Laporan.")%>
  </div>

<% if (savedMsg != null) { %>
  <div class="<%= savedErr ? "warn" : "info" %>"><b><%= savedErr ? Common.getBahasaConfig("Gagal:") : Common.getBahasaConfig("Berhasil:") %></b> <%= DashboardUiKit.esc(savedMsg) %></div>
<% } %>

<% if (err != null) { %>
  <div class="warn"><b>Gagal memuat:</b> <%= DashboardUiKit.esc(err) %></div>
<% } else { %>

  <% if (satkerId.longValue() == -1L) { %>
    <div class="warn"><i>&#9888;</i> <%=Common.getBahasaConfig("Konfigurasi 'satuan_kerja_kantin' belum diisi — menampilkan SEMUA akun terposting (belum tersaring ke kantin). Isi konfigurasi tersebut agar diagnosa & laporan tersaring ke Satuan Kerja kantin.")%></div>
  <% } %>

  <div class="cards">
    <div class="kpi"><div class="n"><%=total%></div><div class="l"><%=Common.getBahasaConfig("Akun terposting")%></div></div>
    <div class="kpi <%= belum>0 ? "bad" : "ok" %>"><div class="n"><%=belum%></div><div class="l"><%=Common.getBahasaConfig("Belum dipetakan")%></div></div>
    <div class="kpi ok"><div class="n"><%=(total-belum)%></div><div class="l"><%=Common.getBahasaConfig("Sudah dipetakan")%></div></div>
  </div>

  <% if (belum > 0) { %>
    <div class="warn"><b><%=Common.getBahasaConfig("Perhatian:")%></b>
      <%=belum%> <%=Common.getBahasaConfig("akun BELUM dipetakan → TIDAK muncul di Neraca / Laba Rugi / Arus Kas. Gunakan 'Petakan Cepat' di kolom kanan (pilih Kelompok Laporan → Petakan).")%>
      <br><b><%=Common.getBahasaConfig("Catatan:")%></b> <%=Common.getBahasaConfig("pemetaan berlaku GLOBAL untuk seluruh instansi (bukan hanya kantin) — lakukan hanya bila paham akuntansi.")%></div>
  <% } else if (total > 0) { %>
    <div class="info"><%=Common.getBahasaConfig("Semua akun kantin yang terposting sudah terpetakan ke Kelompok Laporan. Neraca/Laba Rugi/Arus Kas akan memuat angka kantin.")%></div>
  <% } %>

  <% if (total == 0) { %>
    <div class="info"><%=Common.getBahasaConfig("Belum ada transaksi kantin yang terposting untuk Satuan Kerja ini. Lakukan posting (mis. Posting HPP Kantin / posting jurnal) terlebih dahulu.")%></div>
  <% } else { %>
  <table>
    <thead><tr>
      <th><%=Common.getBahasaConfig("Kode")%></th>
      <th><%=Common.getBahasaConfig("Nama Akun")%></th>
      <th style="text-align:right"><%=Common.getBahasaConfig("Debet")%></th>
      <th style="text-align:right"><%=Common.getBahasaConfig("Kredit")%></th>
      <th><%=Common.getBahasaConfig("Status")%></th>
      <th><%=Common.getBahasaConfig("Muncul di Laporan")%></th>
    </tr></thead>
    <tbody>
    <% for (Object o : rows) { Object[] r = (Object[]) o;
         String kode = r[1]==null?"":String.valueOf(r[1]);
         String nama = r[2]==null?"":String.valueOf(r[2]);
         BigDecimal deb = r[3]==null?BigDecimal.ZERO:new BigDecimal(r[3].toString());
         BigDecimal kre = r[4]==null?BigDecimal.ZERO:new BigDecimal(r[4].toString());
         String lap = r[5]==null?null:String.valueOf(r[5]);
         boolean mapped = lap != null;
    %>
      <tr class="<%= mapped?"":"no" %>">
        <td><%= DashboardUiKit.esc(kode) %></td>
        <td><%= DashboardUiKit.esc(nama) %></td>
        <td class="num"><%= NF.format(deb) %></td>
        <td class="num"><%= NF.format(kre) %></td>
        <td><% if (mapped) { %><span class="pill y"><%=Common.getBahasaConfig("Terpetakan")%></span><% } else { %><span class="pill n"><%=Common.getBahasaConfig("Belum")%></span><% } %></td>
        <td>
        <% if (mapped) { %>
          <%= DashboardUiKit.esc(lap) %>
        <% } else if (optHtml.length() > 0 && bolehPetakanCepat) { %>
          <form method="post" action="<%=Common.ROOT%>/baru" style="display:flex;gap:4px;align-items:center;margin:0;flex-wrap:wrap" onsubmit="return confirm('<%=Common.getBahasaConfigJS("Petakan akun ini ke Kelompok Laporan terpilih? Tindakan ini MENGUBAH struktur laporan keuangan GLOBAL (seluruh instansi).")%>');">
            <input type="hidden" name="hanya_tampil_jsp" value="true"/>
            <input type="hidden" name="p" value="kantin"/>
            <input type="hidden" name="s" value="cek_pemetaan_akun"/>
            <input type="hidden" name="aksi" value="petakan"/>
            <input type="hidden" name="akunId" value="<%= r[0] %>"/>
            <select name="kelompokId" style="max-width:230px;padding:3px 6px;border:1px solid #cbd5e1;border-radius:6px;font-size:12px"><%= optHtml %></select>
            <button type="submit" style="padding:3px 10px;border:0;border-radius:6px;background:#b45309;color:#fff;font-size:12px;font-weight:700;cursor:pointer"><%=Common.getBahasaConfig("Petakan")%></button>
          </form>
        <% } else if (optHtml.length() > 0) { %>
          <i style='color:#94a3b8' title='<%=Common.getBahasaConfigJS("Anda tidak memiliki hak memetakan akun.")%>'><%= Common.getBahasaConfig("tidak berhak memetakan") %></i>
        <% } else { %>
          <i style='color:#b91c1c'><%= Common.getBahasaConfig("tidak muncul") %></i>
        <% } %>
        </td>
      </tr>
    <% } %>
    </tbody>
  </table>
  <% } %>

  <div class="sub" style="margin-top:12px">
    <b><%=Common.getBahasaConfig("Cara memetakan:")%></b>
    <%=Common.getBahasaConfig("menu Akunting → Kelompok Laporan / Kelompok Laporan Punya Akun. Untuk kantin, akun HPP diambil dari Kelompok Aset (Akun Beban Pokok Penjualan) dan akun persediaan dari master Aset (Akun Transaksi); pastikan akun-akun itu terdaftar pada baris Laba Rugi (Beban) dan Neraca (Persediaan) yang sesuai.")%>
  </div>

<% } %>
</div>

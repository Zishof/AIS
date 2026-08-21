<%--
  ============================================================================
  Laporan Keuangan e-Kantin (versi JSP) — daftar berkategori + penampil gaya Accurate.
  Tampilan layar: lembar laporan (kop + tabel minimalis + subtotal grup + grand total).
  Unduh PDF: dialihkan ke SERVLET server-side (iText) {ROOT}/LaporanKantinPdf yang
  membuat PDF dengan PENOMORAN HALAMAN di sisi peladen (tidak bergantung skala cetak
  peramban), kop berulang, dan pengelompokan + subtotal.
  Filter: Pedagang/Toko (dikunci utk pedagang), Tanggal, cari Produk/Pelanggan.
  Data layar: laporan_laporan_service.jsp (JSON, lewat LaporanKantinUtil).
  ============================================================================
--%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser tbmuserLap = Common.getCurrentUser(request);
if (tbmuserLap == null || tbmuserLap.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.print("{\"status\":\"error\"}");
    return;
}
String rndLap = Common.getGeneratedBarCode(7);
String svcLap = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin&s=laporan_laporan_service";
String reportsKeuanganJson = "[]";
try { reportsKeuanganJson = ais.action.master.koperasi.helper.LaporanKatalogData.katalogKeuangan().toString(); } catch (Exception eLK) { ais.common.ErrorAuditUtil.record(eLK, "auto-audit(empty-catch) laporan_keuangan.jsp katalogKeuangan"); }
String pdfLap = Common.ROOT + "/LaporanKantinPdf";
// Pintasan ke layar akuntansi ZK mengikuti togel menu yang SAMA dgn Desktop/Android
// (grid TbmroleAction). Fail-closed: tanpa peran, tidak ada pintasan yang muncul.
ais.database.model.Tbmrole roleLap = tbmuserLap.hakAkses();
java.util.Map<String, Boolean> bolehLap = new java.util.HashMap<String, Boolean>();
for (String kunciLap : new String[] { "kode_akun", "posting_hpp", "posting_penjualan",
        "posting_kulakan", "posting_bayar_hutang", "posting_terima_piutang",
        "posting_penyesuaian" }) {
    bolehLap.put(kunciLap, Boolean.valueOf(roleLap != null
            && ais.common.EbisnisMenuKatalog.aksesAkuntansi(roleLap.getEbisnisMenu(),
                    roleLap.getRoleId(), kunciLap)));
}
Toko scopeTokoLap = (tbmuserLap.getPedagang() != null) ? tbmuserLap.getPedagang().getToko() : null;
boolean lockTokoLap = (scopeTokoLap != null);

String namaInstansiLap = "Laporan e-Kantin / Koperasi";
String logoLap = "";
try {
    Object ptObj = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    if (ptObj != null) {
        Object n = ptObj.getClass().getMethod("getNama").invoke(ptObj);
        if (n != null && n.toString().trim().length() > 0) namaInstansiLap = n.toString().trim();
    }
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_laporan.jsp:39");}
try {
    String lg = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    if (lg != null) logoLap = lg.trim();
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_laporan.jsp:43");}

List tokosLap = new ArrayList();
if (!lockTokoLap) {
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    try { tokosLap = HibernateUtil.currentNativeSession().createCriteria(Toko.class).addOrder(Order.asc("nama")).list(); }
    catch (Exception e) { try { tokosLap = HibernateUtil.currentNativeSession().createCriteria(Toko.class).list(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/laporan_laporan.jsp:50");} }
}
%>
<style>
.lapk{--lk-pri:#0d6efd;--lk-mut:#64748b;}
.lapk .lk-cat{font-size:.78rem;font-weight:800;letter-spacing:.05em;text-transform:uppercase;color:var(--lk-mut);margin:18px 0 8px;}
.lapk .lk-card{cursor:pointer;border:1px solid #e9eef5;border-radius:14px;background:#fff;padding:12px 14px;height:100%;
  transition:.15s;display:flex;gap:12px;align-items:flex-start;box-shadow:0 1px 2px rgba(16,24,40,.04);}
.lapk .lk-card:hover{border-color:var(--lk-pri);box-shadow:0 8px 22px rgba(13,110,253,.12);transform:translateY(-1px);}
.lapk .lk-ic{width:40px;height:40px;border-radius:10px;flex:0 0 40px;display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,#e0ecff,#eff5ff);color:var(--lk-pri);font-size:18px;}
.lapk .lk-tt{font-weight:700;font-size:.92rem;color:#0f172a;line-height:1.25;}
.lapk .lk-ds{font-size:.76rem;color:var(--lk-mut);line-height:1.4;margin-top:2px;}
.lapk .lk-filter{background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:14px;}
.lapk .lk-sheet{background:#fff;border:1px solid #e5e9f0;border-radius:6px;padding:20px 26px;box-shadow:0 2px 12px rgba(16,24,40,.06);}
.lapk .lk-kop{text-align:center;border-bottom:2px solid #111827;padding-bottom:8px;margin-bottom:12px;}
.lapk .lk-kop img{height:48px;max-width:180px;object-fit:contain;display:block;margin:0 auto 6px;}
.lapk .lk-kop .ins{font-size:1.04rem;font-weight:800;letter-spacing:.03em;color:#111827;text-transform:uppercase;}
.lapk .lk-kop .ttl{font-size:.96rem;font-weight:700;margin-top:3px;color:#111827;}
.lapk .lk-kop .per{font-size:.8rem;color:#374151;margin-top:3px;}
.lapk table.lk-tbl{width:100%;font-size:.82rem;border-collapse:collapse;font-family:Arial,Helvetica,sans-serif;color:#1f2937;}
.lapk table.lk-tbl thead th{border-top:1.5px solid #111827;border-bottom:1.5px solid #111827;background:#fff;color:#111827;font-weight:700;padding:5px 8px;white-space:nowrap;text-align:left;}
.lapk table.lk-tbl tbody td{border:0;border-bottom:1px solid #eef2f7;padding:4px 8px;vertical-align:top;}
.lapk table.lk-tbl tr.lk-grp td{background:#eef3fb;font-weight:800;color:#0f172a;border-top:1px solid #cbd5e1;border-bottom:1px solid #e5e9f0;padding:5px 8px;}
.lapk table.lk-tbl tr.lk-sub td{font-weight:800;border-top:1px solid #cbd5e1;border-bottom:1px solid #cbd5e1;padding:4px 8px;background:#fafcff;}
.lapk table.lk-tbl tfoot td{border-top:2.5px double #111827;font-weight:800;padding:6px 8px;color:#111827;}
.lapk .num{text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap;}
.lapk .ctr{text-align:center;}
.lapk th.num{text-align:right;} .lapk th.ctr{text-align:center;}
</style>

<div class="lapk" id="lapk<%=rndLap%>">

  <div class="card border-0 shadow-sm rounded-4 mb-3 border-top border-primary border-4">
    <div class="card-body p-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
      <div>
        <h5 class="fw-bold mb-1"><i class="fas fa-folder-open text-primary me-2"></i><%=Common.getBahasaConfig("Laporan Keuangan")%></h5>
        <small class="text-muted"><%=Common.getBahasaConfig("Kumpulan laporan operasional kantin/koperasi. Pilih laporan, atur filter (toko, tanggal, produk, atau pelanggan), lalu tampilkan atau unduh sebagai PDF.")%></small>
      </div>
      <span class="badge bg-light text-dark border" id="ctxToko<%=rndLap%>"></span>
    </div>
  </div>

  <div class="card border-0 shadow-sm rounded-4 mb-3">
    <div class="card-body p-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
      <div>
        <div class="fw-bold"><i class="fas fa-chart-pie text-primary me-2"></i><%=Common.getBahasaConfig("Dasbor Kantin")%></div>
        <small class="text-muted"><%=Common.getBahasaConfig("Gunakan menu Ringkasan e-Kantin versi JSP untuk KPI dan grafik. Halaman ZK tidak lagi dimuat di dalam tampilan JSP.")%></small>
      </div>
      <a class="btn btn-outline-primary btn-sm" href="<%=Common.ROOT%>/baru?p=kantin&s=ringkasan">
        <i class="fas fa-chart-line me-1"></i><%=Common.getBahasaConfig("Buka Ringkasan JSP")%>
      </a>
    </div>
  </div>

  <div class="d-flex flex-wrap gap-2 mb-3" aria-label="Data pendukung laporan akuntansi">
<% if (bolehLap.get("kode_akun").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/akunting/akun.zul">
      <i class="fas fa-sitemap me-1"></i><%=Common.getBahasaConfig("Akun / Perkiraan")%>
    </a>
<% } %>
<% if (bolehLap.get("posting_hpp").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/koperasi/posting_hpp_kantin.zul">
      <i class="fas fa-boxes-stacked me-1"></i><%=Common.getBahasaConfig("Posting HPP")%>
    </a>
<% } %>
<% if (bolehLap.get("posting_penjualan").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/koperasi/posting_penjualan_kantin.zul">
      <i class="fas fa-file-invoice-dollar me-1"></i><%=Common.getBahasaConfig("Posting Penjualan")%>
    </a>
<% } %>
    <%-- Empat posting penutup rantai pengadaan->pembayaran toko; tanpa ini Persediaan hanya
         pernah dikredit jurnal HPP dan Utang Usaha tak pernah terbentuk di Neraca. --%>
<% if (bolehLap.get("posting_kulakan").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/koperasi/posting_kulakan_toko.zul">
      <i class="fas fa-truck-ramp-box me-1"></i><%=Common.getBahasaConfig("Posting Kulakan")%>
    </a>
<% } %>
<% if (bolehLap.get("posting_bayar_hutang").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/koperasi/posting_bayar_hutang_toko.zul">
      <i class="fas fa-money-bill-transfer me-1"></i><%=Common.getBahasaConfig("Posting Bayar Hutang")%>
    </a>
<% } %>
<% if (bolehLap.get("posting_terima_piutang").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/koperasi/posting_terima_piutang_toko.zul">
      <i class="fas fa-hand-holding-dollar me-1"></i><%=Common.getBahasaConfig("Posting Terima Piutang")%>
    </a>
<% } %>
<% if (bolehLap.get("posting_penyesuaian").booleanValue()) { %>
    <a class="btn btn-outline-primary" target="_blank" rel="noopener"
       href="<%=Common.ROOT%>/pages/master/koperasi/posting_penyesuaian_toko.zul">
      <i class="fas fa-sliders me-1"></i><%=Common.getBahasaConfig("Posting Penyesuaian")%>
    </a>
<% } %>
  </div>

  <div id="listView<%=rndLap%>">
    <input type="text" class="form-control shadow-sm mb-2" id="cari<%=rndLap%>" placeholder="<%=Common.getBahasaConfig("Cari laporan...")%>" oninput="lkFilterList<%=rndLap%>()">
    <div id="katWrap<%=rndLap%>"></div>
  </div>

  <div id="reportView<%=rndLap%>" style="display:none;">
    <button class="btn btn-sm btn-outline-secondary rounded-pill mb-3" onclick="lkKembali<%=rndLap%>()"><i class="fas fa-arrow-left me-1"></i><%=Common.getBahasaConfig("Kembali ke Daftar")%></button>
    <div class="card border-0 shadow-sm rounded-4 mb-3 border-top border-success border-4">
      <div class="card-body p-4">
        <h5 class="fw-bold mb-1" id="repJudul<%=rndLap%>"></h5>
        <small class="text-muted" id="repKet<%=rndLap%>"></small>

        <div class="lk-filter mt-3">
          <div class="row g-3 align-items-end">
            <div class="col-md-3">
              <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Pedagang / Toko")%></label>
              <% if (lockTokoLap) { %>
                <input type="hidden" id="fToko<%=rndLap%>" value="<%=scopeTokoLap.getId()%>">
                <input type="text" class="form-control" value="<%=scopeTokoLap.getNama()==null?"":scopeTokoLap.getNama()%>" disabled>
              <% } else { %>
                <select id="fToko<%=rndLap%>" class="form-select">
                  <option value=""><%=Common.getBahasaConfig("-- Semua Toko --")%></option>
                  <% for (int i=0;i<tokosLap.size();i++){ Toko t=(Toko)tokosLap.get(i); %>
                  <option value="<%=t.getId()%>"><%=t.getNama()==null?("Toko #"+t.getId()):t.getNama()%></option>
                  <% } %>
                </select>
              <% } %>
            </div>
            <div class="col-md-2">
              <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
              <input type="date" id="fMulai<%=rndLap%>" class="form-control">
            </div>
            <div class="col-md-2">
              <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Tanggal Sampai")%></label>
              <input type="date" id="fSampai<%=rndLap%>" class="form-control">
            </div>
            <div class="col-md-3" id="wrapProduk<%=rndLap%>" style="display:none;">
              <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Cari Produk (kode / nama)")%></label>
              <input type="text" id="fProduk<%=rndLap%>" class="form-control" placeholder="<%=Common.getBahasaConfig("kode atau nama produk")%>">
            </div>
            <div class="col-md-3" id="wrapPelanggan<%=rndLap%>" style="display:none;">
              <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Cari Pelanggan (kode / nama / member)")%></label>
              <input type="text" id="fPelanggan<%=rndLap%>" class="form-control" placeholder="<%=Common.getBahasaConfig("kode, nama, atau no. identitas")%>">
            </div>
            <% if (!lockTokoLap) { %>
            <div class="col-md-12" id="wrapPerToko<%=rndLap%>" style="display:none;">
              <div class="form-check">
                <input type="checkbox" class="form-check-input" id="fPerToko<%=rndLap%>">
                <label class="form-check-label small fw-bold" for="fPerToko<%=rndLap%>"><%=Common.getBahasaConfig("Per toko / total (kelompokkan hasil per toko bila \"Semua Toko\" dipilih)")%></label>
              </div>
            </div>
            <% } %>
            <div class="col-md-12 d-flex gap-2 justify-content-end">
              <button class="btn btn-primary fw-bold rounded-pill px-4" onclick="lkTampil<%=rndLap%>()"><i class="fas fa-eye me-2"></i><%=Common.getBahasaConfig("Tampilkan")%></button>
              <button class="btn btn-success fw-bold rounded-pill px-4" id="btnCsv<%=rndLap%>" onclick="lkCsv<%=rndLap%>()"><i class="fas fa-file-csv me-2"></i><%=Common.getBahasaConfig("Unduh CSV")%></button>
              <button class="btn btn-danger fw-bold rounded-pill px-4" id="btnPdf<%=rndLap%>" onclick="lkPdf<%=rndLap%>()"><i class="fas fa-file-pdf me-2"></i><%=Common.getBahasaConfig("Unduh PDF")%></button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-3" id="resultArea<%=rndLap%>">
        <div class="text-center text-muted py-5"><i class="fas fa-table fa-2x mb-2 opacity-50 d-block"></i><%=Common.getBahasaConfig("Atur filter lalu klik Tampilkan untuk melihat laporan.")%></div>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var RND = "<%=rndLap%>";
  var SVC = "<%=svcLap%>";
  var PDFURL = "<%=pdfLap%>";
  var LOCK = <%=lockTokoLap%>;
  var TOKO_NAMA = "<%=lockTokoLap ? (scopeTokoLap.getNama()==null?"":scopeTokoLap.getNama().replace("\\","").replace("\"","")) : ""%>";
  var NAMA_INSTANSI = "<%=namaInstansiLap.replace("\\","").replace("\"","")%>";
  var LOGO_URL = "<%=logoLap.replace("\\","").replace("\"","")%>";
  // Peluncur laporan Akuntansi (ZK) — Satuan Kerja kantin dipra-pilih otomatis di dalam form.
  var LAUNCH = "<%=Common.ROOT%>/pages/master/kantin/laporan_keuangan.zul?lap=";
  var DASHAKUN = "<%=Common.ROOT%>/common/display.zul?p=akuntansi";

  var REPORTS = <%= reportsKeuanganJson %>;

  var current = null, lastData = null;

  function el(id){ return document.getElementById(id + RND); }
  function esc(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
  function numv(v){ return typeof v==="number"?v:0; }
  function absUrl(u){ if(!u) return ""; return (/^https?:/i.test(u))?u:(location.origin + (u.charAt(0)==="/"?"":"/") + u); }

  var BLN = ['Jan','Feb','Mar','Apr','Mei','Jun','Jul','Agu','Sep','Okt','Nov','Des'];
  function fmtTglID(s){ if(!s) return ''; var p=String(s).split('-'); if(p.length<3) return s; var m=parseInt(p[1],10); return parseInt(p[2],10)+' '+(BLN[m-1]||p[1])+' '+p[0]; }
  function isCount(label){ return /^(jml|jumlah)\b/i.test(label||''); }
  function fmtAmt(v){ var n=Number(v)||0, s=new Intl.NumberFormat('id-ID',{minimumFractionDigits:2,maximumFractionDigits:2}).format(Math.abs(n)); return n<0?('('+s+')'):s; }
  function fmtInt(v){ var n=Number(v)||0, s=new Intl.NumberFormat('id-ID',{maximumFractionDigits:0}).format(Math.abs(n)); return n<0?('('+s+')'):s; }
  function fmtCell(v,label){ return isCount(label)?fmtInt(v):fmtAmt(v); }
  function periodeText(){ var a=el('fMulai').value,b=el('fSampai').value; if(!a&&!b)return 'Semua Periode'; if(a&&b)return fmtTglID(a)+' s/d '+fmtTglID(b); return a?('Mulai '+fmtTglID(a)):('s/d '+fmtTglID(b)); }
  function tokoText(){ if(LOCK)return TOKO_NAMA||'-'; var s=el('fToko'); return (s&&s.value)?s.options[s.selectedIndex].text:'Semua Toko'; }

  function ctxBadge(){
    var b = el("ctxToko");
    if (LOCK) b.innerHTML = '<i class="fas fa-store me-1"></i>' + esc(TOKO_NAMA);
    else b.innerHTML = '<i class="fas fa-user-shield me-1"></i>Admin · semua toko';
  }

  function renderList(filter){
    var f = (filter||"").toLowerCase();
    var html = "";
    REPORTS.forEach(function(grp){
      var items = grp.items.filter(function(it){
        return !f || it.judul.toLowerCase().indexOf(f)>=0 || (it.ket||"").toLowerCase().indexOf(f)>=0 || grp.kat.toLowerCase().indexOf(f)>=0;
      });
      if (!items.length) return;
      html += '<div class="lk-cat">'+esc(grp.kat)+'</div><div class="row g-2">';
      items.forEach(function(it){
        var isExt = !!it.url;
        var icon = isExt ? 'fa-arrow-up-right-from-square' : 'fa-file-invoice';
        var badge = isExt ? ' <span style="font-size:.62rem;font-weight:700;color:#0d6efd;background:#eaf2ff;border-radius:8px;padding:1px 6px;vertical-align:middle;white-space:nowrap;">&#8599; tab baru</span>' : '';
        html += '<div class="col-md-6 col-xl-4">'
          + '<div class="lk-card" onclick="lkOpen'+RND+'(\''+it.id+'\')">'
          + '<div class="lk-ic"><i class="fas '+icon+'"></i></div>'
          + '<div class="flex-grow-1"><div class="lk-tt">'+esc(it.judul)+badge+'</div>'
          + '<div class="lk-ds">'+esc(it.ket||"")+'</div></div>'
          + '</div></div>';
      });
      html += '</div>';
    });
    if (!html) html = '<div class="text-center text-muted py-5">Tidak ada laporan cocok.</div>';
    el("katWrap").innerHTML = html;
  }
  window["lkFilterList"+RND] = function(){ renderList(el("cari").value); };

  function findReport(id){
    for (var a=0;a<REPORTS.length;a++){ for (var b=0;b<REPORTS[a].items.length;b++){ if(REPORTS[a].items[b].id===id) return REPORTS[a].items[b]; } }
    return null;
  }

  window["lkOpen"+RND] = function(id){
    var rep = findReport(id); if(!rep) return;
    if (rep.url) { window.open(absUrl(rep.url), "_blank"); return; }
    current = rep; lastData = null;
    el("repJudul").textContent = rep.judul;
    el("repKet").textContent = rep.ket||"";
    el("wrapProduk").style.display = rep.produk ? "" : "none";
    el("wrapPelanggan").style.display = rep.pelanggan ? "" : "none";
    var wpt = el("wrapPerToko");
    if (wpt) { wpt.style.display = rep.perToko ? "" : "none"; var cb = el("fPerToko"); if (cb) cb.checked = false; }
    el("resultArea").innerHTML = '<div class="text-center text-muted py-5"><i class="fas fa-table fa-2x mb-2 opacity-50 d-block"></i>Atur filter lalu klik Tampilkan untuk melihat laporan, atau langsung Unduh PDF.</div>';
    el("listView").style.display = "none";
    el("reportView").style.display = "";
    window.scrollTo({top:0,behavior:"smooth"});
  };
  window["lkKembali"+RND] = function(){
    el("reportView").style.display = "none";
    el("listView").style.display = "";
  };

  function filterParams(){
    var p = new URLSearchParams();
    p.append("r", current.id);
    var t = el("fToko"); if (t) p.append("tokoId", t.value||"");
    p.append("tglMulai", el("fMulai").value||"");
    p.append("tglSampai", el("fSampai").value||"");
    if (current.produk && el("fProduk")) p.append("qProduk", el("fProduk").value||"");
    if (current.pelanggan && el("fPelanggan")) p.append("qPelanggan", el("fPelanggan").value||"");
    if (current.perToko && el("fPerToko") && el("fPerToko").checked) p.append("perToko", "true");
    return p;
  }

  window["lkTampil"+RND] = function(){
    if (!current) return;
    var area = el("resultArea");
    area.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';
    fetch(SVC, {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body: filterParams().toString()})
      .then(function(res){ return res.json(); })
      .then(function(d){
        if (d.status !== "00"){ area.innerHTML = '<div class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-2 d-block"></i>'+esc(d.message||"Gagal memuat data.")+'</div>'; return; }
        lastData = d;
        area.innerHTML = buildSheet(d);
      })
      .catch(function(e){ area.innerHTML = '<div class="text-center text-danger py-5">Kesalahan koneksi.</div>'; });
  };

  function buildHead(kol){ var h='<tr>'; for(var i=0;i<kol.length;i++){ h+='<th class="'+(kol[i].t==='num'?'num':(kol[i].t==='tgl'?'ctr':''))+'">'+esc(kol[i].l)+'</th>'; } return h+'</tr>'; }

  function buildBodyFoot(d){
    var kol=d.kolom, gi=(typeof d.grup==='number')?d.grup:-1;
    var grand=[], hasTotal=false;
    for(var c=0;c<kol.length;c++){ grand[c]=0; if(kol[c].t==='num') hasTotal=true; }
    function cellsDetail(row, sub){
      var s='';
      for(var c=0;c<kol.length;c++){ var v=row[c];
        if(gi>=0 && c===gi){ s+='<td></td>'; }
        else if(kol[c].t==='num'){ if(v===null){ s+='<td class="num"></td>'; } else { var n=numv(v); grand[c]+=n; if(sub) sub[c]+=n; s+='<td class="num">'+fmtCell(v,kol[c].l)+'</td>'; } }
        else if(kol[c].t==='tgl'){ s+='<td class="ctr">'+esc(v)+'</td>'; }
        else s+='<td>'+esc(v)+'</td>';
      }
      return s;
    }
    function subtotalRow(key, sub){
      var s='<tr class="lk-sub">';
      for(var c=0;c<kol.length;c++){
        if(c===gi) s+='<td>Subtotal '+esc(key)+'</td>';
        else if(kol[c].t==='num') s+='<td class="num">'+fmtCell(sub[c],kol[c].l)+'</td>';
        else s+='<td></td>';
      }
      return s+'</tr>';
    }
    var body='';
    if(gi>=0 && d.baris.length){
      var curKey=null, started=false, sub=null;
      d.baris.forEach(function(row){
        var key=row[gi];
        if(!started || key!==curKey){
          if(started) body+=subtotalRow(curKey, sub);
          curKey=key; started=true; sub=[]; for(var c=0;c<kol.length;c++) sub[c]=0;
          body+='<tr class="lk-grp"><td colspan="'+kol.length+'">'+esc(key)+'</td></tr>';
        }
        body+='<tr>'+cellsDetail(row, sub)+'</tr>';
      });
      if(started) body+=subtotalRow(curKey, sub);
    } else {
      d.baris.forEach(function(row){ body+='<tr>'+cellsDetail(row, null)+'</tr>'; });
    }
    var foot='';
    if(hasTotal && d.grandTotal!==false){
      foot='<tr>';
      for(var c2=0;c2<kol.length;c2++){
        if(c2===0) foot+='<td>GRAND TOTAL</td>';
        else if(kol[c2].t==='num') foot+='<td class="num">'+fmtCell(grand[c2],kol[c2].l)+'</td>';
        else foot+='<td></td>';
      }
      foot+='</tr>';
    }
    return {body:body, foot:foot};
  }

  function kopHtml(d){
    var logo = LOGO_URL ? ('<img src="'+absUrl(LOGO_URL)+'" alt="">') : '';
    return '<div class="lk-kop">'+logo
      + '<div class="ins">'+esc(NAMA_INSTANSI)+'</div>'
      + '<div class="ttl">'+esc(d.judul)+'</div>'
      + '<div class="per">'+esc(tokoText())+' &nbsp;|&nbsp; Periode: '+esc(periodeText())+'</div></div>';
  }

  function buildSheet(d){
    var kop = '<div class="lk-sheet">'+kopHtml(d);
    if (d.catatan) kop += '<div style="font-size:.76rem;color:#6b7280;font-style:italic;margin-bottom:6px;">'+esc(d.catatan)+'</div>';
    if (!d.baris.length) return kop + '<div class="text-center text-muted py-4">Tidak ada data untuk filter ini.</div></div>';
    var bf = buildBodyFoot(d);
    return kop + '<div class="table-responsive" style="max-height:62vh;overflow:auto;"><table class="lk-tbl"><thead>'
      + buildHead(d.kolom) + '</thead><tbody>' + bf.body + '</tbody>'
      + (bf.foot ? '<tfoot>'+bf.foot+'</tfoot>' : '') + '</table></div>'
      + '<div style="font-size:.72rem;color:#9aa4b2;margin-top:8px;">Jumlah baris: '+d.baris.length+'</div></div>';
  }

  // ---------- Unduh PDF: dibuat di SERVER (iText) dgn penomoran halaman ----------
  window["lkPdf"+RND] = function(){
    if (!current) return;
    var url = PDFURL + "?" + filterParams().toString();
    window.open(url, "_blank");
  };

  // ---------- Unduh CSV (Excel-friendly): ekspor generik utk SEMUA laporan (varian "(CSV)" Accurate) ----------
  function csvVal(v){ var s=(v==null?"":String(v)); return /[",;\n]/.test(s) ? ('"'+s.replace(/"/g,'""')+'"') : s; }
  function dataToCsv(d){
    var lines=[]; var hdr=[];
    for(var i=0;i<d.kolom.length;i++) hdr.push(csvVal(d.kolom[i].l));
    lines.push(hdr.join(";"));
    (d.baris||[]).forEach(function(row){
      var cells=[];
      for(var c=0;c<d.kolom.length;c++){ var v=row[c];
        if(d.kolom[c].t==='num'){ cells.push(v===null||v===undefined?"":String(numv(v))); }
        else cells.push(csvVal(v));
      }
      lines.push(cells.join(";"));
    });
    return lines.join("\r\n");
  }
  function unduhCsv(d){
    var csv="﻿"+dataToCsv(d); // BOM UTF-8 agar Excel benar
    var blob=new Blob([csv],{type:"text/csv;charset=utf-8;"});
    var a=document.createElement("a"); a.href=URL.createObjectURL(blob);
    var nm=((d.judul||"laporan")+"").replace(/[^a-z0-9]+/gi,"_").replace(/^_+|_+$/g,"").toLowerCase()||"laporan";
    a.download=nm+".csv"; document.body.appendChild(a); a.click();
    setTimeout(function(){ document.body.removeChild(a); URL.revokeObjectURL(a.href); }, 120);
  }
  window["lkCsv"+RND] = function(){
    if (!current) return;
    if (current.url){ window.open(absUrl(current.url), "_blank"); return; } // laporan peluncur (ZK): tak ada CSV native
    if (lastData){ unduhCsv(lastData); return; }
    fetch(SVC, {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body: filterParams().toString()})
      .then(function(res){ return res.json(); })
      .then(function(d){ if(d.status!=="00"){ alert(d.message||"Gagal memuat data."); return; } lastData=d; unduhCsv(d); })
      .catch(function(){ alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi. Silakan periksa jaringan Bapak/Ibu dan coba lagi.") %>'); });
  };

  ctxBadge();
  renderList("");
})();
</script>

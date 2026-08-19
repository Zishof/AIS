<%--
  ============================================================================
  Laporan-Laporan e-Kantin (versi JSP) — daftar berkategori + penampil gaya Accurate.
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
String pdfLap = Common.ROOT + "/LaporanKantinPdf";
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
        <h5 class="fw-bold mb-1"><i class="fas fa-folder-open text-primary me-2"></i><%=Common.getBahasaConfig("Laporan-Laporan e-Kantin")%></h5>
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
            <div class="col-md-12" id="wrapStok<%=rndLap%>" style="display:none;">
              <div class="row g-2">
                <div class="col-md-4">
                  <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Kategori / Jenis Barang")%></label>
                  <select id="fJenisProduk<%=rndLap%>" class="form-select"><option value=""><%=Common.getBahasaConfig("-- Semua Kategori --")%></option></select>
                </div>
                <div class="col-md-4">
                  <label class="form-label small fw-bold mb-1"><%=Common.getBahasaConfig("Grup Produk")%></label>
                  <select id="fGrupProduk<%=rndLap%>" class="form-select"><option value=""><%=Common.getBahasaConfig("-- Semua Grup --")%></option></select>
                </div>
                <div class="col-md-4 d-flex align-items-end">
                  <div class="form-check me-3">
                    <input type="checkbox" class="form-check-input" id="fHanyaAktif<%=rndLap%>" checked>
                    <label class="form-check-label small fw-bold" for="fHanyaAktif<%=rndLap%>"><%=Common.getBahasaConfig("Hanya barang aktif")%></label>
                  </div>
                  <div class="form-check">
                    <input type="checkbox" class="form-check-input" id="fStokTidakNol<%=rndLap%>">
                    <label class="form-check-label small fw-bold" for="fStokTidakNol<%=rndLap%>"><%=Common.getBahasaConfig("Sembunyikan stok nol")%></label>
                  </div>
                </div>
              </div>
              <div class="form-text"><%=Common.getBahasaConfig("Saldo dihitung sampai \"Tanggal Sampai\" di atas. Kosongkan tanggal untuk memakai stok terkini.")%></div>
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


<!-- Popup rincian perhitungan: dibuka saat angka laporan diklik (lihat bukaRincian()). -->
<div class="modal fade" id="lkModalRincian" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="lkModalRincianJudul">Rincian</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Tutup"></button>
      </div>
      <div class="modal-body" id="lkModalRincianIsi"></div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Tutup</button>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var RND = "<%=rndLap%>";
  var SVC = "<%=svcLap%>";
  var DATA_URL = "<%=Common.ROOT%>/Data";
  var PDFURL = "<%=pdfLap%>";
  var LOCK = <%=lockTokoLap%>;
  var TOKO_NAMA = "<%=lockTokoLap ? (scopeTokoLap.getNama()==null?"":scopeTokoLap.getNama().replace("\\","").replace("\"","")) : ""%>";
  var NAMA_INSTANSI = "<%=namaInstansiLap.replace("\\","").replace("\"","")%>";
  var LOGO_URL = "<%=logoLap.replace("\\","").replace("\"","")%>";
  // Peluncur laporan Akuntansi (ZK) — Satuan Kerja kantin dipra-pilih otomatis di dalam form.
  var LAUNCH = "<%=Common.ROOT%>/pages/master/kantin/laporan_keuangan.zul?lap=";
  var DASHAKUN = "<%=Common.ROOT%>/common/display.zul?p=akuntansi";

  var REPORTS = [
    {kat:"Penjualan", items:[
      {id:"pnj_barang_laku", judul:"Barang Paling Laku", ket:"Peringkat produk berdasarkan jumlah terjual.", produk:true, perToko:true},
      {id:"pnj_faktur", judul:"Daftar Faktur Penjualan", ket:"Daftar transaksi/faktur penjualan beserta totalnya.", pelanggan:true},
      {id:"pnj_per_barang", judul:"Penjualan per Barang", ket:"Rekap qty & nilai penjualan per produk.", produk:true, perToko:true},
      {id:"pnj_rincian_barang", judul:"Rincian Penjualan per Barang", ket:"Rincian baris penjualan tiap produk (dikelompokkan).", produk:true},
      {id:"pnj_per_pelanggan", judul:"Penjualan Barang per Pelanggan", ket:"Nilai belanja per pelanggan/anggota.", pelanggan:true, perToko:true},
      {id:"pnj_per_kategori_pelanggan", judul:"Penjualan per Kategori Pelanggan", ket:"Nilai penjualan per kategori anggota.", perToko:true},
      {id:"pnj_per_cabang", judul:"Penjualan per Cabang", ket:"Nilai penjualan per toko/merchant."},
      {id:"pnj_per_pemasok", judul:"Penjualan Barang Per Pemasok", ket:"Nilai penjualan dikelompokkan per pemasok.", produk:true, perToko:true},
      {id:"pnj_uang_muka", judul:"Uang Muka Penjualan", ket:"Faktur dengan pembayaran sebagian (uang muka).", pelanggan:true},
      {id:"pnj_detail_transaksi", judul:"Detail Transaksi Penjualan", ket:"Rincian item tiap transaksi POS (per nomor faktur).", produk:true},
      {id:"pnj_per_jam", judul:"Penjualan per Jam", ket:"Omzet & transaksi tiap jam operasional (jam ramai).", perToko:true},
      {id:"pnj_per_kategori", judul:"Penjualan per Kategori", ket:"Rekap penjualan per kategori produk.", perToko:true},
      {id:"retur_barang", judul:"Retur Barang", ket:"Daftar transaksi retur barang.", produk:true}
    ]},
    {kat:"Accurate POS", items:[
      {id:"pos_kasir_harian", judul:"Penjualan per Kasir per Hari", ket:"Rekap penjualan tiap kasir dirinci per tanggal.", perToko:true},
      {id:"pos_harian", judul:"Penjualan Harian (Semua Kasir)", ket:"Total penjualan seluruh kasir per tanggal.", perToko:true},
      {id:"pos_per_kasir", judul:"Laporan Penerimaan Per Kasir", ket:"Penerimaan kas per kasir/operator (akumulasi).", perToko:true},
      {id:"transaksi_per_kasir", judul:"Transaksi Per Kasir", ket:"Rekonsiliasi modal, penerimaan, kas closing, dan selisih per kasir.", perToko:true},
      {id:"pos_per_akun_bank", judul:"Penerimaan Per Akun (Bank/Tunai/E-Money/Voucher)", ket:"Penerimaan per metode/akun pembayaran.", perToko:true}
    ]},
    {kat:"Kasir & Kas", items:[
      {id:"pos_kasir_harian", judul:"Penjualan per Kasir per Hari", ket:"Rekap penjualan tiap kasir dirinci per tanggal."},
      {id:"kas_buka_tutup", judul:"Buka Tutup Kas Kasir", ket:"Sesi kas: modal awal, tunai, non-tunai, uang fisik, selisih."},
      {id:"kas_selisih", judul:"Selisih Kas Kasir", ket:"Sesi kas yang selisih (pengawasan akurasi kas)."},
      {id:"kasir_sering_selisih", judul:"Kasir Sering Selisih", ket:"Peringkat kasir berdasar frekuensi & besar selisih kas."}
    ]},
    {kat:"Pembelian", items:[
      {id:"beli_penerimaan", judul:"Daftar Penerimaan Barang", ket:"Daftar penerimaan barang (kulakan) per faktur.", produk:false},
      {id:"beli_pesanan", judul:"Daftar Pesanan Pembelian", ket:"Ringkasan pembelian per pemasok.", produk:false},
      {id:"beli_rincian_penerimaan", judul:"Rincian Penerimaan Barang", ket:"Rincian item penerimaan (dikelompokkan per pemasok).", produk:true},
      {id:"beli_rincian_pesanan", judul:"Rincian Pesanan Pembelian", ket:"Rincian item pembelian (dikelompokkan per pemasok).", produk:true},
      {id:"beli_faktur", judul:"Faktur Pembelian", ket:"Daftar faktur pembelian per nomor faktur pemasok.", produk:false}
    ]},
    {kat:"Persediaan", items:[
      {id:"sed_barang_jasa", judul:"Daftar Barang dan Jasa", ket:"Master barang/jasa beserta harga & stok.", produk:true},
      {id:"sed_penyesuaian", judul:"Daftar Penyesuaian Stok Barang", ket:"Ringkasan penyesuaian (stok opname) per produk.", produk:true},
      {id:"sed_penyesuaian_rinci", judul:"Penyesuaian Stok Barang (Rinci)", ket:"Rincian selisih penyesuaian (dikelompokkan per produk).", produk:true},
      {id:"sed_stok_harian", judul:"Stok Harian (Nilai Persediaan)", ket:"Posisi stok terkini + nilai persediaan.", produk:true}
    ]},
    {kat:"Gudang", items:[
      {id:"gud_kuantitas", judul:"Kuantitas Barang per Gudang", ket:"Stok kuantitas per gudang/toko.", produk:true},
      {id:"gud_penghitungan", judul:"Lembar Penghitungan Stok", ket:"Lembar kerja penghitungan fisik stok.", produk:true},
      {id:"gud_mutasi_kategori", judul:"Ringkasan Mutasi Gudang By Category", ket:"Mutasi masuk/keluar per kategori produk."},
      {id:"gud_mutasi_item", judul:"Ringkasan Mutasi Gudang By Item", ket:"Mutasi masuk/keluar per item produk.", produk:true},
      {id:"pakai_bahan", judul:"Pemakaian Bahan Baku", ket:"Rincian pemakaian bahan baku (dikelompokkan per produk).", produk:true}
    ]},
    {kat:"Pergudangan — Stok & Posisi", items:[
      {id:"wh_stok_kini", judul:"Stok Saat Ini (per Lokasi)", ket:"Posisi stok tiap lokasi/gudang + nilai persediaan.", produk:true},
      {id:"wh_valuasi", judul:"Valuasi Persediaan per Lokasi", ket:"Total nilai barang diringkas per lokasi/gudang."},
      {id:"wh_stok_minus", judul:"Stok Minus (per Lokasi)", ket:"Barang berstok negatif — deteksi salah input/jual tanpa stok.", produk:true},
      {id:"wh_stok_kosong", judul:"Stok Kosong (per Lokasi)", ket:"Barang yang pernah bergerak namun stoknya kini 0.", produk:true},
      {id:"stok_per_kategori", judul:"Stok per Kategori", ket:"Rekap jumlah & nilai stok per kategori produk.", perToko:true},
      {id:"wh_umur_stok", judul:"Analisa Umur Stok", ket:"Lama sejak barang terakhir masuk (barang lama menumpuk).", produk:true},
      {id:"stok_minimum", judul:"Stok Minimum / Reorder", ket:"Produk yang stoknya mencapai/di bawah batas minimum.", produk:true, perToko:true}
    ]},
    {kat:"Expired, Rusak & Hilang", items:[
      {id:"barang_mendekati_expired", judul:"Barang Mendekati Kadaluarsa", ket:"Produk yang akan kedaluwarsa dalam 60 hari.", produk:true, perToko:true},
      {id:"barang_expired", judul:"Barang Kadaluarsa (Expired)", ket:"Produk yang sudah lewat tanggal kedaluwarsa.", produk:true, perToko:true},
      {id:"barang_rusak", judul:"Barang Rusak", ket:"Barang berkurang saat opname karena rusak/basi/pecah/cacat.", produk:true},
      {id:"barang_hilang", judul:"Barang Hilang", ket:"Barang berkurang saat opname karena hilang/dicuri.", produk:true},
      {id:"nilai_kerugian", judul:"Nilai Kerugian Barang", ket:"Akumulasi kerugian dari kekurangan stok saat opname per produk.", produk:true}
    ]},
    {kat:"Pergudangan — Kartu & Mutasi", items:[
      {id:"stok_per_tanggal", judul:"Stok Barang per Tanggal", ket:"Saldo stok tiap barang pada tanggal tertentu; filter kategori/grup, unduh Excel atau PDF.", produk:true, perToko:true, stokPerTanggal:true},
      {id:"wh_kartu_stok", judul:"Kartu Stok Barang", ket:"Riwayat masuk/keluar/transfer/koreksi + saldo berjalan.", produk:true},
      {id:"wh_mutasi_harian", judul:"Mutasi Stok Harian", ket:"Total barang masuk & keluar tiap hari."},
      {id:"wh_mutasi_barang", judul:"Mutasi Stok per Barang", ket:"Rekap masuk/keluar/perubahan bersih per produk.", produk:true},
      {id:"wh_mutasi_petugas", judul:"Mutasi Stok per Petugas", ket:"Aktivitas mutasi stok per petugas gudang."},
      {id:"wh_mutasi_dokumen", judul:"Mutasi per Dokumen (Referensi)", ket:"Pergerakan stok per nomor referensi/dokumen."}
    ]},
    {kat:"Pergudangan — Masuk / Keluar / Transfer", items:[
      {id:"wh_barang_masuk", judul:"Barang Masuk (per Lokasi)", ket:"Rincian barang masuk ke tiap lokasi/gudang.", produk:true},
      {id:"wh_barang_keluar", judul:"Barang Keluar (per Lokasi)", ket:"Rincian barang keluar dari tiap lokasi/gudang.", produk:true},
      {id:"wh_transfer", judul:"Transfer Stok Antar Lokasi", ket:"Perpindahan barang dari satu lokasi ke lokasi lain.", produk:true}
    ]},
    {kat:"Produksi & Resep (BOM)", items:[
      {id:"resep_bom", judul:"Resep / BOM per Menu", ket:"Komposisi bahan baku tiap menu + subtotal biaya.", produk:true},
      {id:"resep_hpp_menu", judul:"HPP & Margin per Menu (dari Resep)", ket:"HPP dari bahan baku vs harga jual & untung.", produk:true},
      {id:"pakai_bahan", judul:"Pemakaian Bahan Baku", ket:"Rincian pemakaian bahan baku aktual (per produk).", produk:true},
      {id:"kebutuhan_bahan", judul:"Kebutuhan Bahan Baku (Resep x Jual)", ket:"Bahan yang SEHARUSNYA dipakai dari resep x menu terjual."},
      {id:"bahan_selisih_resep", judul:"Selisih Bahan Aktual vs Resep", ket:"Pemakaian aktual vs seharusnya (deteksi pemborosan).", produk:true},
      {id:"realisasi_produksi", judul:"Realisasi Produksi Harian", ket:"Porsi rencana/dibuat/terjual/sisa/waste per hari (menu Produksi Kantin).", produk:true},
      {id:"rekap_produksi", judul:"Rekap Produksi per Menu", ket:"Total rencana/dibuat/terjual/sisa/waste per menu.", produk:true},
      {id:"waste_produksi", judul:"Sisa Makanan / Waste Produksi", ket:"Porsi terbuang/rusak + nilai kerugian.", produk:true}
    ]},
    {kat:"Stock Opname", items:[
      {id:"sed_penyesuaian", judul:"Ringkasan Penyesuaian per Produk", ket:"Jumlah & total selisih opname per produk.", produk:true},
      {id:"sed_penyesuaian_rinci", judul:"Rincian Penyesuaian (Hasil Opname)", ket:"Rincian stok sistem vs fisik & selisih.", produk:true},
      {id:"opname_selisih_nilai", judul:"Selisih Opname (Nilai Terbesar)", ket:"Barang dengan selisih terbesar (qty & nilai).", produk:true},
      {id:"opname_per_toko", judul:"Riwayat Opname per Toko", ket:"Rekap jumlah opname & nilai selisih per toko."},
      {id:"produk_sering_dikoreksi", judul:"Produk Sering Dikoreksi", ket:"Produk yang paling sering dikoreksi saat opname.", produk:true},
      {id:"jadwal_opname", judul:"Jadwal Stock Opname", ket:"Rencana/kegiatan opname beserta status (dari menu Jadwal Opname)."},
      {id:"berita_acara_opname", judul:"Berita Acara Opname (Ringkasan)", ket:"Ringkasan hasil opname per toko: item lebih/kurang & nilai selisih."}
    ]},
    {kat:"Pemusnahan & Penghapusan", items:[
      {id:"pemusnahan", judul:"Pemusnahan / Penghapusan Barang", ket:"Daftar penghapusan/pemusnahan barang + status persetujuan."},
      {id:"pemusnahan_rinci", judul:"Rincian Pemusnahan per Item", ket:"Barang yang dimusnahkan per dokumen.", produk:true}
    ]},
    {kat:"Tenant / Stan", items:[
      {id:"mst_tenant", judul:"Daftar Tenant / Stan", ket:"Daftar tenant/stan/outlet beserta jumlah produk."},
      {id:"pnj_per_cabang", judul:"Penjualan per Tenant/Outlet", ket:"Omzet & transaksi per tenant/toko."},
      {id:"tenant_setoran", judul:"Setoran & Bagi Hasil Tenant", ket:"Kewajiban (bagi hasil+sewa+biaya) & setoran per periode."},
      {id:"tenant_bagi_hasil", judul:"Rekap Bagi Hasil per Tenant", ket:"Total omzet/kewajiban/setoran/sisa per tenant."},
      {id:"tenant_tunggakan", judul:"Tunggakan Tenant", ket:"Tenant dengan setoran kurang dari kewajiban."}
    ]},
    {kat:"Piutang", items:[
      {id:"ar_saldo", judul:"Daftar Saldo Piutang Customer", ket:"Saldo piutang per pelanggan/anggota.", pelanggan:true, perToko:true},
      {id:"ar_faktur_belum_lunas", judul:"Faktur Belum Lunas", ket:"Faktur penjualan yang belum lunas.", pelanggan:true},
      {id:"ar_umur_piutang", judul:"Umur Piutang (Aging)", ket:"Sisa piutang per kelompok umur 0-30/31-60/61-90/>90 hari.", pelanggan:true},
      {id:"ar_sisa_kredit", judul:"Limit & Sisa Kredit Pelanggan", ket:"Plafon kredit anggota vs piutang terpakai; sisa & tanda melebihi limit. Atur limit di Pengaturan.", pelanggan:true}
    ]},
    {kat:"Pajak (PPN)", items:[
      {id:"akn_ppn_rekap", judul:"Rekapitulasi PPN (Keluaran & Masukan)", ket:"Aktivitas akun PPN; Saldo = Kredit - Debet, total baris = PPN Kurang/(Lebih) Bayar. Akun PPN dikenali dari nama akun."},
      {id:"akn_ppn_keluaran", judul:"Rincian PPN Keluaran (Penjualan)", ket:"Baris jurnal pada akun PPN Keluaran — PPN dipungut atas penjualan."},
      {id:"akn_ppn_masukan", judul:"Rincian PPN Masukan (Pembelian)", ket:"Baris jurnal pada akun PPN Masukan — PPN dibayar atas pembelian."}
    ]},
    {kat:"Anggaran (RAB)", items:[
      {id:"akn_anggaran", judul:"Anggaran vs Realisasi (RAB)", ket:"Serapan anggaran RAB (Workspace) Satuan Kerja kantin: Anggaran, Realisasi, Sisa, % Serap. Tahun dari filter tanggal."}
    ]},
    {kat:"Gaji Karyawan (Payroll)", items:[
      {id:"akn_gaji_rincian", judul:"Rincian Gaji Karyawan", ket:"Komponen gaji per karyawan (Payroll seluruh instansi). Periode = tanggal bayar gaji."},
      {id:"akn_gaji_komponen", judul:"Porsi Gaji per Komponen", ket:"Total nilai gaji per komponen (tunjangan, potongan, dll)."},
      {id:"akn_gaji_pph", judul:"Pajak Penghasilan (PPh) Karyawan", ket:"Total PPh/pajak per karyawan dari komponen gaji."}
    ]},
    {kat:"Rekonsiliasi Bank", items:[
      {id:"akn_rekon_ikhtisar", judul:"Ikhtisar Rekonsiliasi Bank", ket:"Saldo Buku (jurnal) vs Saldo Rekening Koran (bank) per akun + Selisih. Entri rekening koran di Pengaturan."},
      {id:"akn_rekon_belum", judul:"Mutasi Rek. Koran Belum Cocok", ket:"Baris rekening koran bank yang belum direkonsiliasi dengan buku."}
    ]},
    {kat:"Buku Besar (Akuntansi)", items:[
      {id:"akn_jurnal", judul:"Keseluruhan Jurnal (Jurnal Umum)", ket:"Semua baris jurnal TERPOSTING Satuan Kerja kantin (dari Jurnal Akuntansi)."},
      {id:"akn_buku_besar", judul:"Rincian Buku Besar (per Akun)", ket:"Mutasi tiap akun + subtotal Debet/Kredit (dari Jurnal Akuntansi)."},
      {id:"akn_histori_bb", judul:"Histori Buku Besar (Saldo Berjalan)", ket:"Mutasi tiap akun + saldo berjalan per baris (dari Jurnal Akuntansi)."},
      {id:"akn_ringkasan_bb", judul:"Ringkasan Buku Besar", ket:"Total Debet, Kredit & Saldo per akun (dari Jurnal Akuntansi)."},
      {id:"akn_neraca_saldo", judul:"Neraca Percobaan (Neraca Saldo)", ket:"Saldo Debet/Kredit per akun; total harus seimbang (dari Jurnal Akuntansi)."},
      {id:"akn_daftar_akun", judul:"Daftar Akun Perkiraan (Bagan Akun)", ket:"Seluruh akun beserta klasifikasi Kelompok Laporan (Neraca/Laba Rugi)."},
      {id:"akn_ringkasan_beban", judul:"Ringkasan Pencatatan Beban", ket:"Total beban per akun (klasifikasi Beban/Biaya/HPP) dari Jurnal Akuntansi."},
      {id:"akn_rincian_beban", judul:"Rincian Beban Pembayaran", ket:"Rincian baris jurnal yang membebani akun Beban (per akun) dari Jurnal Akuntansi."},
      {id:"akn_diagnosa_jurnal_toko", judul:"Diagnosa Jurnal Toko (Belum Diposting)", ket:"Dokumen toko yang belum masuk buku besar per jenis: kulakan, bayar hutang, terima piutang, retur, opname, mutasi."},
      {id:"akn_diagnosa_akun", judul:"Diagnosa Pemetaan Akun", ket:"Akun yang dipakai jurnal TERPOSTING tetapi belum dipetakan ke Kelompok Laporan aktif, sehingga tidak muncul di Laba Rugi/Neraca."},
      {id:"gl_rincian", judul:"Rincian Buku Besar Kas (Ringkas)", ket:"Pembanding cepat dari data transaksi POS, BUKAN dari jurnal. Versi jurnalnya: 'Rekening Koran (Kas & Bank)'."}
    ]},
    {kat:"Kas & Bank (Akuntansi)", items:[
      {id:"akn_rekening_koran", judul:"Rekening Koran (Kas & Bank)", ket:"Mutasi tiap akun Kas/Bank: penerimaan vs pengeluaran (dari Jurnal Akuntansi)."},
      {id:"akn_penerimaan", judul:"Ringkasan Daftar Penerimaan", ket:"Total uang masuk per akun Kas/Bank pada periode (dari Jurnal Akuntansi)."},
      {id:"akn_pembayaran", judul:"Ringkasan Daftar Pembayaran", ket:"Total uang keluar per akun Kas/Bank pada periode (dari Jurnal Akuntansi)."}
    ]},
    {kat:"Keuangan", items:[
      {id:"akn_laba_rugi", judul:"Laba Rugi (Berbasis Jurnal Akuntansi)", ket:"Laba rugi resmi dari jurnal TERPOSTING + klasifikasi akun (Satuan Kerja kantin)."},
      {id:"akn_neraca", judul:"Neraca (Berbasis Jurnal Akuntansi)", ket:"Neraca resmi dari jurnal TERPOSTING (kumulatif s/d Tgl Sampai) + laba berjalan; dilengkapi baris SELISIH."},
      {id:"akn_arus_kas", judul:"Arus Kas (Berbasis Jurnal Akuntansi)", ket:"Mutasi akun Kas/Bank dari jurnal TERPOSTING, diuraikan menurut akun lawan; saldo awal & akhir ikut ditampilkan."},
      {id:"fin_laba_rugi", judul:"Laba Rugi (Ringkas Operasional)", ket:"Pembanding cepat dari data transaksi POS, BUKAN dari jurnal: penjualan − HPP − bahan. Laporan resmi = versi Berbasis Jurnal."},
      {id:"fin_neraca", judul:"Neraca (Ringkas Operasional)", ket:"Pembanding cepat dari data transaksi POS, BUKAN dari jurnal: kas, persediaan, piutang & modal per Tgl Sampai."},
      {id:"fin_arus_kas", judul:"Arus Kas (Ringkas Operasional)", ket:"Pembanding cepat dari data transaksi POS, BUKAN dari jurnal: penerimaan penjualan vs pengeluaran pengadaan."}
    ]},
    {kat:"Laporan Keuangan Resmi — Komparatif (Akuntansi)", items:[
      {id:"lk_keu2",  judul:"Neraca / Laba Rugi / Arus Kas — 2 Periode", ket:"Perbandingan 2 periode (pilih jenis di combo 'Jenis Laporan'). Resmi dari jurnal, Satuan Kerja kantin.", url:LAUNCH+"keu2"},
      {id:"lk_keu12", judul:"Neraca / Laba Rugi / Arus Kas — 12 Bulan", ket:"Kolom 12 bulan berjalan (pilih jenis di combo). Resmi dari jurnal akuntansi.", url:LAUNCH+"keu12"},
      {id:"lk_keu2th",judul:"Neraca / Laba Rugi / Arus Kas — 2 Tahun", ket:"Perbandingan 2 tahun (pilih jenis di combo). Resmi dari jurnal akuntansi.", url:LAUNCH+"keu2th"},
      {id:"lk_neracalajur", judul:"Neraca Lajur (Kertas Kerja)", ket:"Worksheet neraca lajur akuntansi resmi.", url:LAUNCH+"neracalajur"}
    ]},
    {kat:"Buku Besar Resmi (Akuntansi)", items:[
      {id:"lk_trial", judul:"Neraca Saldo / Trial Balance", ket:"Neraca saldo resmi dari jurnal (versi Akuntansi, format JRXML).", url:LAUNCH+"trial"},
      {id:"lk_bukubesar", judul:"Buku Besar", ket:"Buku besar resmi per akun (versi Akuntansi).", url:LAUNCH+"bukubesar"},
      {id:"lk_bukubesartgl", judul:"Buku Besar per Tanggal", ket:"Buku besar resmi difilter tanggal (versi Akuntansi).", url:LAUNCH+"bukubesartgl"},
      {id:"lk_jurnal", judul:"Jurnal Harian", ket:"Daftar jurnal harian resmi (versi Akuntansi).", url:LAUNCH+"jurnal"}
    ]},
    {kat:"Arus Kas & Analisa Keuangan (Akuntansi)", items:[
      {id:"lk_aruskas12", judul:"Arus Kas — 12 Bulan (Kolom)", ket:"Arus kas resmi format kolom 12 bulan.", url:LAUNCH+"aruskas12"},
      {id:"lk_aruskas31", judul:"Arus Kas — 31 Hari (Kolom)", ket:"Arus kas resmi harian 31 hari.", url:LAUNCH+"aruskas31"},
      {id:"lk_dashakun", judul:"Rasio, Grafik, Laba Ditahan & Proyeksi Kas", ket:"Buka Dasbor Akuntansi penuh: rasio keuangan, grafik, laba ditahan, perubahan ekuitas, proyeksi kas.", url:DASHAKUN}
    ]},
    {kat:"Margin, Laba & Analisa", items:[
      {id:"margin_produk", judul:"Margin per Produk/Menu", ket:"Penjualan, HPP, dan laba per produk.", produk:true, perToko:true},
      {id:"margin_kategori", judul:"Margin per Kategori", ket:"Penjualan, HPP, dan laba kotor per kategori.", perToko:true},
      {id:"laba_kotor_harian", judul:"Laba Kotor Harian", ket:"Penjualan dikurangi HPP per hari.", perToko:true},
      {id:"produk_bawah_modal", judul:"Produk Dijual di Bawah Modal", ket:"Transaksi harga jual < harga modal (potensi rugi).", produk:true, perToko:true},
      {id:"slow_moving", judul:"Barang Slow Moving", ket:"Produk paling lambat bergerak (qty terkecil).", produk:true, perToko:true},
      {id:"harga_beli_terakhir", judul:"Harga Beli Terakhir", ket:"Harga modal terakhir tiap produk dari pengadaan.", produk:true},
      {id:"mgr_pertumbuhan", judul:"Pertumbuhan Penjualan (Bulanan)", ket:"Tren omzet & transaksi per bulan.", perToko:true},
      {id:"analisa_diskon", judul:"Analisa Diskon per Produk", ket:"Total diskon yang diberikan tiap produk.", produk:true, perToko:true},
      {id:"kontribusi_produk", judul:"Kontribusi Produk (%)", ket:"Persentase sumbangan tiap produk ke omzet.", produk:true, perToko:true},
      {id:"dead_stock", judul:"Barang Dead Stock / Tidak Laku", ket:"Produk berstok tapi tanpa penjualan pada periode.", produk:true, perToko:true},
      {id:"perputaran_stok", judul:"Perputaran Stok (Turnover)", ket:"Qty terjual dibagi stok saat ini.", produk:true, perToko:true},
      {id:"prediksi_habis", judul:"Prediksi Kehabisan Stok", ket:"Perkiraan hari stok habis dari rata-rata jual 30 hari.", produk:true},
      {id:"rekomendasi_beli", judul:"Rekomendasi Pembelian Ulang", ket:"Produk mau habis <14 hari + saran jumlah beli.", produk:true}
    ]},
    {kat:"Pengadaan (Vendor / PO / BAST)", items:[
      {id:"beli_pr", judul:"Permintaan Pengadaan (PR)", ket:"Daftar Purchase Requisition beserta status persetujuan."},
      {id:"beli_pr_rinci", judul:"Rincian PR per Item", ket:"Item barang tiap PR (dikelompokkan per nomor PR).", produk:true},
      {id:"beli_po", judul:"Pesanan Pembelian (PO)", ket:"Purchase Order ke vendor + status pembayaran."},
      {id:"beli_po_rinci", judul:"Rincian PO per Item", ket:"Item barang tiap PO (dikelompokkan per nomor PO).", produk:true},
      {id:"beli_bast", judul:"Penerimaan Barang (BAST)", ket:"Berita Acara Serah Terima barang dari vendor."},
      {id:"beli_bast_rinci", judul:"Rincian BAST per Item", ket:"Item barang tiap penerimaan/BAST (per nomor BAST).", produk:true},
      {id:"beli_tagihan", judul:"Terima Tagihan / Pembayaran Vendor", ket:"Tagihan vendor: nilai, dibayar, pajak, sisa."},
      {id:"beli_utang_vendor", judul:"Utang Vendor (Belum Lunas)", ket:"Sisa kewajiban ke tiap vendor."},
      {id:"beli_per_vendor", judul:"Pembelian per Vendor", ket:"Rekap nilai PO per vendor."},
      {id:"beli_retur_pengadaan", judul:"Retur Pengadaan (ke Vendor)", ket:"Retur barang yang dikembalikan ke vendor."},
      {id:"beli_belum_datang", judul:"Barang Belum Diterima (PR)", ket:"Permintaan pengadaan yang barangnya belum lengkap datang."}
    ]},
    {kat:"Deposit / Saldo", items:[
      {id:"saldo_deposit", judul:"Deposit / Top Up Saldo", ket:"Daftar setoran/top up saldo (modul Deposit)."},
      {id:"saldo_deposit_rekap", judul:"Rekap Deposit per Nama", ket:"Total top up per nama/pengguna."},
      {id:"potong_gaji", judul:"Potong Gaji Pegawai", ket:"Potongan gaji pegawai (mis. belanja kantin) dari Transaksi Pegawai. Cari: ketik kantin.", pelanggan:true}
    ]},
    {kat:"Dompet, Tabungan & Voucher", items:[
      {id:"dompet_saldo", judul:"Saldo Tabungan/Dompet per Anggota", ket:"Saldo tabungan tiap anggota/siswa/mahasiswa (Deposit).", pelanggan:true},
      {id:"dompet_per_jenis", judul:"Rekap Tabungan per Jenis", ket:"Total saldo per jenis tabungan."},
      {id:"pencairan_saldo", judul:"Pencairan / Penarikan Saldo", ket:"Pencairan saldo anggota + status.", pelanggan:true},
      {id:"rekap_pencairan", judul:"Rekap Pencairan/Refund", ket:"Rekap pencairan berhasil per cara bayar."},
      {id:"voucher_kantin", judul:"Voucher Kantin (Topup/Expiry)", ket:"Voucher/topup beserta kadaluarsa & status.", pelanggan:true}
    ]},
    {kat:"Konsumsi (Siswa / Mahasiswa)", items:[
      {id:"pnj_per_pelanggan", judul:"Belanja per Siswa/Mahasiswa", ket:"Total belanja per anggota/pelanggan.", pelanggan:true},
      {id:"konsumsi_kelas", judul:"Rekap Belanja per Kelas", ket:"Total belanja kantin per kelas siswa."},
      {id:"konsumsi_prodi", judul:"Rekap Belanja per Prodi/Jurusan", ket:"Total belanja kantin per prodi/jurusan mahasiswa."}
    ]},
    {kat:"Audit", items:[
      {id:"audit_harga", judul:"Audit Perubahan Harga", ket:"Riwayat perubahan harga modal produk (Envers)."},
      {id:"audit_master", judul:"Audit Perubahan Master Barang", ket:"Riwayat perubahan nama produk (Envers)."}
    ]},
    {kat:"Master Data", items:[
      {id:"mst_pemasok", judul:"Daftar Pemasok / Supplier", ket:"Daftar pemasok beserta total pengadaan."},
      {id:"mst_pelanggan", judul:"Daftar Pelanggan / Member", ket:"Daftar anggota/pelanggan koperasi.", pelanggan:true},
      {id:"mst_vendor", judul:"Daftar Vendor / Penyedia", ket:"Master vendor/penyedia dari modul Pengadaan."}
    ]}
  ];

  var current = null, lastData = null;

  function el(id){ return document.getElementById(id + RND); }
  // ---------- Drill-down: angka laporan bisa diklik utk melihat data penyusunnya ----------
  // Peta: id laporan -> { potongan label baris : id laporan rincian }. Menambah drill-down baru
  // cukup mendaftarkannya di sini + membuat cabang laporannya di LaporanKantinUtil, tanpa
  // menyentuh alur render. Pencocokan label memakai "mengandung" agar tahan indentasi/terjemahan.
  var PETA_RINCIAN = {
    "fin_laba_rugi": [
      ["HPP",              "fin_laba_rugi_rincian_hpp"],
      ["Penjualan",        "fin_laba_rugi_rincian_penjualan"],
      ["Pendapatan Bersih","fin_laba_rugi_rincian_penjualan"]
    ]
  };
  function cariRincian(row){
    if (!current || !PETA_RINCIAN[current.id] || !row || row.length===0) return null;
    var label = String(row[0]==null?"":row[0]).trim().toLowerCase();
    if (!label) return null;
    var daftar = PETA_RINCIAN[current.id];
    for (var i=0;i<daftar.length;i++){
      if (label.indexOf(String(daftar[i][0]).toLowerCase()) >= 0) return daftar[i][1];
    }
    return null;
  }
  function bukaRincian(idRincian, judulBaris){
    var modalEl = el("lkModalRincian");
    el("lkModalRincianJudul").textContent = "Rincian: " + judulBaris;
    var isi = el("lkModalRincianIsi");
    isi.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary"></div></div>';
    var m = new bootstrap.Modal(modalEl); m.show();
    var prm = filterParams(); prm.set("r", idRincian);
    fetch(SVC, {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body: prm.toString()})
      .then(function(res){ return res.json(); })
      .then(function(d){
        if (d.status !== "00"){ isi.innerHTML = '<div class="text-danger py-3">'+esc(d.message||"Rincian tidak dapat dimuat.")+'</div>'; return; }
        var html = '';
        if (d.catatan) html += '<div class="alert alert-info small py-2">'+esc(d.catatan)+'</div>';
        html += buildSheet(d);
        isi.innerHTML = html;
      })
      .catch(function(){ isi.innerHTML = '<div class="text-danger py-3">Kesalahan koneksi.</div>'; });
  }
  // Tabel ringkas penyusun angka (dipakai popup sisi-klien) -- memakai metadata kolom
  // laporan yang sedang tampil sehingga format angka/tanggalnya konsisten dgn layar.
  function tabelPenyusun(kol, baris, kolAngka){
    var h = '<div class="table-responsive"><table class="table table-sm table-striped mb-2"><thead><tr>';
    for (var i=0;i<kol.length;i++) h += '<th class="'+(kol[i].t==='num'?'num':'')+'">'+esc(kol[i].l)+'</th>';
    h += '</tr></thead><tbody>';
    var total = 0;
    for (var r=0;r<baris.length;r++){
      h += '<tr>';
      for (var c=0;c<kol.length;c++){
        var v = baris[r][c];
        if (kol[c].t==='num'){ if (c===kolAngka) total += numv(v); h += '<td class="num">'+(v===null?'':fmtCell(v,kol[c].l))+'</td>'; }
        else h += '<td>'+esc(v)+'</td>';
      }
      h += '</tr>';
    }
    h += '</tbody><tfoot><tr class="fw-bold"><td colspan="'+Math.max(1,kolAngka)+'">TOTAL ('+esc(kol[kolAngka]?kol[kolAngka].l:'')+')</td>'
       + '<td class="num">'+fmtCell(total, kol[kolAngka]?kol[kolAngka].l:'')+'</td>'
       + (kol.length-kolAngka-1 > 0 ? '<td colspan="'+(kol.length-kolAngka-1)+'"></td>' : '')
       + '</tr></tfoot></table></div>';
    h += '<div class="text-muted small">'+baris.length+' baris penyusun.</div>';
    return h;
  }

  // Popup "asal angka" utk sel biasa: tampilkan seluruh kolom baris itu + catatan rumus laporan.
  function tabelAsalBaris(kol, row){
    var h = '<table class="table table-sm mb-2"><tbody>';
    for (var c=0;c<kol.length;c++){
      var v = row[c];
      h += '<tr><th style="width:38%">'+esc(kol[c].l)+'</th><td'+(kol[c].t==='num'?' class="num"':'')+'>'
         + (v===null?'':(kol[c].t==='num'?fmtCell(v,kol[c].l):esc(v)))+'</td></tr>';
    }
    return h + '</tbody></table>';
  }

  function tampilkanPopupKlien(judul, isiHtml, catatan){
    var modalEl = el("lkModalRincian");
    el("lkModalRincianJudul").textContent = judul;
    var pre = catatan ? '<div class="alert alert-info small py-2">'+esc(catatan)+'</div>' : '';
    el("lkModalRincianIsi").innerHTML = pre + isiHtml;
    new bootstrap.Modal(modalEl).show();
  }

  document.addEventListener("click", function(ev){
    var a = ev.target && ev.target.closest ? ev.target.closest("a.lk-drill") : null;
    if (!a) return;
    ev.preventDefault();
    var judul = a.getAttribute("data-judul") || "Rincian";
    // 1) Rincian dari server (laporan ringkasan yang punya kueri penyusun sendiri)
    var idRincian = a.getAttribute("data-rincian");
    if (idRincian) { bukaRincian(idRincian, judul); return; }
    if (!lastData || !lastData.kolom) return;
    var kol = lastData.kolom, gi = (typeof lastData.grup==='number') ? lastData.grup : -1;
    var kolAngka = parseInt(a.getAttribute("data-kol")||"0",10);
    // 2) Grand total -> seluruh baris penyusun kolom itu
    var kolTotal = a.getAttribute("data-total");
    if (kolTotal !== null) {
      var ct = parseInt(kolTotal,10);
      tampilkanPopupKlien(judul, tabelPenyusun(kol, lastData.baris, ct),
        "Seluruh baris yang dijumlahkan menjadi angka ini.");
      return;
    }
    // 3) Subtotal grup -> baris dalam grup tsb
    var grup = a.getAttribute("data-grup");
    if (grup !== null && gi >= 0) {
      var anggota = lastData.baris.filter(function(r){ return String(r[gi]) === grup; });
      tampilkanPopupKlien(judul, tabelPenyusun(kol, anggota, kolAngka),
        "Baris yang dijumlahkan menjadi subtotal grup ini.");
      return;
    }
    // 4) Sel biasa -> asal-usul baris + catatan rumus laporan
    var idx = parseInt(a.getAttribute("data-baris")||"-1",10);
    if (idx >= 0 && lastData.baris[idx]) {
      tampilkanPopupKlien(judul, tabelAsalBaris(kol, lastData.baris[idx]), lastData.catatan||"");
    }
  });

  // Isi dropdown Kategori & Grup Produk pada filter "Stok Barang per Tanggal".
  // Memakai endpoint /Data (action sql) yang sama dipakai layar kantin lain; hasil
  // di-cache sekali per pemuatan halaman agar tidak menembak DB tiap ganti laporan.
  var opsiStokSudahDimuat = false;
  function isiOpsiStok(){
    if (opsiStokSudahDimuat) return;
    opsiStokSudahDimuat = true;
    var ambil = function(sql, elId, kosong){
      fetch(DATA_URL, {method:"POST", headers:{"Content-Type":"application/json"},
                       body: JSON.stringify({ action:"sql", sql: sql })})
        .then(function(r){ return r.json(); })
        .then(function(d){
          var sel = el(elId); if (!sel) return;
          var baris = (d && d.data) ? d.data : [];
          var html = '<option value="">' + kosong + '</option>';
          for (var i=0;i<baris.length;i++){
            html += '<option value="' + baris[i].id + '">' + esc(baris[i].nama) + '</option>';
          }
          sel.innerHTML = html;
        })
        .catch(function(){ /* dropdown dibiarkan kosong; laporan tetap bisa dijalankan */ });
    };
    ambil("SELECT id, nama FROM koperasi.jenis_produk WHERE COALESCE(aktif,true) ORDER BY nama",
          "fJenisProduk", "-- Semua Kategori --");
    ambil("SELECT id, nama FROM koperasi.grup_produk WHERE COALESCE(aktif,true) ORDER BY nama",
          "fGrupProduk", "-- Semua Grup --");
  }

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
    var wstok = el("wrapStok");
    if (wstok) { wstok.style.display = rep.stokPerTanggal ? "" : "none"; if (rep.stokPerTanggal) isiOpsiStok(); }
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
    if (current.stokPerTanggal) {
      var jp = el("fJenisProduk"), gp = el("fGrupProduk");
      if (jp && jp.value) p.append("jenisProdukId", jp.value);
      if (gp && gp.value) p.append("grupProdukId", gp.value);
      if (el("fHanyaAktif") && el("fHanyaAktif").checked) p.append("hanyaAktif", "true");
      if (el("fStokTidakNol") && el("fStokTidakNol").checked) p.append("hanyaStokTidakNol", "true");
    }
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
    function cellsDetail(row, sub, idxBaris){
      var s='';
      for(var c=0;c<kol.length;c++){ var v=row[c];
        if(gi>=0 && c===gi){ s+='<td></td>'; }
        else if(kol[c].t==='num'){ if(v===null){ s+='<td class="num"></td>'; } else { var n=numv(v); grand[c]+=n; if(sub) sub[c]+=n;
          var drillId = cariRincian(row); var isiSel = fmtCell(v,kol[c].l);
          // SEMUA angka bisa diklik. Bila laporan punya rincian server (PETA_RINCIAN) dipakai itu;
          // selain itu popup menampilkan asal-usul baris ini (seluruh kolomnya + catatan rumus).
          s+= '<td class="num"><a href="javascript:void(0)" class="lk-drill"'
              + (drillId ? ' data-rincian="'+esc(drillId)+'"' : '')
              + ' data-baris="'+idxBaris+'" data-kol="'+c+'"'
              + ' data-judul="'+esc(String(row[0]||kol[c].l||'').trim())+'"'
              + ' title="Klik untuk melihat data penghitungannya">'+isiSel+'</a></td>'; } }
        else if(kol[c].t==='tgl'){ s+='<td class="ctr">'+esc(v)+'</td>'; }
        else s+='<td>'+esc(v)+'</td>';
      }
      return s;
    }
    function subtotalRow(key, sub){
      var s='<tr class="lk-sub">';
      for(var c=0;c<kol.length;c++){
        if(c===gi) s+='<td>Subtotal '+esc(key)+'</td>';
        else if(kol[c].t==='num') s+='<td class="num"><a href="javascript:void(0)" class="lk-drill" data-grup="'+esc(String(key))+'" data-kol="'+c+'" data-judul="Subtotal '+esc(String(key))+'" title="Klik untuk melihat baris penyusun subtotal">'+fmtCell(sub[c],kol[c].l)+'</a></td>';
        else s+='<td></td>';
      }
      return s+'</tr>';
    }
    var body='';
    if(gi>=0 && d.baris.length){
      var curKey=null, started=false, sub=null;
      d.baris.forEach(function(row, idxAsli){
        var key=row[gi];
        if(!started || key!==curKey){
          if(started) body+=subtotalRow(curKey, sub);
          curKey=key; started=true; sub=[]; for(var c=0;c<kol.length;c++) sub[c]=0;
          body+='<tr class="lk-grp"><td colspan="'+kol.length+'">'+esc(key)+'</td></tr>';
        }
        body+='<tr>'+cellsDetail(row, sub, idxAsli)+'</tr>';
      });
      if(started) body+=subtotalRow(curKey, sub);
    } else {
      d.baris.forEach(function(row, idxAsli){ body+='<tr>'+cellsDetail(row, null, idxAsli)+'</tr>'; });
    }
    var foot='';
    if(hasTotal && d.grandTotal!==false){
      foot='<tr>';
      for(var c2=0;c2<kol.length;c2++){
        if(c2===0) foot+='<td>GRAND TOTAL</td>';
        else if(kol[c2].t==='num') foot+='<td class="num"><a href="javascript:void(0)" class="lk-drill" data-total="'+c2+'" data-judul="Grand Total '+esc(kol[c2].l)+'" title="Klik untuk melihat seluruh baris penyusun">'+fmtCell(grand[c2],kol[c2].l)+'</a></td>';
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

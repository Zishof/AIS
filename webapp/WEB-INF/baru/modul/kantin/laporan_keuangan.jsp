<%--
  Laporan Keuangan (sisi Kantin, versi JSP) — MEMAKAI KEMBALI modul Akunting yang sudah ada.
  Tiap kartu membuka kelas laporan Akunting (ZKoss) di tab baru lewat peluncur
  /pages/master/kantin/laporan_keuangan.zul?lap=<kode>. Data berasal dari jurnal akuntansi,
  jadi angka muncul setelah transaksi kantin DIPOSTING (Posting HPP Kantin / posting jurnal),
  dan di dalam laporan pilih Satuan Kerja & periode kantin.
--%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser uLK = Common.getCurrentUser(request);
if (uLK == null || uLK.getUserId() == null) { response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); out.print("{\"status\":\"error\"}"); return; }
String zulLK = Common.ROOT + "/pages/master/kantin/laporan_keuangan.zul";
String dashLK = Common.ROOT + "/common/display.zul?p=akuntansi";
String cekLK = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin&s=cek_pemetaan_akun";
%>
<style>
.lapkeu .lk-card{cursor:pointer;border:1px solid #e9eef5;border-radius:14px;background:#fff;padding:14px 16px;height:100%;
  transition:.15s;display:flex;gap:12px;align-items:flex-start;box-shadow:0 1px 2px rgba(16,24,40,.04);}
.lapkeu .lk-card:hover{border-color:#0d6efd;box-shadow:0 8px 22px rgba(13,110,253,.12);transform:translateY(-1px);}
.lapkeu .lk-ic{width:44px;height:44px;border-radius:12px;flex:0 0 44px;display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,#e0ecff,#eff5ff);color:#0d6efd;font-size:20px;}
.lapkeu .lk-tt{font-weight:700;color:#0f172a;line-height:1.2;}
.lapkeu .lk-ds{font-size:.78rem;color:#64748b;margin-top:2px;line-height:1.4;}
.lapkeu .lk-cat{font-size:.78rem;font-weight:800;letter-spacing:.05em;text-transform:uppercase;color:#64748b;margin:16px 0 8px;}
</style>

<div class="lapkeu">
  <div class="card border-0 shadow-sm rounded-4 mb-3 border-top border-primary border-4">
    <div class="card-body p-4">
      <h5 class="fw-bold mb-1"><i class="fas fa-scale-balanced text-primary me-2"></i><%=Common.getBahasaConfig("Laporan Keuangan (Akuntansi)")%></h5>
      <small class="text-muted"><%=Common.getBahasaConfig("Laporan keuangan resmi dari modul Akuntansi (Laba Rugi, Neraca, Arus Kas, Buku Besar, dll). Dibuka di tab baru.")%></small>
      <div class="alert alert-info mt-3 mb-0 small">
        <i class="fas fa-circle-info me-1"></i>
        <%=Common.getBahasaConfig("Satuan Kerja kantin OTOMATIS terpilih di laporan (konfigurasi 'satuan_kerja_kantin'). Angka muncul setelah transaksi kantin DIPOSTING ke jurnal (Posting HPP Kantin) & masuk Closing.")%>
      </div>
    </div>
  </div>

  <div class="lk-cat"><%=Common.getBahasaConfig("Laporan Keuangan Pokok (Neraca / Laba Rugi / Arus Kas)")%></div>
  <div class="text-muted small mb-2" style="margin-top:-4px"><i class="fas fa-circle-info me-1"></i><%=Common.getBahasaConfig("Di dalam form, pilih Jenis Laporan: Posisi Keuangan (Neraca), Laba Rugi, atau Arus Kas.")%></div>
  <div class="row g-2">
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=keu2','_blank')">
      <div class="lk-ic"><i class="fas fa-file-invoice-dollar"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Laporan Keuangan — 2 Periode")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Neraca / Laba Rugi / Arus Kas, perbandingan 2 periode (seperti contoh).")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=keu12','_blank')">
      <div class="lk-ic"><i class="fas fa-table-list"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Laporan Keuangan — 12 Bulan")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Neraca / Laba Rugi / Arus Kas, kolom 12 bulan.")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=keu2th','_blank')">
      <div class="lk-ic"><i class="fas fa-calendar"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Laporan Keuangan — 2 Tahun")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Neraca / Laba Rugi / Arus Kas, perbandingan 2 tahun.")%></div></div></div></div>
  </div>

  <div class="lk-cat"><%=Common.getBahasaConfig("Buku & Neraca Lajur")%></div>
  <div class="row g-2">
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=neracalajur','_blank')">
      <div class="lk-ic"><i class="fas fa-table-columns"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Neraca Lajur")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Kertas kerja neraca lajur.")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=trial','_blank')">
      <div class="lk-ic"><i class="fas fa-scale-unbalanced"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Trial Balance (Neraca Saldo)")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Saldo per akun.")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=bukubesar','_blank')">
      <div class="lk-ic"><i class="fas fa-book"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Buku Besar")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Mutasi per akun (buku besar).")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=bukubesartgl','_blank')">
      <div class="lk-ic"><i class="fas fa-book-open"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Buku Besar per Tanggal")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Buku besar difilter tanggal.")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=jurnal','_blank')">
      <div class="lk-ic"><i class="fas fa-list-ol"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Jurnal Harian")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Daftar jurnal harian.")%></div></div></div></div>
  </div>

  <div class="lk-cat"><%=Common.getBahasaConfig("Arus Kas (format kolom)")%></div>
  <div class="row g-2">
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=aruskas12','_blank')">
      <div class="lk-ic"><i class="fas fa-money-bill-transfer"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Arus Kas — 12 Bulan")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Arus kas kolom 12 bulan.")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>?lap=aruskas31','_blank')">
      <div class="lk-ic"><i class="fas fa-money-bill-wave"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Arus Kas — 31 Hari")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Arus kas harian 31 hari.")%></div></div></div></div>
  </div>

  <div class="lk-cat"><%=Common.getBahasaConfig("Lainnya")%></div>
  <div class="row g-2">
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=zulLK%>','_blank')">
      <div class="lk-ic"><i class="fas fa-list"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Menu Laporan Keuangan (ZKoss)")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Buka peluncur laporan versi ZKoss (semua pilihan).")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=dashLK%>','_blank')">
      <div class="lk-ic"><i class="fas fa-gauge-high"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Dasbor Akuntansi Lengkap")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Buka dasbor akuntansi penuh (semua fitur).")%></div></div></div></div>
    <div class="col-md-6 col-xl-4"><div class="lk-card" onclick="window.open('<%=cekLK%>','_blank')" style="border-color:#fde68a;background:#fffdf5">
      <div class="lk-ic" style="background:linear-gradient(135deg,#fef3c7,#fffbeb);color:#b45309"><i class="fas fa-clipboard-check"></i></div>
      <div><div class="lk-tt"><%=Common.getBahasaConfig("Diagnosa Pemetaan Akun")%></div><div class="lk-ds"><%=Common.getBahasaConfig("Cek akun kantin yang belum terpetakan ke Kelompok Laporan (agar muncul di Neraca/Laba Rugi).")%></div></div></div></div>
  </div>
</div>

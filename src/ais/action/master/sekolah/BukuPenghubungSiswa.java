package ais.action.master.sekolah;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Textbox;

import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AktiftasHarianSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Panel Buku Penghubung Digital.
 *
 * Menampilkan catatan harian siswa dalam format yang menyerupai buku
 * penghubung fisik — setiap halaman memuat aktivitas, materi, pesan guru,
 * dan kolom tanggapan orang tua untuk satu hari.
 *
 * Alur: pilih siswa → pilih rentang tanggal → klik Tampilkan → lihat/cetak.
 */
public class BukuPenghubungSiswa extends Div {

    private static final long serialVersionUID = 1L;

    // ── Locale ──────────────────────────────────────────────────────────────
    private static final Locale    LOC_ID = new Locale("id", "ID");
    private static final String[]  NAMA_BULAN = {
        "Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember"
    };
    private static final String[]  NAMA_HARI_SINGKAT = {
        "Ahad","Senin","Selasa","Rabu","Kamis","Jum'at","Sabtu"
    };
    private static final SimpleDateFormat FMT_HARI_PANJANG =
            new SimpleDateFormat("EEEE, dd MMMM yyyy", LOC_ID);
    private static final SimpleDateFormat FMT_PENDEK =
            new SimpleDateFormat("dd MMM yyyy", LOC_ID);

    // ── Komponen UI ──────────────────────────────────────────────────────────
    private AmbilDataSiswaBanbox siswaBanbox;
    private Datebox               tglMulai;
    private Datebox               tglSelesai;
    private Div                   divFilter;
    private Div                   divHasil;

    // ── State ────────────────────────────────────────────────────────────────
    private Siswa siswaTerpilih;

    // ════════════════════════════════════════════════════════════════════════
    // Konstruktor
    // ════════════════════════════════════════════════════════════════════════
    public BukuPenghubungSiswa() {
        setWidth("100%");
        setStyle("background:#f0f4f8;padding:12px;box-sizing:border-box;min-height:400px;");
        renderCss();
        buildFilterUI();
        divHasil = new Div();
        divHasil.setStyle("margin-top:12px;");
        divHasil.setParent(this);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CSS — format buku penghubung fisik
    // ════════════════════════════════════════════════════════════════════════
    private void renderCss() {
        h(this,
            "<style>"
            // Filter card
            + ".bp-filter{background:#fff;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.08);"
            +   "padding:16px 18px;margin-bottom:12px;}"
            + ".bp-filter-title{font-size:13px;font-weight:800;color:#1e3a5f;"
            +   "text-transform:uppercase;letter-spacing:.5px;margin-bottom:4px;}"
            + ".bp-filter-desc{font-size:12px;color:#64748b;margin-bottom:14px;line-height:1.5;}"
            + ".bp-filter-row{display:flex;gap:12px;flex-wrap:wrap;align-items:flex-end;}"
            + ".bp-filter-group{display:flex;flex-direction:column;gap:4px;min-width:160px;flex:1 1 160px;}"
            + ".bp-filter-label{font-size:11px;font-weight:700;color:#1e3a5f;}"

            // Toolbar tombol cetak
            + ".bp-toolbar{display:flex;gap:8px;align-items:center;margin-bottom:16px;flex-wrap:wrap;}"
            + ".bp-cetak-btn{background:#065f46;color:#fff;border:none;border-radius:8px;"
            +   "padding:8px 18px;font-size:12px;font-weight:700;cursor:pointer;}"
            + ".bp-cetak-btn:hover{background:#047857;}"
            + ".bp-info-label{font-size:11px;color:#64748b;"
            +   "background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:6px 12px;}"

            // HALAMAN BUKU — menyerupai lembar fisik
            + ".bp-halaman{background:#fff;border:1px solid #cbd5e1;"
            +   "border-radius:4px;max-width:720px;margin:0 auto 28px;overflow:hidden;"
            +   "box-shadow:0 4px 20px rgba(0,0,0,.12);page-break-after:always;}"

            // Header halaman
            + ".bp-page-header{text-align:center;border-bottom:2px solid #1e3a5f;padding:12px 16px;"
            +   "background:linear-gradient(135deg,#1e3a5f 0%,#1d4ed8 100%);color:#fff;}"
            + ".bp-page-header-sekolah{font-size:12px;opacity:.85;margin-bottom:4px;"
            +   "text-transform:uppercase;letter-spacing:1px;}"
            + ".bp-page-header-judul{font-size:18px;font-weight:900;letter-spacing:2px;"
            +   "text-transform:uppercase;}"
            + ".bp-page-header-sub{font-size:11px;opacity:.8;margin-top:4px;}"

            // Tabel info atas (Bulan/Hari/Tanggal)
            + ".bp-info-tbl{width:100%;border-collapse:collapse;border-bottom:1px solid #cbd5e1;}"
            + ".bp-info-tbl td{padding:6px 12px;font-size:12px;border-right:1px solid #e2e8f0;}"
            + ".bp-info-tbl td:last-child{border-right:none;}"
            + ".bp-info-tbl .bp-lbl{font-weight:800;color:#1e3a5f;background:#f8fafc;width:80px;"
            +   "vertical-align:middle;}"
            + ".bp-bulan-list{display:flex;gap:4px;flex-wrap:wrap;}"
            + ".bp-bulan-item{width:24px;height:24px;border-radius:4px;display:flex;"
            +   "align-items:center;justify-content:center;font-size:10px;font-weight:700;"
            +   "border:1px solid #e2e8f0;color:#64748b;}"
            + ".bp-bulan-item.aktif{background:#1d4ed8;color:#fff;border-color:#1d4ed8;}"
            + ".bp-hari-tgl{font-size:14px;font-weight:800;color:#1e3a5f;}"
            + ".bp-hari-nama{font-size:11px;color:#64748b;margin-top:2px;}"

            // Body dua kolom: aktivitas | materi
            + ".bp-body{display:flex;gap:0;border-bottom:1px solid #cbd5e1;}"
            + ".bp-col-akt{flex:1.6;border-right:1px solid #cbd5e1;}"
            + ".bp-col-mat{flex:1;}"

            // Tabel aktivitas
            + ".bp-akt-tbl{width:100%;border-collapse:collapse;font-size:11px;}"
            + ".bp-akt-tbl th{background:#1e3a5f;color:#fff;padding:7px 8px;"
            +   "text-align:center;font-size:10px;text-transform:uppercase;letter-spacing:.5px;}"
            + ".bp-akt-tbl th:nth-child(1){width:26px;}"
            + ".bp-akt-tbl th:nth-child(3),.bp-akt-tbl th:nth-child(4){width:34px;}"
            + ".bp-akt-tbl td{padding:6px 8px;border-bottom:1px solid #f1f5f9;vertical-align:middle;}"
            + ".bp-akt-tbl tr:last-child td{border-bottom:none;}"
            + ".bp-akt-tbl .no-col{text-align:center;color:#94a3b8;font-weight:700;}"
            + ".bp-akt-tbl .chk-col{text-align:center;font-size:14px;}"
            + ".bp-chk-ya{color:#16a34a;font-weight:900;}"
            + ".bp-chk-tdk{color:#dc2626;font-weight:900;}"
            + ".bp-chk-kosong{color:#cbd5e1;}"

            // Tabel materi
            + ".bp-mat-tbl{width:100%;border-collapse:collapse;font-size:11px;}"
            + ".bp-mat-tbl th{background:#1e40af;color:#fff;padding:7px 8px;"
            +   "text-align:center;font-size:10px;text-transform:uppercase;letter-spacing:.5px;}"
            + ".bp-mat-tbl th:nth-child(1){width:24px;}"
            + ".bp-mat-tbl td{padding:6px 8px;border-bottom:1px solid #f1f5f9;vertical-align:middle;}"
            + ".bp-mat-tbl tr:last-child td{border-bottom:none;}"
            + ".bp-mat-tbl .no-col{text-align:center;color:#94a3b8;font-weight:700;}"
            + ".bp-nilai-badge{display:inline-block;padding:1px 7px;border-radius:10px;"
            +   "font-size:10px;font-weight:700;}"

            // Baris kosong materi (placeholder)
            + ".bp-mat-kosong{padding:6px 8px;font-size:10px;color:#94a3b8;}"

            // Footer pesan & paraf
            + ".bp-footer{border-collapse:collapse;width:100%;font-size:11px;}"
            + ".bp-footer td{padding:0;border:1px solid #cbd5e1;vertical-align:top;}"
            + ".bp-pesan-cell{width:65%;padding:0;}"
            + ".bp-paraf-cell{width:35%;padding:0;border-left:1px solid #cbd5e1;}"
            + ".bp-footer-lbl{background:#f8fafc;font-weight:800;color:#1e3a5f;"
            +   "padding:6px 10px;font-size:10px;text-transform:uppercase;letter-spacing:.4px;"
            +   "border-bottom:1px solid #e2e8f0;}"
            + ".bp-footer-isi{padding:8px 10px;min-height:52px;font-size:11px;"
            +   "color:#374151;line-height:1.6;}"
            + ".bp-paraf-space{height:52px;display:flex;align-items:flex-end;padding:4px 10px 6px;"
            +   "border-bottom:none;}"
            + ".bp-paraf-nama{font-size:10px;font-weight:700;color:#1e3a5f;border-top:1px solid #1e3a5f;"
            +   "padding-top:4px;min-width:100px;text-align:center;}"

            // Ortu comment area (terintegrasi ke dalam halaman)
            + ".bp-ortu-input{width:100%;border:none;padding:6px 10px;min-height:52px;"
            +   "font-size:11px;font-family:inherit;resize:vertical;line-height:1.6;"
            +   "background:transparent;outline:none;}"
            + ".bp-ortu-input:focus{background:#faf5ff;}"
            + ".bp-simpan-btn{background:#7c3aed;color:#fff;border:none;border-radius:6px;"
            +   "padding:5px 14px;font-size:11px;font-weight:700;cursor:pointer;"
            +   "margin:4px 8px 6px;}"
            + ".bp-simpan-btn:hover{background:#6d28d9;}"
            + ".bp-saved-badge{background:#dcfce7;color:#15803d;border-radius:6px;"
            +   "padding:3px 10px;font-size:10px;font-weight:700;margin:4px 8px;}"

            // Empty state
            + ".bp-empty{text-align:center;padding:48px 20px;color:#64748b;font-size:13px;"
            +   "background:#fff;border-radius:14px;}"

            // Print styles
            + "@media print{"
            +   ".bp-filter{display:none!important;}"
            +   ".bp-toolbar{display:none!important;}"
            +   ".bp-halaman{box-shadow:none;border:1px solid #000;max-width:100%;}"
            +   ".bp-page-header{-webkit-print-color-adjust:exact;print-color-adjust:exact;}"
            +   ".bp-akt-tbl th,.bp-mat-tbl th{-webkit-print-color-adjust:exact;print-color-adjust:exact;}"
            + "}"
            + "@media(max-width:600px){"
            +   ".bp-body{flex-direction:column;}"
            +   ".bp-col-akt{border-right:none;border-bottom:1px solid #cbd5e1;}"
            +   ".bp-halaman{max-width:100%;margin:0 0 20px;}"
            + "}"
            + "</style>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Panel Filter
    // ════════════════════════════════════════════════════════════════════════
    private void buildFilterUI() {
        divFilter = new Div();
        divFilter.setSclass("bp-filter");
        divFilter.setParent(this);

        h(divFilter,
            "<div class='bp-filter-title'>&#128216; Buku Penghubung</div>"
            + "<div class='bp-filter-desc'>"
            + "Pilih siswa dan rentang tanggal, lalu klik <b>Tampilkan</b> untuk melihat dan mencetak "
            + "buku penghubung dalam format yang siap digunakan."
            + "</div>");

        Div row = new Div();
        row.setSclass("bp-filter-row");
        row.setParent(divFilter);

        // ── Siswa ───────────────────────────────────────────────────────────
        Div grpSiswa = new Div();
        grpSiswa.setSclass("bp-filter-group");
        grpSiswa.setParent(row);
        h(grpSiswa, "<div class='bp-filter-label'>&#128100; Siswa</div>");
        siswaBanbox = new AmbilDataSiswaBanbox();
        siswaBanbox.setWidth("100%");
        siswaBanbox.setParent(grpSiswa);

        // Auto-pilih jika login sebagai siswa
        Tbmuser user = Common.getCurrentUser();
        if (user != null && user.getSiswa() != null) {
            siswaTerpilih = user.getSiswa();
            siswaBanbox.setAttribute("siswa", siswaTerpilih);
            siswaBanbox.setValue(siswaTerpilih.getNamaSiswa());
            siswaBanbox.setDisabled(true);
        }

        // ── Tanggal Mulai ────────────────────────────────────────────────────
        Div grpMulai = new Div();
        grpMulai.setSclass("bp-filter-group");
        grpMulai.setParent(row);
        h(grpMulai, "<div class='bp-filter-label'>&#128197; Tanggal Mulai</div>");
        tglMulai = new Datebox();
        tglMulai.setFormat("dd-MM-yyyy");
        tglMulai.setWidth("100%");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        if (tglMulai != null) tglMulai.setValue(cal.getTime());
        tglMulai.setParent(grpMulai);

        // ── Tanggal Selesai ──────────────────────────────────────────────────
        Div grpSelesai = new Div();
        grpSelesai.setSclass("bp-filter-group");
        grpSelesai.setParent(row);
        h(grpSelesai, "<div class='bp-filter-label'>&#128197; Tanggal Selesai</div>");
        tglSelesai = new Datebox();
        tglSelesai.setFormat("dd-MM-yyyy");
        tglSelesai.setWidth("100%");
        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        tglSelesai.setValue(calEnd.getTime());
        tglSelesai.setParent(grpSelesai);

        // ── Tombol ──────────────────────────────────────────────────────────
        Div grpBtn = new Div();
        grpBtn.setSclass("bp-filter-group");
        grpBtn.setStyle("flex:0 0 auto;");
        grpBtn.setParent(row);
        h(grpBtn, "<div class='bp-filter-label'>&nbsp;</div>");
        MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
        btnTampil.setParent(grpBtn);
        btnTampil.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception { onTampilkan(); }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler: Tampilkan buku
    // ════════════════════════════════════════════════════════════════════════
    private void onTampilkan() {
        Common.clear(divHasil);

        Siswa siswa = (Siswa) siswaBanbox.getAttribute("siswa");
        if (siswa == null) {
            h(divHasil, "<div class='bp-empty'>&#9888; Silakan pilih siswa terlebih dahulu.</div>");
            return;
        }

        Date mulai   = tglMulai.getValue();
        Date selesai = tglSelesai.getValue();
        if (mulai == null || selesai == null) {
            h(divHasil, "<div class='bp-empty'>&#9888; Tanggal mulai dan selesai harus diisi.</div>");
            return;
        }
        if (mulai.after(selesai)) {
            h(divHasil, "<div class='bp-empty'>&#9888; Tanggal mulai tidak boleh setelah tanggal selesai.</div>");
            return;
        }

        List<AktiftasHarianSiswa> list = queryData(siswa, mulai, selesai);
        if (list.isEmpty()) {
            h(divHasil,
                "<div class='bp-empty'>&#128214; Tidak ada catatan untuk rentang tanggal ini.<br>"
                + "Coba pilih bulan yang lain.</div>");
            return;
        }

        // Toolbar cetak
        h(divHasil,
            "<div class='bp-toolbar'>"
            + "<button class='bp-cetak-btn' onclick='window.print();'>&#128438; Cetak / Simpan PDF</button>"
            + "<span class='bp-info-label'>"
            +   esc(siswa.getNamaSiswa()) + " &nbsp;|&nbsp; "
            +   esc(FMT_PENDEK.format(mulai)) + " — " + esc(FMT_PENDEK.format(selesai))
            +   " &nbsp;|&nbsp; " + list.size() + " hari"
            + "</span>"
            + "</div>");

        // Render setiap hari sebagai satu halaman buku
        for (AktiftasHarianSiswa akt : list) {
            renderSatuHalaman(siswa, akt);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Query data
    // ════════════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private List<AktiftasHarianSiswa> queryData(Siswa siswa, Date mulai, Date selesai) {
        Calendar c = Calendar.getInstance();
        c.setTime(selesai);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);

        Session session = HibernateUtil.currentSession();
        Criteria cr = session.createCriteria(AktiftasHarianSiswa.class)
                .add(Restrictions.eq("siswa", siswa))
                .add(Restrictions.ge("tanggal", mulai))
                .add(Restrictions.le("tanggal", c.getTime()))
                .addOrder(Order.asc("tanggal"));
        return ConstantValues.simpleList(cr, AktiftasHarianSiswa.class);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Render: satu halaman = satu entry AktiftasHarianSiswa
    // ════════════════════════════════════════════════════════════════════════
    private void renderSatuHalaman(Siswa siswa, final AktiftasHarianSiswa akt) {
        Div halaman = new Div();
        halaman.setSclass("bp-halaman");
        halaman.setParent(divHasil);

        renderPageHeader(halaman, siswa);
        renderInfoBaris(halaman, akt);
        renderBodyDuaKolom(halaman, akt);
        renderPesanFooter(halaman, akt);
    }

    // ── Header halaman ───────────────────────────────────────────────────────
    private void renderPageHeader(Div halaman, Siswa siswa) {
        String namaSekolah = "";
        try {
            if (siswa.getSekolah() != null) namaSekolah = safeStr(siswa.getSekolah().getNama());
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/BukuPenghubungSiswa.java:381");}

        h(halaman,
            "<div class='bp-page-header'>"
            + (namaSekolah.isEmpty() ? "" :
               "<div class='bp-page-header-sekolah'>&#127891; " + esc(namaSekolah) + "</div>")
            + "<div class='bp-page-header-judul'>Buku Penghubung</div>"
            + "<div class='bp-page-header-sub'>"
            +   esc(safeStr(siswa.getNamaSiswa()))
            +   (siswa.getNomorInduk() != null && !siswa.getNomorInduk().isEmpty()
                ? " &nbsp;&bull;&nbsp; NIS: " + esc(siswa.getNomorInduk()) : "")
            + "</div>"
            + "</div>");
    }

    // ── Baris info: Bulan | Hari | Tanggal ───────────────────────────────────
    private void renderInfoBaris(Div halaman, AktiftasHarianSiswa akt) {
        Date tgl = akt.getTanggal();
        Calendar cal = Calendar.getInstance();
        if (tgl != null) cal.setTime(tgl);

        int bulanIdx = cal.get(Calendar.MONTH);       // 0-based
        int hariTgl  = cal.get(Calendar.DAY_OF_MONTH);
        int dowIdx   = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Min
        String namaHari = NAMA_HARI_SINGKAT[Math.max(0, Math.min(6, dowIdx))];

        // Bulan — 12 kotak kecil, aktif = bulan saat ini
        StringBuilder bulanRow = new StringBuilder();
        bulanRow.append("<div class='bp-bulan-list'>");
        for (int i = 0; i < 12; i++) {
            String cls = (i == bulanIdx) ? "bp-bulan-item aktif" : "bp-bulan-item";
            bulanRow.append("<div class='" + cls + "'>" + (i + 1) + "</div>");
        }
        bulanRow.append("</div>");

        h(halaman,
            "<table class='bp-info-tbl'>"
            + "<tr>"
            +   "<td class='bp-lbl'>Bulan</td>"
            +   "<td colspan='2'>" + bulanRow + "</td>"
            + "</tr>"
            + "<tr>"
            +   "<td class='bp-lbl'>Hari / Tgl</td>"
            +   "<td><div class='bp-hari-tgl'>" + namaHari + ", " + hariTgl + " "
            +       esc(NAMA_BULAN[bulanIdx]) + " " + cal.get(Calendar.YEAR) + "</div></td>"
            +   "<td style='width:200px;'>"
            +     (akt.getKeterangan() != null && !akt.getKeterangan().trim().isEmpty()
                  ? "<span style='font-size:10px;color:#64748b;'>"
                  +   "&#128203; " + esc(akt.getKeterangan().trim()) + "</span>"
                  : "")
            +   "</td>"
            + "</tr>"
            + "</table>");
    }

    // ── Body dua kolom: Aktivitas | Materi ──────────────────────────────────
    private void renderBodyDuaKolom(Div halaman, AktiftasHarianSiswa akt) {
        Div body = new Div();
        body.setSclass("bp-body");
        body.setParent(halaman);

        // Kolom kiri: Aktivitas Harian
        Div colAkt = new Div();
        colAkt.setSclass("bp-col-akt");
        colAkt.setParent(body);
        renderTabelAktivitas(colAkt, akt.getAktifitas());

        // Kolom kanan: Materi
        Div colMat = new Div();
        colMat.setSclass("bp-col-mat");
        colMat.setParent(body);
        renderTabelMateri(colMat, akt.getMateri());
    }

    // ── Tabel Aktivitas Harian ────────────────────────────────────────────────
    private void renderTabelAktivitas(Div parent, String aktifitasJson) {
        List<String[]> rows = parseJsonToList(aktifitasJson); // [nama, nilai]

        StringBuilder sb = new StringBuilder();
        sb.append("<table class='bp-akt-tbl'>"
            + "<tr>"
            +   "<th>No</th>"
            +   "<th style='text-align:left;'>Aktivitas Harian</th>"
            +   "<th>YA</th>"
            +   "<th>TIDAK</th>"
            + "</tr>");

        if (rows.isEmpty()) {
            sb.append("<tr><td colspan='4' style='text-align:center;color:#94a3b8;"
                + "font-size:11px;padding:12px;'>— Belum ada data aktivitas —</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String nama  = rows.get(i)[0];
                String nilai = rows.get(i)[1];
                boolean isYa  = "YA".equalsIgnoreCase(nilai);
                boolean isTdk = "TIDAK".equalsIgnoreCase(nilai);

                sb.append("<tr>")
                  .append("<td class='no-col'>").append(i + 1).append(".</td>")
                  .append("<td>").append(esc(nama)).append("</td>")
                  .append("<td class='chk-col'>")
                  .append(isYa  ? "<span class='bp-chk-ya'>&#10003;</span>"
                                : "<span class='bp-chk-kosong'>&#9633;</span>")
                  .append("</td>")
                  .append("<td class='chk-col'>")
                  .append(isTdk ? "<span class='bp-chk-tdk'>&#10007;</span>"
                                : "<span class='bp-chk-kosong'>&#9633;</span>")
                  .append("</td>")
                  .append("</tr>");
            }
        }
        sb.append("</table>");
        h(parent, sb.toString());
    }

    // ── Tabel Materi ─────────────────────────────────────────────────────────
    private void renderTabelMateri(Div parent, String materiJson) {
        List<String[]> rows = parseJsonToList(materiJson); // [nama, nilai]

        StringBuilder sb = new StringBuilder();
        sb.append("<table class='bp-mat-tbl'>"
            + "<tr>"
            +   "<th>No</th>"
            +   "<th style='text-align:left;'>Materi</th>"
            +   "<th>Penilaian</th>"
            + "</tr>");

        if (rows.isEmpty()) {
            sb.append("<tr><td colspan='3' class='bp-mat-kosong' style='text-align:center;'>"
                + "— Belum ada materi —</td></tr>");
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String nama  = rows.get(i)[0];
                String nilai = rows.get(i)[1];
                String warnaHex = nilaiWarna(nilai);

                sb.append("<tr>")
                  .append("<td class='no-col'>").append(i + 1).append("</td>")
                  .append("<td>").append(esc(nama)).append("</td>")
                  .append("<td style='text-align:center;'>")
                  .append(nilai.isEmpty() ? "<span style='color:#94a3b8;'>—</span>"
                          : "<span class='bp-nilai-badge' style='background:").append(warnaHex).append(";'>")
                            .append(esc(nilai)).append("</span>")
                  .append("</td>")
                  .append("</tr>");
            }
        }
        sb.append("</table>");
        h(parent, sb.toString());
    }

    // ── Footer: Pesan Guru + Orang Tua ──────────────────────────────────────
    private void renderPesanFooter(Div halaman, final AktiftasHarianSiswa akt) {
        // Nama pembina
        String namaPembina = "";
        try {
            if (akt.getPembina1() != null) namaPembina = safeStr(akt.getPembina1().getUserNama());
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/BukuPenghubungSiswa.java:538");}

        // Baris 1: Pesan Guru
        h(halaman,
            "<table class='bp-footer'><tbody>"
            // ── Pesan Pembina/Guru ──
            + "<tr>"
            +   "<td class='bp-pesan-cell'>"
            +     "<div class='bp-footer-lbl'>&#128218; Pesan Guru / Pembina</div>"
            +     "<div class='bp-footer-isi'>"
            +       (akt.getPesanPembina() != null && !akt.getPesanPembina().trim().isEmpty()
                    ? esc(akt.getPesanPembina().trim())
                    : "<span style='color:#94a3b8;font-size:10px;'>— Tidak ada pesan —</span>")
            +     "</div>"
            +   "</td>"
            +   "<td class='bp-paraf-cell'>"
            +     "<div class='bp-footer-lbl'>Paraf Guru</div>"
            +     "<div class='bp-paraf-space'>"
            +       "<div class='bp-paraf-nama'>"
            +         (namaPembina.isEmpty() ? "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" : esc(namaPembina))
            +       "</div>"
            +     "</div>"
            +   "</td>"
            + "</tr>"
            + "</tbody></table>");

        // ── Pesan Orang Tua (dengan form simpan) ──
        Div tblOrtu = new Div();
        tblOrtu.setParent(halaman);
        h(tblOrtu,
            "<table class='bp-footer' style='margin-top:-1px;'><tbody><tr>"
            + "<td class='bp-pesan-cell'>"
            +   "<div class='bp-footer-lbl' style='background:#faf5ff;color:#7c3aed;'>"
            +     "&#9997; Pesan Orang Tua / Wali"
            +   "</div>");

        // ZK Textbox agar bisa disimpan
        final Textbox txOrtu = new Textbox();
        txOrtu.setMultiline(true);
        txOrtu.setRows(3);
        txOrtu.setSclass("bp-ortu-input");
        txOrtu.setWidth("100%");
        if (akt.getPesanOrangTua() != null) txOrtu.setValue(akt.getPesanOrangTua().trim());
        txOrtu.setParent(tblOrtu);

        final Long aktId = akt.getId();
        org.zkoss.zul.Button btnSimpan = new org.zkoss.zul.Button("Simpan");
        btnSimpan.setStyle("background:#7c3aed;color:#fff;border:none;border-radius:6px;"
                + "padding:5px 14px;font-size:11px;font-weight:700;cursor:pointer;"
                + "margin:4px 8px 6px;");
        btnSimpan.setParent(tblOrtu);
        btnSimpan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                String val = txOrtu.getValue();
                if (val == null) val = "";
                simpanPesanOrtu(aktId, val.trim());
                MyMessageboxConfig.show("Pesan berhasil disimpan.");
            }
        });

        h(tblOrtu,
            "<td class='bp-paraf-cell'>"
            + "<div class='bp-footer-lbl' style='background:#faf5ff;color:#7c3aed;'>Paraf Orang Tua</div>"
            + "<div class='bp-paraf-space'><div class='bp-paraf-nama'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div></div>"
            + "</td></tr></tbody></table>");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Save pesan orang tua
    // ════════════════════════════════════════════════════════════════════════
    private void simpanPesanOrtu(Long aktId, String pesan) {
        Session sess = HibernateUtil.currentSession();
        Transaction tx = sess.beginTransaction();
        try {
            AktiftasHarianSiswa obj = (AktiftasHarianSiswa) sess.get(AktiftasHarianSiswa.class, aktId);
            if (obj != null) {
                obj.setPesanOrangTua(pesan);
                sess.merge(obj);
                tx.commit();
            } else {
                tx.rollback();
            }
        } catch (Exception ex) {
            try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/BukuPenghubungSiswa.java:621");}
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sekolah/BukuPenghubungSiswa.java:622");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // JSON parser helper — format: {"1":{"nama":"...","nilai":"..."},...}
    // Kembalikan list [nama, nilai] diurutkan by numeric key
    // ════════════════════════════════════════════════════════════════════════
    private static List<String[]> parseJsonToList(String json) {
        List<String[]> result = new ArrayList<String[]>();
        if (json == null || json.trim().isEmpty()) return result;
        try {
            final JSONObject jObj = new JSONObject(json);
            List<String> keys = new ArrayList<String>();
            Iterator<String> iter = jObj.keys();
            while (iter.hasNext()) keys.add(iter.next());
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    try { return Integer.parseInt(a) - Integer.parseInt(b); }
                    catch (NumberFormatException e) { return a.compareTo(b); }
                }
            });
            for (String key : keys) {
                JSONObject item = jObj.getJSONObject(key);
                String nama  = item.optString("nama", "").trim();
                String nilai = item.optString("nilai", "").trim();
                if (!nama.isEmpty()) result.add(new String[]{nama, nilai});
            }
        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/BukuPenghubungSiswa.java:650"); /* ignore malformed JSON */ }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════
    private static void h(org.zkoss.zk.ui.Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String nilaiWarna(String nilai) {
        if (nilai == null || nilai.isEmpty()) return "#f1f5f9";
        String v = nilai.toUpperCase().trim();
        if (v.equals("A") || v.contains("SANGAT BAIK") || v.contains("LANCAR") || v.contains("BAGUS")) {
            return "#dcfce7";
        }
        if (v.equals("B") || v.contains("BAIK")) return "#dbeafe";
        if (v.equals("C") || v.contains("CUKUP")) return "#fef9c3";
        if (v.equals("D") || v.contains("KURANG")) return "#fee2e2";
        return "#f1f5f9";
    }
}

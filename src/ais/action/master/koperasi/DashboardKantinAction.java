package ais.action.master.koperasi;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DashboardCache;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor Kantin (ZK) — konversi & pengembangan dari kumpulan panel JSP di
 * {@code webapp/WEB-INF/baru/modul/kantin/*} menjadi satu dasbor terpadu, responsif,
 * dan cepat.
 *
 * <p>Dibagi menjadi 11 tab: 6 tab analitik asli (Ringkasan, Keuangan &amp; Laba, Produk &amp; Stok,
 * Pelanggan, Transaksi &amp; Pesanan, Resep/HPP &amp; Margin), 1 tab katalog laporan ZK NATIF
 * (Laporan-Laporan, lewat {@link ais.action.master.koperasi.helper.LaporanKantinZkPanel} -- SEBELUMNYA
 * iframe ke {@code laporan_laporan.jsp}, diganti 2026-07-26 krn seluruh Dasbor Kantin TIDAK BOLEH lagi
 * menautkan halaman JSP), dan 4 tab yang memakai ULANG dasbor ZK lain apa adanya via {@link MyInclude} (Ramalan
 * Penjualan, Monitor Promo &amp; Cashback, Stok &amp; Mutasi/rekonsiliasi Aset, Kas Kasir) supaya pimpinan
 * tak perlu tahu menu-menu itu tersebar terpisah. Setiap tab dimuat saat dibuka (lazy) agar tampilan awal ringan.
 * Semua grafik memakai {@link DashboardUiKit} (HTML/CSS/SVG murni — TANPA JFreeChart):
 * kartu, garis tren, batang, donat, dan jaring laba-laba. Tabel rekap ditampilkan sebagai
 * grid ZK lebih dulu, dengan tombol "Unduh Excel" per tabel yang baru menghasilkan berkas
 * Excel asli ketika ditekan.</p>
 *
 * <p><b>Cache berjenjang</b> ({@link DashboardCache}):
 * L1 (90 dtk) menyimpan potongan HTML grafik per-tab agar buka-tutup tab terasa instan;
 * L2 menandai query native ({@code setCacheable}) agar dilayani Hibernate query cache;
 * L3 (30 mnt, dipakai bersama semua pengguna) menyimpan hasil agregat berat per panel —
 * pengguna pertama "membayar", sisanya gratis.</p>
 *
 * <p>Sumber data utama: {@code koperasi.pembelian} (baris item transaksi) yang di-join ke
 * {@code produk}, {@code toko}, dan {@code anggota_koperasi}; serta
 * {@code koperasi.draft_pembelian_anggota_koperasi} untuk pesanan online yang belum dibayar.</p>
 */
public class DashboardKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // ---- ZK auto-wired dari ZUL (id harus cocok) ----
    private Combobox searchPeriode;
    private Combobox searchToko;
    private org.zkoss.zul.Checkbox chkPerToko;
    private Div dashHost;

    // State periode aktif (dipakai untuk kunci cache & klausa WHERE)
    private String periodeSinceCache = "CURRENT_DATE - INTERVAL '29 days'";
    private String periodeKeyCache = "30_Hari_Terakhir";
    private String periodeLabelCache = "30 Hari Terakhir";

    // Pembatasan per-toko: bila pengguna login sebagai pedagang yang TIDAK boleh melihat toko lain,
    // seluruh data dasbor dikunci hanya untuk tokonya (lihat resolveScopeToko()) -- TIDAK BISA diubah
    // lewat searchToko (dikunci di ZUL: combobox disabled). null = boleh melihat semua toko
    // (admin/pusat) -- di sini admin BOLEH memilih satu toko tertentu lewat searchToko untuk
    // menyaring dasbor (nilai yg sama dipakai andToko()/scopeKey() persis seperti kunci otomatis
    // pedagang, jadi seluruh query yg sudah toko-aware otomatis ikut tersaring tanpa perubahan lain).
    private Long scopeTokoId = null;
    private String scopeTokoNama = null;
    private List<Long> roleTokoAksesIds = new ArrayList<Long>();
    /** {@code true} bila {@link #scopeTokoId} berasal dari kunci pedagang (bukan pilihan admin) -- searchToko/chkPerToko disembunyikan. */
    private boolean scopeTerkunciPedagang = false;

    /**
     * "Per Toko" (2026-07-26, permintaan lanjutan): saat dicentang DAN dasbor TIDAK sedang disaring
     * ke satu toko (baik lewat kunci pedagang maupun pilihan searchToko admin), tab Ringkasan
     * menampilkan tabel rincian per toko (memakai ULANG {@link #qPerformaPedagang()}, data yg SAMA
     * dipakai jaring laba-laba &amp; sheet Excel "Performa Pedagang" yg sudah ada -- tidak ada query
     * baru) SEBAGAI TAMBAHAN di atas kartu KPI agregat, bukan menggantikannya. Cakupan checkbox ini
     * SAAT INI baru mencakup tab Ringkasan -- tab lain (Keuangan &amp; Laba, Produk &amp; Stok, dst.)
     * belum ikut berubah tampilannya; perluasan ke tab lain adalah pekerjaan lanjutan terpisah.
     */
    private boolean perTokoAktif = false;

    // ======================== ZK lifecycle ========================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        resolveScopeToko();
        isiToko();

        String judul = scopeTokoId == null ? "Dasbor Kantin" : "Dasbor Kantin — " + scopeTokoNama;
        String desc = "Pusat pemantauan kantin: lihat pemasukan, keuntungan, produk laku/tidak laku, "
                + "stok menipis, pelanggan setia, dan pesanan yang masih berjalan — semua dalam grafik "
                + "yang mudah dibaca. Pilih rentang waktu di atas, lalu buka tab sesuai kebutuhan.";
        if (scopeTerkunciPedagang) {
            desc = "Khusus menampilkan data toko Anda (" + scopeTokoNama
                    + "); data toko/pedagang lain tidak ditampilkan. " + desc;
        } else if (scopeTokoId != null) {
            desc = "Sedang menyaring khusus toko " + scopeTokoNama + " (dipilih lewat \"Cari Toko\" di atas). "
                    + desc;
        }
        DashboardUiKit.attachIntro(comp, judul, desc);
        pasangDrilldownAnalitik();

        isiPeriode();
        buildDashboard();
    }

    /**
     * Mengaktifkan drill-down ringan untuk seluruh kartu/grafik HTML dari DashboardUiKit.
     * Popup menyalin informasi yang benar-benar sedang terlihat, lalu menyediakan unduhan
     * tabel Excel dan cetak PDF tanpa mengubah query atau aturan bisnis dasbor.
     */
    private void pasangDrilldownAnalitik() {
        Clients.evalJavaScript("(function(){if(window.aisDashboardCardDetail)return;"
                + "function e(s){return String(s==null?'':s).replace(/[&<>\\\"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','\\\"':'&quot;',\"'\":'&#39;'}[c];});}"
                + "function table(t,v,h,lines){var rows=v?[['Nilai',v],['Keterangan',h||'-']]:lines.map(function(x,i){return [i+1,x];});return '<table style=\"width:100%;border-collapse:collapse\"><thead><tr><th style=\"border:1px solid #cbd5e1;padding:7px\">No/Ukuran</th><th style=\"border:1px solid #cbd5e1;padding:7px\">Data</th></tr></thead><tbody>'+rows.map(function(r){return '<tr><td style=\"border:1px solid #cbd5e1;padding:7px\">'+e(r[0])+'</td><td style=\"border:1px solid #cbd5e1;padding:7px\">'+e(r[1])+'</td></tr>';}).join('')+'</tbody></table>';}"
                + "window.aisDashboardCardDetail=function(el,ev){if(ev){ev.preventDefault();ev.stopPropagation();}var t=el.getAttribute('data-ais-title')||'Detail Analitik',v=el.getAttribute('data-ais-value')||'',h=el.getAttribute('data-ais-hint')||'',lines=(el.innerText||'').split(/\\n+/).map(function(x){return x.trim();}).filter(function(x){return x&&x!==t&&x!==v&&x!==h;});var tb=table(t,v,h,lines),o=document.createElement('div');o.id='ais-dashboard-detail-overlay';o.style.cssText='position:fixed;z-index:2147483000;inset:0;background:rgba(15,23,42,.55);display:flex;align-items:center;justify-content:center;padding:20px';o.innerHTML='<div style=\"background:#fff;border-radius:14px;max-width:900px;width:100%;max-height:90vh;overflow:auto;padding:18px;box-shadow:0 24px 60px rgba(0,0,0,.3)\"><div style=\"display:flex;justify-content:space-between;gap:12px;align-items:center\"><h2 style=\"margin:0;font-size:18px\">'+e(t)+'</h2><button data-close style=\"border:0;background:#eef2f7;padding:7px 12px;border-radius:8px;cursor:pointer\">Tutup</button></div><div data-table style=\"margin-top:14px\">'+tb+'</div><div style=\"display:flex;justify-content:flex-end;gap:8px;margin-top:14px\"><button data-xls style=\"padding:8px 12px\">Download Excel</button><button data-pdf style=\"padding:8px 12px\">Download PDF</button></div></div>';var old=document.getElementById(o.id);if(old)old.parentNode.removeChild(old);document.body.appendChild(o);o.querySelector('[data-close]').onclick=function(){o.parentNode.removeChild(o);};o.onclick=function(x){if(x.target===o)o.parentNode.removeChild(o);};o.querySelector('[data-xls]').onclick=function(){var a=document.createElement('a'),b=new Blob(['<html><meta charset=\\\"UTF-8\\\"><body><h2>'+e(t)+'</h2>'+tb+'</body></html>'],{type:'application/vnd.ms-excel'});a.href=URL.createObjectURL(b);a.download=t.replace(/[^a-z0-9]+/gi,'-')+'.xls';a.click();setTimeout(function(){URL.revokeObjectURL(a.href);},1000);};o.querySelector('[data-pdf]').onclick=function(){var w=window.open('','_blank');if(!w)return;w.document.write('<html><head><title>'+e(t)+'</title><style>@page{size:A4 landscape}body{font:11px Arial}</style></head><body><h2>'+e(t)+'</h2>'+tb+'</body></html>');w.document.close();setTimeout(function(){w.print();},250);};};"
                + "})();");
    }

    /**
     * Tentukan apakah dasbor harus dikunci ke satu toko saja. Mengikuti pola yang sama dengan
     * modul master lain ({@code ProdukAction}): pengguna yang terkait sebuah toko dan tidak
     * berhak "melihat toko lain" hanya boleh melihat datanya sendiri -- {@link #scopeTerkunciPedagang}
     * ditandai {@code true} di sini supaya {@link #isiToko()} tahu utk menyembunyikan/mengunci
     * searchToko/chkPerToko (pengguna toko TIDAK boleh diberi opsi berpindah melihat toko lain lewat
     * jalur mana pun, termasuk banbox pencarian admin di bawah).
     */
    private void resolveScopeToko() {
        try {
            ais.database.model.Tbmuser user = Common.getCurrentUser();
            ais.database.model.Tbmrole role = user == null ? null : user.hakAkses();
            roleTokoAksesIds = parseRoleTokoAkses(role == null ? null : role.getTokoAksesJson());
            if (!roleTokoAksesIds.isEmpty()) {
                if (roleTokoAksesIds.size() == 1) {
                    org.hibernate.Session session = HibernateUtil.currentSession();
                    ais.database.model.inventory.Toko t = (ais.database.model.inventory.Toko) session
                            .get(ais.database.model.inventory.Toko.class, roleTokoAksesIds.get(0));
                    if (t != null) {
                        scopeTokoId = t.getId();
                        scopeTokoNama = t.getNama();
                        scopeTerkunciPedagang = true;
                    }
                }
                return;
            }
            ais.database.model.inventory.Toko ct = Common.getCurrentToko();
            if (ct != null && ct.getId() != null) {
                Boolean boleh = ct.getBolehMelihatTokolain();
                if (boleh == null || !boleh.booleanValue()) {
                    scopeTokoId = ct.getId();
                    scopeTokoNama = ct.getNama();
                    scopeTerkunciPedagang = true;
                }
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardKantinAction.java:130");
            // bila gagal menentukan toko, jangan kunci (perilaku aman default = tergantung query lain)
        }
    }

    private List<Long> parseRoleTokoAkses(String json) {
        List<Long> ids = new ArrayList<Long>();
        if (json == null || json.trim().isEmpty()) {
            return ids;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                try {
                    ids.add(Long.valueOf(arr.get(i).toString()));
                } catch (Exception ignore) {
                    ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardKantinAction.java:parseRoleTokoAkses");
                }
            }
        } catch (Exception e) {
            ais.common.Common.tampilErrorJikaAdmin(e);
        }
        return ids;
    }

    /**
     * Isi "Cari Toko" (2026-07-26, permintaan lanjutan) -- banbox pencarian toko utk admin/pusat
     * menyaring SELURUH dasbor (7 tab natif di file ini) ke satu toko tertentu, TANPA perlu login
     * sbg pedagang toko itu. Nilai yg dipilih ditampung ke {@link #scopeTokoId}/{@link
     * #scopeTokoNama} -- variabel PERSIS SAMA yg sudah dipakai {@link #andToko(String)}/{@link
     * #scopeKey()} di seluruh query file ini, jadi seluruh 7 tab otomatis ikut tersaring tanpa
     * perubahan query satu pun. Bila {@link #scopeTerkunciPedagang} true (pengguna toko), kotak ini
     * disembunyikan sepenuhnya -- tidak ada jalan memilih toko lain.
     */
    @SuppressWarnings("unchecked")
    private void isiToko() {
        if (searchToko == null) {
            return;
        }
        if (scopeTerkunciPedagang) {
            searchToko.setVisible(false);
            if (chkPerToko != null) {
                chkPerToko.setVisible(false);
            }
            return;
        }
        org.hibernate.Session session = HibernateUtil.currentSession();
        org.hibernate.Criteria criteriaToko = session.createCriteria(ais.database.model.inventory.Toko.class)
                .add(org.hibernate.criterion.Restrictions.eq("aktif", true));
        if (roleTokoAksesIds != null && !roleTokoAksesIds.isEmpty()) {
            criteriaToko.add(Restrictions.in("id", roleTokoAksesIds));
        }
        List<ais.database.model.inventory.Toko> semua = criteriaToko
                .addOrder(org.hibernate.criterion.Order.asc("nama")).list();
        Comboitem semuaToko = new Comboitem("== Semua Toko ==");
        semuaToko.setParent(searchToko);
        searchToko.setSelectedItem(semuaToko);
        for (ais.database.model.inventory.Toko t : semua) {
            Comboitem it = new Comboitem(t.getNama());
            it.setValue(t);
            it.setParent(searchToko);
        }
        searchToko.setReadonly(true);
    }

    /** Dipicu saat admin memilih toko di "Cari Toko" -- lihat javadoc {@link #isiToko()}. */
    public void onGantiToko(Event event) throws Exception {
        if (scopeTerkunciPedagang || searchToko == null || searchToko.getSelectedItem() == null) {
            return;
        }
        Object v = searchToko.getSelectedItem().getValue();
        if (v instanceof ais.database.model.inventory.Toko) {
            scopeTokoId = ((ais.database.model.inventory.Toko) v).getId();
            scopeTokoNama = ((ais.database.model.inventory.Toko) v).getNama();
        } else {
            scopeTokoId = null;
            scopeTokoNama = null;
        }
        buildDashboard();
    }

    /** Dipicu saat admin centang/hapus centang "Per Toko" -- lihat javadoc {@link #perTokoAktif}. */
    public void onGantiPerToko(Event event) throws Exception {
        if (chkPerToko == null) {
            return;
        }
        perTokoAktif = chkPerToko.isChecked();
        buildDashboard();
    }

    /** Fragmen SQL pembatas toko untuk alias tertentu; kosong bila boleh melihat semua toko. */
    private String andToko(String alias) {
        return scopeTokoId == null ? "" : (" AND " + alias + ".toko = " + scopeTokoId);
    }

    /** Bagian kunci cache yang membedakan lingkup data (agar data antar-toko/mode per-toko tidak tertukar di cache bersama). */
    private String scopeKey() {
        if (perTokoAktif) {
            return scopeKeyMurni() + ".perToko";
        }
        return scopeKeyMurni();
    }

    private String scopeKeyMurni() {
        return scopeTokoId == null ? "all" : ("t" + scopeTokoId);
    }

    // ======================== Filter periode ========================

    private void isiPeriode() {
        if (searchPeriode == null) {
            return;
        }
        addPeriode("Hari Ini", "CURRENT_DATE", false);
        addPeriode("7 Hari Terakhir", "CURRENT_DATE - INTERVAL '6 days'", false);
        addPeriode("30 Hari Terakhir", "CURRENT_DATE - INTERVAL '29 days'", true);
        addPeriode("Bulan Ini", "DATE_TRUNC('month', CURRENT_DATE)", false);
        addPeriode("6 Bulan Terakhir", "NOW() - INTERVAL '6 months'", false);
        searchPeriode.setReadonly(true);
    }

    private void addPeriode(String label, String sinceExpr, boolean selected) {
        Comboitem it = new Comboitem(label);
        it.setValue(sinceExpr);
        it.setParent(searchPeriode);
        if (selected) {
            searchPeriode.setSelectedItem(it);
        }
    }

    private String periodeSince() {
        if (searchPeriode != null && searchPeriode.getSelectedItem() != null
                && searchPeriode.getSelectedItem().getValue() != null) {
            return (String) searchPeriode.getSelectedItem().getValue();
        }
        return "CURRENT_DATE - INTERVAL '29 days'";
    }

    private String periodeLabel() {
        if (searchPeriode != null && searchPeriode.getSelectedItem() != null) {
            return searchPeriode.getSelectedItem().getLabel();
        }
        return "30 Hari Terakhir";
    }

    /** Klausa WHERE standar untuk tabel pembelian (alias p), termasuk pembatasan toko bila perlu. */
    private String wherePembelian() {
        return "p.aktif = true AND p.waktu >= (" + periodeSinceCache + ")" + andToko("p");
    }

    // ======================== Aksi tombol ========================

    public void onRefresh(Event event) throws Exception {
        buildDashboard();
    }

    /** Unduh seluruh rekap penting dalam satu berkas Excel multi-sheet. */
    public void onDownload(Event event) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook();
            writeSheet(wb, "Produk Terlaris", "Rekap Produk Terlaris - " + periodeLabelCache,
                    new String[] { "Tenant", "Produk", "Qty Terjual", "Omzet" }, new int[] { 0, 0, 1, 2 },
                    qRekapProduk());
            writeSheet(wb, "Pelanggan Terloyal", "Rekap Pelanggan Terloyal - " + periodeLabelCache,
                    new String[] { "Tenant", "Pelanggan", "Frekuensi", "Total Belanja" }, new int[] { 0, 0, 1, 2 },
                    qRekapPelanggan());
            writeSheet(wb, "Performa Pedagang", "Performa Pedagang - " + periodeLabelCache,
                    new String[] { "Tenant", "Omzet", "Transaksi", "Barang Terjual" }, new int[] { 0, 2, 1, 1 },
                    qPerformaPedagang());
            writeSheet(wb, "Peringatan Stok", "Peringatan Stok",
                    new String[] { "Tenant", "Produk", "Sisa Stok" }, new int[] { 0, 0, 1 }, qStok());

            File file = saveWorkbook(wb, "rekap_kantin_semua");
            Filedownload.save(file, XLSX_MIME);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Mohon maaf, gagal menyiapkan berkas Excel. Langkah yang dapat dilakukan: (1) muat ulang halaman dan coba ekspor kembali; (2) pastikan koneksi jaringan stabil; (3) hubungi Administrator jika masalah berlanjut.", "Informasi",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        }
    }

    // ======================== Pembangun dasbor (tab) ========================

    private void buildDashboard() {
        if (dashHost == null) {
            return;
        }
        dashHost.getChildren().clear();
        periodeSinceCache = periodeSince();
        periodeLabelCache = periodeLabel();
        periodeKeyCache = periodeLabelCache.replaceAll("[^a-zA-Z0-9]", "_");

        Tabbox tb = new Tabbox();
        tb.setWidth("100%");
        tb.setStyle("border:0;");
        Tabs tabs = new Tabs();
        tabs.setParent(tb);
        Tabpanels tps = new Tabpanels();
        tps.setParent(tb);

        String[] judulTab = { "Ringkasan", "Peringkat Mitra/Toko", "Keuangan & Laba", "Produk & Stok", "Pelanggan",
                "Transaksi & Pesanan", "Resep, HPP & Margin", "Laporan-Laporan", "Kepatuhan Operasional",
                "Ramalan Penjualan", "Monitor Promo & Cashback", "Stok & Mutasi (Inventory)", "Kas Kasir",
                "Laporan Keuangan", "Analisis Keputusan" };
        Tabpanel[] panels = new Tabpanel[judulTab.length];
        for (int i = 0; i < judulTab.length; i++) {
            Tab t = new Tab(judulTab[i]);
            t.setParent(tabs);
            Tabpanel tp = new ais.ui.util.MyTabpanel();
            tp.setStyle("padding:8px 4px;");
            tp.setParent(tps);
            panels[i] = tp;
            if (i == 0) {
                t.setSelected(true);
            } else {
                lazyTab(t, tp, i);
            }
        }
        tb.setParent(dashHost);

        buildTab(0, panels[0]); // tab pertama dimuat langsung
    }

    /** Pasang pemuatan tab secara lazy: konten dibangun saat tab pertama kali dibuka. */
    private void lazyTab(Tab tab, final Tabpanel panel, final int which) {
        final EventListener loader = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (panel.getChildren().isEmpty()) {
                    buildTab(which, panel);
                }
            }
        };
        tab.addEventListener("onClick", loader);
        tab.addEventListener("onSelect", loader);
    }

    private void buildTab(int which, Tabpanel panel) {
        try {
            switch (which) {
                case 0: buildRingkasan(panel); break;
                case 1: buildLeaderboardMitra(panel); break;
                case 2: buildKeuangan(panel); break;
                case 3: buildProdukStok(panel); break;
                case 4: buildPelanggan(panel); break;
                case 5: buildTransaksiPesanan(panel); break;
                case 6: buildResepHpp(panel); break;
                case 7: buildLaporanLaporan(panel); break;
                case 8: muatDasborLain(panel, "/pages/master/koperasi/dashboard_kepatuhan_kantin.zul", true); break;
                case 9: muatDasborLain(panel, "/pages/master/koperasi/forecast_penjualan.zul", true); break;
                case 10: muatDasborLain(panel, "/pages/master/koperasi/monitor_diskon.zul", true); break;
                case 11: muatDasborLain(panel, "/pages/master/inventory/dashboard_stok_kantin.zul", true); break;
                // Kas Kasir SENGAJA tidak diteruskan tokoId/perToko -- dasbor ini aksi buka/tutup kas
                // milik toko SI PENGGUNA yang login, bukan laporan lintas-toko yang boleh disaring admin.
                case 12: muatDasborLain(panel, "/pages/master/kantin/kas_kasir.zul", false); break;
                case 13: buildLaporanKeuangan(panel); break;
                case 14: buildAnalisisKeputusan(panel); break;
                default: break;
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            panel.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:#dc2626;padding:14px;'>Maaf, data tidak dapat dimuat saat ini.</div>"));
        }
    }

    /** Cetak tampilan analitik aktif; CSS cetak browser mempertahankan grafik SVG. */
    public void onDownloadPdf(Event event) throws Exception {
        Clients.evalJavaScript("(function(){var s=document.getElementById('ais-dashboard-print-style');"
                + "if(!s){s=document.createElement('style');s.id='ais-dashboard-print-style';"
                + "s.innerHTML='@media print{@page{size:A4 landscape;margin:6mm}body{zoom:.58} .ais-crud-filter-actions{display:none!important} .z-panel{break-inside:avoid}}';"
                + "document.head.appendChild(s);}window.print();})();");
    }

    /**
     * Analisis keputusan native ZKoss. Angka berasal dari agregasi database pada
     * periode dan cakupan toko yang sama dengan tab lain. Aturannya sengaja
     * transparan: setiap saran menjelaskan indikator yang memicunya, sehingga
     * pengguna tidak menganggap rekomendasi sebagai keputusan otomatis.
     */
    private void buildAnalisisKeputusan(Tabpanel panel) {
        final String where = wherePembelian();
        Object[] kini = first(q("decision_summary",
                "SELECT COALESCE(SUM(p.total),0),COUNT(DISTINCT COALESCE(p.pembelian_anggota_koperasi,p.id)),"
                + "COALESCE(SUM(p.qty),0),COUNT(DISTINCT NULLIF(TRIM(p.member),'')) "
                + "FROM koperasi.pembelian p WHERE " + where));
        double omzet = kini == null ? 0 : num(kini[0]);
        long transaksi = kini == null ? 0 : (long) num(kini[1]);
        double qty = kini == null ? 0 : num(kini[2]);
        long member = kini == null ? 0 : (long) num(kini[3]);
        Object[] lalu = first(q("decision_previous",
                "SELECT COALESCE(SUM(p.total),0),COUNT(DISTINCT COALESCE(p.pembelian_anggota_koperasi,p.id)) "
                + "FROM koperasi.pembelian p WHERE p.aktif=true AND p.waktu >= ((" + periodeSinceCache
                + ")-(NOW()-(" + periodeSinceCache + "))) AND p.waktu < (" + periodeSinceCache + ")"
                + andToko("p")));
        double omzetLalu = lalu == null ? 0 : num(lalu[0]);
        double pertumbuhan = omzetLalu > 0 ? (omzet - omzetLalu) * 100.0 / omzetLalu : (omzet > 0 ? 100 : 0);

        Object[] invalid = first(q("decision_invalid",
                "SELECT COUNT(*) FROM (SELECT COALESCE(p.pembelian_anggota_koperasi,p.id),MAX(h.total_biaya),SUM(p.total) "
                + "FROM koperasi.pembelian p LEFT JOIN koperasi.pembelian_anggota_koperasi h ON h.id=p.pembelian_anggota_koperasi "
                + "WHERE " + where + " GROUP BY 1 HAVING MAX(h.id) IS NOT NULL "
                + "AND ABS(COALESCE(MAX(h.total_biaya),0)-COALESCE(SUM(p.total),0))>0.01) x"));
        long jumlahInvalid = invalid == null ? 0 : (long) num(invalid[0]);

        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Omzet", "Rp " + DashboardUiKit.money(omzet), periodeLabelCache,
                DashboardUiKit.PRIMARY));
        kartu.add(new DashboardUiKit.Stat("Transaksi", DashboardUiKit.money(transaksi), "nota",
                DashboardUiKit.ACCENT));
        kartu.add(new DashboardUiKit.Stat("Barang Terjual", DashboardUiKit.money(qty), "item",
                DashboardUiKit.GOOD));
        kartu.add(new DashboardUiKit.Stat("Pertumbuhan",
                (pertumbuhan >= 0 ? "▲ " : "▼ ") + Math.round(Math.abs(pertumbuhan)) + "%",
                "vs periode sebelumnya", pertumbuhan >= 0 ? DashboardUiKit.GOOD : DashboardUiKit.BAD));
        kartu.add(new DashboardUiKit.Stat("Transaksi Tidak Valid", DashboardUiKit.money(jumlahInvalid),
                "selisih master-detail", jumlahInvalid == 0 ? DashboardUiKit.GOOD : DashboardUiKit.BAD));
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Ringkasan untuk membantu keputusan. Angka dihitung dari seluruh transaksi pada "
                        + periodeLabelCache + ", bukan hanya data yang terlihat di satu halaman.")));
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

        StringBuilder saran = new StringBuilder();
        saran.append("<div style='margin:16px 0;padding:16px;border:1px solid #dbeafe;border-radius:14px;background:#f8fafc'>")
                .append("<div style='font-size:16px;font-weight:800;color:#0f172a;margin-bottom:10px'>Analisis Cerdas dan Tindakan yang Disarankan</div>")
                .append("<div style='font-size:12px;color:#64748b;margin-bottom:12px'>Saran memakai aturan yang dapat diperiksa. Supervisor tetap perlu mencocokkan kondisi stok, petugas, dan kegiatan toko.</div><ol style='line-height:1.7;color:#334155'>");
        if (jumlahInvalid > 0) {
            saran.append("<li><b>Ada ").append(jumlahInvalid).append(" transaksi tidak valid.</b> Aktifkan filter Transaksi tidak valid di Riwayat Penjualan, lalu cocokkan total header, rincian, dan struk sebelum koreksi.</li>");
        } else {
            saran.append("<li><b>Integritas transaksi baik.</b> Tidak ditemukan selisih total master dan rincian pada periode ini. Pertahankan pemeriksaan harian.</li>");
        }
        if (pertumbuhan < 0) {
            saran.append("<li><b>Omzet turun ").append(Math.round(Math.abs(pertumbuhan))).append("%.</b> Periksa produk yang turun, ketersediaan stok, jam operasional, dan perubahan jumlah pelanggan sebelum menambah pembelian.</li>");
        } else {
            saran.append("<li><b>Omzet tumbuh ").append(Math.round(pertumbuhan)).append("%.</b> Siapkan stok dan petugas agar pertumbuhan tidak menimbulkan kehabisan barang atau antrean.</li>");
        }
        if (transaksi > 0 && member * 100.0 / transaksi < 30) {
            saran.append("<li><b>Identifikasi member masih rendah.</b> Kasir dapat menanyakan nomor telepon secara sopan dan wajib memakai data yang sudah ada agar tidak terbentuk member ganda.</li>");
        }
        saran.append("<li><b>Gunakan grafik jam sibuk, metode pembayaran, dan produk terlaris di bawah.</b> Jadwalkan petugas sebelum jam puncak, siapkan kanal pembayaran cadangan, dan tetapkan stok minimum produk utama.</li></ol></div>");
        panel.appendChild(DashboardUiKit.html(saran.toString()));

        LinkedHashMap<String, Double> jam = new LinkedHashMap<String, Double>();
        for (Object[] r : q("decision_hours", "SELECT CAST(EXTRACT(HOUR FROM p.waktu) AS integer),"
                + "COUNT(DISTINCT COALESCE(p.pembelian_anggota_koperasi,p.id)) FROM koperasi.pembelian p WHERE "
                + where + " GROUP BY 1 ORDER BY 1")) {
            jam.put(String.format("%02d:00", (int) num(r[0])), num(r[1]));
        }
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.barList("Jam Ramai",
                "Gunakan untuk pembagian jadwal petugas dan persiapan rak.", jam, DashboardUiKit.ACCENT,
                "transaksi", false, "Belum ada transaksi.")));

        LinkedHashMap<String, Double> bayar = new LinkedHashMap<String, Double>();
        for (Object[] r : q("decision_payments", "SELECT COALESCE(NULLIF(TRIM(p.carabayar),''),'Lainnya'),"
                + "COALESCE(SUM(p.total),0) FROM koperasi.pembelian p WHERE " + where + " GROUP BY 1 ORDER BY 2 DESC")) {
            bayar.put(str(r[0]), num(r[1]));
        }
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.donut("Komposisi Pembayaran",
                "Proporsi kanal pembayaran dan kebutuhan jalur cadangan.", bayar, true, "Belum ada transaksi.")));

        List<Object[]> dasar = new ArrayList<Object[]>();
        dasar.add(new Object[] { "Omzet periode", omzet, "Omzet periode sebelumnya", omzetLalu });
        dasar.add(new Object[] { "Jumlah transaksi", transaksi, "Barang terjual", qty });
        dasar.add(new Object[] { "Transaksi tidak valid", jumlahInvalid, "Member berbeda", member });
        appendRekapTable(panel, "Dasar Angka Rekomendasi",
                "Tabel audit ringkas yang dapat dicetak ke PDF atau Excel sebagai bahan rapat dan tindak lanjut.",
                "Dasar Analisis Keputusan", new String[] { "Indikator A", "Nilai A", "Indikator B", "Nilai B" },
                new int[] { 0, 1, 0, 1 }, dasar);
    }

    // ---------- TAB 1: RINGKASAN ----------
    private void buildRingkasan(final Tabpanel panel) {
        final String where = wherePembelian();
        String html = htmlCached("tab0", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                // Kartu ringkasan utama
                Object[] s = first(q("summary", "SELECT COALESCE(SUM(p.total),0), COUNT(DISTINCT p.id), "
                        + "COALESCE(SUM(p.qty),0), COUNT(DISTINCT p.toko), COUNT(DISTINCT p.anggota_koperasi) "
                        + "FROM koperasi.pembelian p WHERE " + where));
                double omzet = s == null ? 0 : num(s[0]);
                long trx = s == null ? 0 : (long) num(s[1]);
                double qty = s == null ? 0 : num(s[2]);
                long toko = s == null ? 0 : (long) num(s[3]);
                long member = s == null ? 0 : (long) num(s[4]);
                double avg = trx > 0 ? omzet / trx : 0;

                // Pertumbuhan vs periode SEBELUMNYA yang panjangnya sama persis (dihitung di SQL:
                // awal_sebelumnya = awal_sekarang - (SEKARANG - awal_sekarang), berlaku utk semua
                // pilihan periode -- Hari Ini/7 Hari/30 Hari/Bulan Ini/6 Bulan -- tanpa perlu tahu
                // panjang periode di sisi Java). SEBELUMNYA tab ini tak punya pembanding sama sekali,
                // jadi pimpinan tak tahu apakah angka hari ini itu bagus atau buruk tanpa konteks.
                Object[] prev = first(q("summary_prev", "SELECT COALESCE(SUM(p.total),0), COUNT(DISTINCT p.id) "
                        + "FROM koperasi.pembelian p WHERE p.aktif = true AND p.waktu >= ((" + periodeSinceCache
                        + ") - (NOW() - (" + periodeSinceCache + "))) AND p.waktu < (" + periodeSinceCache + ")"
                        + andToko("p")));
                double omzetSebelumnya = prev == null ? 0 : num(prev[0]);
                long trxSebelumnya = prev == null ? 0 : (long) num(prev[1]);
                double pertumbuhanOmzet = omzetSebelumnya > 0 ? ((omzet - omzetSebelumnya) / omzetSebelumnya) * 100.0
                        : (omzet > 0 ? 100.0 : 0.0);
                double pertumbuhanTrx = trxSebelumnya > 0 ? ((trx - trxSebelumnya) / (double) trxSebelumnya) * 100.0
                        : (trx > 0 ? 100.0 : 0.0);

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Total Pemasukan", "Rp " + DashboardUiKit.money(omzet),
                        periodeLabelCache, DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Jumlah Transaksi", DashboardUiKit.money(trx),
                        "struk/nota", DashboardUiKit.ACCENT));
                kartu.add(new DashboardUiKit.Stat("Barang Terjual", DashboardUiKit.money(qty),
                        "item", DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Rata-rata per Transaksi", "Rp " + DashboardUiKit.money(avg),
                        "belanja tiap nota", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Tenant Aktif", DashboardUiKit.money(toko),
                        "toko berjualan", DashboardUiKit.WARN));
                kartu.add(new DashboardUiKit.Stat("Member Belanja", DashboardUiKit.money(member),
                        "anggota bertransaksi", DashboardUiKit.ACCENT));
                kartu.add(new DashboardUiKit.Stat("Pertumbuhan Omzet",
                        (pertumbuhanOmzet >= 0 ? "▲ " : "▼ ") + Math.round(Math.abs(pertumbuhanOmzet)) + "%",
                        "vs periode sebelumnya (Rp " + DashboardUiKit.money(omzetSebelumnya) + ")",
                        pertumbuhanOmzet >= 0 ? DashboardUiKit.GOOD : DashboardUiKit.BAD));
                kartu.add(new DashboardUiKit.Stat("Pertumbuhan Transaksi",
                        (pertumbuhanTrx >= 0 ? "▲ " : "▼ ") + Math.round(Math.abs(pertumbuhanTrx)) + "%",
                        "vs periode sebelumnya (" + DashboardUiKit.money(trxSebelumnya) + " transaksi)",
                        pertumbuhanTrx >= 0 ? DashboardUiKit.GOOD : DashboardUiKit.BAD));
                sb.append(DashboardUiKit.descChip("Angka utama kantin untuk " + periodeLabelCache
                        + ", dibandingkan dengan periode sebelumnya yang panjangnya sama."));
                sb.append(DashboardUiKit.cards(kartu));

                // Tren transaksi keseluruhan (garis)
                List<Double> trenTrx = new ArrayList<Double>();
                for (Object[] r : q("tren", "SELECT TO_CHAR(DATE(p.waktu),'DD Mon'), COUNT(DISTINCT p.id) "
                        + "FROM koperasi.pembelian p WHERE " + where + " GROUP BY DATE(p.waktu) ORDER BY DATE(p.waktu)")) {
                    trenTrx.add(num(r[1]));
                }
                sb.append(DashboardUiKit.sparkline("Tren Transaksi Keseluruhan",
                        "Banyaknya transaksi dari hari ke hari — terlihat kapan kantin sedang ramai atau sepi.",
                        trenTrx, DashboardUiKit.PRIMARY, "Belum ada transaksi pada periode ini."));

                sb.append(DashboardUiKit.openGrid(320));

                // Sebaran metode pembayaran (donat)
                LinkedHashMap<String, Double> bayar = new LinkedHashMap<String, Double>();
                for (Object[] r : q("bayar", "SELECT COALESCE(NULLIF(TRIM(CAST(p.carabayar AS varchar)),''),'Lainnya'), "
                        + "COALESCE(SUM(p.total),0) FROM koperasi.pembelian p WHERE " + where
                        + " GROUP BY 1 ORDER BY 2 DESC")) {
                    bayar.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.donut("Sebaran Metode Pembayaran",
                        "Pembeli lebih sering bayar pakai cara apa (saldo, tunai, dll).", bayar, true,
                        "Belum ada transaksi."));

                // Jam sibuk (batang per jam)
                LinkedHashMap<String, Double> jam = new LinkedHashMap<String, Double>();
                for (Object[] r : q("jam", "SELECT CAST(EXTRACT(HOUR FROM p.waktu) AS integer), COUNT(DISTINCT p.id) "
                        + "FROM koperasi.pembelian p WHERE " + where + " GROUP BY 1 ORDER BY 1")) {
                    jam.put(String.format("%02d:00", (int) num(r[0])), num(r[1]));
                }
                sb.append(DashboardUiKit.barList("Jam Sibuk Transaksi Keseluruhan",
                        "Jam berapa kantin paling ramai — berguna untuk menyiapkan stok dan petugas.", jam,
                        DashboardUiKit.ACCENT, "transaksi", false, "Belum ada transaksi."));

                // Omzet per kategori/jenis produk -- SEBELUMNYA tab ini hanya membedah omzet per tenant
                // (jaring laba-laba di bawah) & per cara bayar, belum per kategori barang (mis. makanan
                // vs minuman vs alat tulis) yang berguna melihat komposisi jualan lintas semua tenant.
                LinkedHashMap<String, Double> kategori = new LinkedHashMap<String, Double>();
                for (Object[] r : q("omzet_kategori", "SELECT COALESCE(jp.nama,'Lainnya'), COALESCE(SUM(p.total),0) "
                        + "FROM koperasi.pembelian p LEFT JOIN koperasi.produk c ON c.id = p.produk "
                        + "LEFT JOIN koperasi.jenis_produk jp ON jp.id = c.jenis_produk WHERE " + where
                        + " GROUP BY 1 ORDER BY 2 DESC")) {
                    kategori.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.donut("Omzet per Kategori Produk",
                        "Kategori/jenis produk mana yang menyumbang pemasukan paling besar, lintas semua tenant.",
                        kategori, true, "Belum ada transaksi."));

                sb.append(DashboardUiKit.closeGrid());

                // Jaring laba-laba: perbandingan kontribusi tenant
                List<Object[]> tn = qPerformaPedagang();
                if (tn.size() >= 3) {
                    double maxO = 0;
                    int n = Math.min(tn.size(), 6);
                    for (int i = 0; i < n; i++) {
                        maxO = Math.max(maxO, num(tn.get(i)[1]));
                    }
                    String[] labels = new String[n];
                    int[] vals = new int[n];
                    for (int i = 0; i < n; i++) {
                        labels[i] = DashboardUiKit.shorten(str(tn.get(i)[0]), 12);
                        vals[i] = maxO <= 0 ? 0 : (int) Math.round(num(tn.get(i)[1]) * 100.0 / maxO);
                    }
                    sb.append(DashboardUiKit.spider("Perbandingan Kinerja Tenant",
                            "Makin lebar jaringnya, makin besar sumbangan pemasukan tenant tersebut.", labels, vals));
                }

                // Peta panas hari x jam (kapan kantin paling ramai)
                List<Object[]> hm = q("heatjam", "SELECT CAST(EXTRACT(DOW FROM p.waktu) AS integer), "
                        + "CAST(EXTRACT(HOUR FROM p.waktu) AS integer), COUNT(DISTINCT p.id) "
                        + "FROM koperasi.pembelian p WHERE " + where + " GROUP BY 1, 2");
                if (!hm.isEmpty()) {
                    String[] hari = { "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab" };
                    String[] jamLbl = new String[24];
                    for (int h = 0; h < 24; h++) {
                        jamLbl[h] = String.format("%02d", h);
                    }
                    double[][] mat = new double[7][24];
                    for (Object[] r : hm) {
                        int dow = (int) num(r[0]);
                        int jamx = (int) num(r[1]);
                        if (dow >= 0 && dow < 7 && jamx >= 0 && jamx < 24) {
                            mat[dow][jamx] = num(r[2]);
                        }
                    }
                    sb.append(DashboardUiKit.heatmap("Peta Jam Ramai (Hari x Jam)",
                            "Kotak makin pekat = jam & hari paling ramai transaksi. Berguna untuk mengatur jadwal petugas dan stok.",
                            hari, jamLbl, mat, DashboardUiKit.PRIMARY, "Belum ada transaksi."));
                }

				List<Object[]> candle = q("candle", "SELECT TO_CHAR(DATE(p.waktu),'DD Mon'), "
						+ "(ARRAY_AGG(p.total ORDER BY p.waktu ASC))[1], MAX(p.total), MIN(p.total), "
						+ "(ARRAY_AGG(p.total ORDER BY p.waktu DESC))[1] "
						+ "FROM koperasi.pembelian p WHERE " + where
						+ " GROUP BY DATE(p.waktu) ORDER BY DATE(p.waktu)");
				if (!candle.isEmpty()) {
					int cn = candle.size();
					String[] cl = new String[cn];
					double[] co = new double[cn], ch = new double[cn], cw = new double[cn], cc = new double[cn];
					for (int ci = 0; ci < cn; ci++) {
						Object[] cr = candle.get(ci);
						cl[ci] = str(cr[0]); co[ci] = num(cr[1]); ch[ci] = num(cr[2]);
						cw[ci] = num(cr[3]); cc[ci] = num(cr[4]);
					}
					sb.append(DashboardUiKit.candlestick("Candlestick Nilai Transaksi",
							"Badan menunjukkan transaksi pertama/terakhir; garis menunjukkan nilai terendah/tertinggi per hari.",
							cl, co, ch, cw, cc));
				}
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(html));

        // "Per Toko" (2026-07-26, permintaan lanjutan) -- rincian tiap toko sbg TABEL, TAMBAHAN di
        // atas kartu KPI gabungan (bukan pengganti). Hanya masuk akal saat dasbor TIDAK sedang
        // disaring ke satu toko (scopeTokoId == null) -- kalau sudah disaring ke satu toko (baik
        // lewat kunci pedagang maupun searchToko admin), tabel "per toko" isinya cuma 1 baris,
        // tak ada gunanya ditampilkan lagi. Data DIPAKAI ULANG dari qPerformaPedagang() -- SAMA
        // persis dgn jaring laba-laba di atas &amp; sheet Excel "Performa Pedagang" -- tidak ada
        // query baru sama sekali.
        if (perTokoAktif && scopeTokoId == null) {
            List<Object[]> perToko = qPerformaPedagang();
            appendRekapTable(panel, "Rincian per Toko / Penjual",
                    "Angka " + periodeLabelCache + " dipecah per toko -- centang \"Per Toko\" di atas untuk melihat/menyembunyikan.",
                    "Rincian Per Toko", new String[] { "Toko", "Omzet", "Transaksi", "Qty Terjual" },
                    new int[] { 0, 2, 1, 1 }, perToko);
        }
    }

    // ---------- TAB 2: PERINGKAT MITRA/TOKO ----------
    /**
     * <h2>Papan Peringkat Antar-Mitra/Toko (2026-07-26).</h2>
     *
     * <p>Dibangun khusus utk kebutuhan pimpinan: satu layar yang langsung menjawab "toko/mitra mana
     * yang paling bagus, mana yang sedang turun" TANPA harus buka tab Keuangan lalu mental-hitung
     * sendiri dari tabel "Performa Pedagang"/"Margin per Tenant". Beda dgn kedua tabel itu (yang
     * urut berdasar omzet SAJA), di sini tiap toko dibandingkan dari 3 sisi sekaligus: omzet, margin
     * (dipakai ulang {@link #petaHargaPokok()}, sumber sama dgn tab Keuangan/Resep-HPP), dan
     * PERTUMBUHAN omzet dibanding periode SEBELUMNYA dgn panjang yang sama (lihat
     * {@link #periodePrevSince()}).</p>
     *
     * <p><b>Kenapa tak ada "skor gabungan" satu angka.</b> Sengaja TIDAK dibuat formula skor komposit
     * (mis. 40% omzet + 30% margin + 30% pertumbuhan) krn bobotnya akan selalu terlihat arbitrer/sulit
     * dipertanggungjawabkan ke pimpinan -- lebih jujur menampilkan 3 angka apa adanya (omzet, margin,
     * pertumbuhan) berdampingan, tabel tetap terurut omzet (metrik uang riil, paling langsung
     * dipahami), dan kolom "Status" hanya label kualitatif dari AMBANG pertumbuhan (bukan skor).</p>
     *
     * <p><b>Hanya utk pandangan gabungan.</b> Kalau dasbor sedang disaring ke satu toko (baik lewat
     * kunci pedagang maupun "Cari Toko" admin), peringkat lintas-toko tak bermakna (cuma 1 baris) --
     * ditampilkan pesan pengarah saja, bukan tabel kosong.</p>
     */
    private void buildLeaderboardMitra(final Tabpanel panel) {
        if (scopeTokoId != null) {
            panel.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                    "Peringkat lintas-toko hanya tersedia saat melihat SEMUA toko sekaligus. Saat ini dasbor "
                            + "sedang disaring ke toko \"" + scopeTokoNama + "\" -- hapus pilihan pada \"Cari Toko\" "
                            + "di atas (bila tersedia) untuk melihat halaman ini.")));
            return;
        }

        final String where = wherePembelian();
        final java.util.Map<Long, Double> peta = petaHargaPokok();

        // Omzet+transaksi+qty periode SEKARANG per toko -- data SAMA PERSIS dipakai ulang dari
        // qPerformaPedagang() (sudah terurut omzet DESC), tidak ada query duplikat.
        List<Object[]> now_ = qPerformaPedagang();

        // Modal (HPP) per toko periode sekarang, utk hitung margin -- perlu level (toko,produk) dulu
        // krn peta HPP per-unit hanya bisa dikalikan di level produk, baru dijumlah per toko.
        LinkedHashMap<String, Double> modalPerToko = new LinkedHashMap<String, Double>();
        for (Object[] r : q("leaderboard_modal", "SELECT b.nama, p.produk, COALESCE(SUM(p.qty),0) FROM "
                + "koperasi.pembelian p JOIN koperasi.toko b ON b.id = p.toko WHERE " + where
                + " GROUP BY b.nama, p.produk")) {
            String nama = str(r[0]);
            Double harga = peta.get(Long.valueOf(Math.round(num(r[1]))));
            double m = num(r[2]) * (harga == null ? 0 : harga.doubleValue());
            Double acc = modalPerToko.get(nama);
            modalPerToko.put(nama, Double.valueOf((acc == null ? 0 : acc.doubleValue()) + m));
        }

        // Omzet periode SEBELUMNYA (panjang sama, langsung sebelum periode aktif) per toko -- dasar
        // hitung pertumbuhan %.
        LinkedHashMap<String, Double> omzetPrevPerToko = new LinkedHashMap<String, Double>();
        String wherePrev = "p.aktif = true AND p.waktu >= (" + periodePrevSince() + ") AND p.waktu < ("
                + periodeSinceCache + ")" + andToko("p");
        for (Object[] r : q("leaderboard_prev", "SELECT b.nama, COALESCE(SUM(p.total),0) FROM koperasi.pembelian p "
                + "JOIN koperasi.toko b ON b.id = p.toko WHERE " + wherePrev + " GROUP BY b.nama")) {
            omzetPrevPerToko.put(str(r[0]), Double.valueOf(num(r[1])));
        }

        // Gabungkan jadi SATU daftar peringkat (dipakai ULANG utuh oleh kartu KPI, grafik batang, dan
        // tabel lengkap di bawah -- dihitung SEKALI di sini, bukan dibangun ulang per konsumen).
        // Kolom: [0]=nama [1]=omzet [2]=trx [3]=qty [4]=margin% [5]=teks pertumbuhan [6]=status.
        final List<Object[]> ranking = new ArrayList<Object[]>();
        String topGrowthNamaTmp = null;
        double topGrowthPctTmp = -Double.MAX_VALUE;
        String worstGrowthNamaTmp = null;
        double worstGrowthPctTmp = Double.MAX_VALUE;
        for (Object[] r : now_) {
            String nama = str(r[0]);
            double omzet = num(r[1]);
            long trx = (long) num(r[2]);
            double qty = num(r[3]);
            Double modal = modalPerToko.get(nama);
            double margin = omzet > 0 ? ((omzet - (modal == null ? 0 : modal.doubleValue())) / omzet) * 100.0 : 0;
            Double prevOmzet = omzetPrevPerToko.get(nama);
            String growthText;
            String statusText;
            if (prevOmzet == null || prevOmzet.doubleValue() <= 0) {
                growthText = "-";
                statusText = "Baru / Belum Ada Pembanding";
            } else {
                double growthPct = (omzet - prevOmzet.doubleValue()) / prevOmzet.doubleValue() * 100.0;
                growthText = (growthPct >= 0 ? "+" : "") + Math.round(growthPct) + "%";
                // Label teks polos (BUKAN emoji) -- konsisten dgn seluruh berkas ini & aman melewati
                // rendering grid ZK 5.5 maupun ekspor Excel tanpa risiko mojibake encoding.
                if (growthPct >= 20) {
                    statusText = "Tumbuh Pesat";
                } else if (growthPct > 0) {
                    statusText = "Bertumbuh";
                } else if (growthPct >= -5) {
                    statusText = "Stabil";
                } else {
                    statusText = "Menurun";
                }
                if (growthPct > topGrowthPctTmp) {
                    topGrowthPctTmp = growthPct;
                    topGrowthNamaTmp = nama;
                }
                if (growthPct < worstGrowthPctTmp) {
                    worstGrowthPctTmp = growthPct;
                    worstGrowthNamaTmp = nama;
                }
            }
            ranking.add(new Object[] { nama, Double.valueOf(omzet), Long.valueOf(trx), Double.valueOf(qty),
                    Double.valueOf(margin), growthText, statusText });
        }
        final String topGrowthNama = topGrowthNamaTmp;
        final double topGrowthPct = topGrowthPctTmp;
        final String worstGrowthNama = worstGrowthNamaTmp;
        final double worstGrowthPct = worstGrowthPctTmp;

        String html = htmlCached("leaderboard", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                if (ranking.isEmpty()) {
                    sb.append(DashboardUiKit.descChip(
                            "Belum ada transaksi pada " + periodeLabelCache + " utk dibuat peringkat."));
                    return sb.toString();
                }

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Mitra/Toko Aktif", DashboardUiKit.money(ranking.size()),
                        periodeLabelCache, DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Omzet Tertinggi",
                        "Rp " + DashboardUiKit.money(num(ranking.get(0)[1])),
                        DashboardUiKit.shorten(str(ranking.get(0)[0]), 22), DashboardUiKit.GOOD));
                if (topGrowthNama != null) {
                    kartu.add(new DashboardUiKit.Stat("Pertumbuhan Tertinggi",
                            (topGrowthPct >= 0 ? "+" : "") + Math.round(topGrowthPct) + "%",
                            DashboardUiKit.shorten(topGrowthNama, 22), DashboardUiKit.ACCENT));
                }
                if (worstGrowthNama != null && worstGrowthPct < 0) {
                    kartu.add(new DashboardUiKit.Stat("Perlu Perhatian", Math.round(worstGrowthPct) + "%",
                            DashboardUiKit.shorten(worstGrowthNama, 22), DashboardUiKit.BAD));
                }
                sb.append(DashboardUiKit.descChip(
                        "Peringkat seluruh toko/mitra pada " + periodeLabelCache + ", dibandingkan dari sisi omzet, "
                                + "margin (untung), dan pertumbuhan terhadap periode sebelumnya dgn panjang yang sama. "
                                + "Tabel tetap terurut omzet (uang riil) -- kolom lain dilihat berdampingan, bukan digabung jadi satu skor."));
                sb.append(DashboardUiKit.cards(kartu));

                LinkedHashMap<String, Double> barOmzet = new LinkedHashMap<String, Double>();
                for (int i = 0; i < ranking.size() && i < 10; i++) {
                    barOmzet.put(str(ranking.get(i)[0]), num(ranking.get(i)[1]));
                }
                sb.append(DashboardUiKit.barList("Peringkat Omzet Mitra/Toko",
                        "10 mitra/toko dengan omzet tertinggi pada " + periodeLabelCache + ".", barOmzet,
                        DashboardUiKit.PRIMARY, "", true, "Belum ada transaksi."));
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(html));

        if (ranking.isEmpty()) {
            return;
        }
        appendRekapTable(panel, "Peringkat Lengkap Mitra/Toko",
                "Semua toko/mitra pada " + periodeLabelCache + ", terurut omzet tertinggi.", "Peringkat Mitra",
                new String[] { "Toko/Mitra", "Omzet", "Transaksi", "Qty Terjual", "Margin", "Pertumbuhan", "Status" },
                new int[] { 0, 2, 1, 1, 4, 0, 0 }, ranking);
    }

    /**
     * Rentang tanggal "periode sebelumnya" (panjang sama, langsung sebelum periode aktif) -- dipakai
     * HANYA oleh {@link #buildLeaderboardMitra} utk menghitung pertumbuhan omzet per toko. Dipetakan
     * manual dari label periode (BUKAN dihitung otomatis dari periodeSinceCache) krn periodeSinceCache
     * kadang berupa ekspresi timestamp (mis. {@code NOW() - INTERVAL '6 months'}) yang sulit
     * diaritmatika ulang secara generik -- 5 pilihan periode combobox ini tetap/tidak dinamis (lihat
     * {@link #isiPeriode()}), jadi pemetaan manual per-label ini aman & selalu sinkron selama kedua
     * method diubah bersamaan.
     */
    private String periodePrevSince() {
        String label = periodeLabelCache;
        if ("Hari Ini".equals(label)) {
            return "CURRENT_DATE - INTERVAL '1 day'";
        } else if ("7 Hari Terakhir".equals(label)) {
            return "CURRENT_DATE - INTERVAL '13 days'";
        } else if ("Bulan Ini".equals(label)) {
            return "DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month'";
        } else if ("6 Bulan Terakhir".equals(label)) {
            return "NOW() - INTERVAL '12 months'";
        }
        // Default (termasuk "30 Hari Terakhir"): periode sebelumnya = 30 hari sebelum itu.
        return "CURRENT_DATE - INTERVAL '59 days'";
    }

    // ---------- TAB 3: KEUANGAN & LABA ----------
    private void buildKeuangan(final Tabpanel panel) {
        final String where = wherePembelian();

        // Peta HPP per unit SEMUA produk (rollup resep utk yang ber-resep, hargabeli utk sisanya) --
        // sama persis dgn yang dipakai tab Resep, HPP & Margin. Data laba diagregasi per (hari, produk)
        // -- BUKAN pakai c.hargabeli langsung di SQL -- lalu modal dihitung di Java via peta ini.
        final java.util.Map<Long, Double> peta = petaHargaPokok();
        final List<Object[]> labDetail = q("laba_detail", "SELECT DATE(p.waktu), TO_CHAR(DATE(p.waktu),'DD Mon'), "
                + "p.produk, COALESCE(SUM(p.total),0), COALESCE(SUM(p.qty),0) FROM koperasi.pembelian p "
                + "WHERE " + where + " GROUP BY DATE(p.waktu), p.produk ORDER BY DATE(p.waktu)");

        String html = htmlCached("tab1", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                // Agregasi ulang per hari (kunci = tanggal ASLI, bukan label "DD Mon" -- supaya tak
                // pernah tabrakan walau periode lintas tahun) sebelum dipetakan ke label tampilan.
                LinkedHashMap<String, double[]> harian = new LinkedHashMap<String, double[]>();
                LinkedHashMap<String, String> labelHarian = new LinkedHashMap<String, String>();
                for (Object[] r : labDetail) {
                    String tgl = str(r[0]);
                    double o = num(r[3]);
                    double qty = num(r[4]);
                    Double harga = peta.get(Long.valueOf(Math.round(num(r[2]))));
                    double m = qty * (harga == null ? 0 : harga.doubleValue());
                    double[] acc = harian.get(tgl);
                    if (acc == null) {
                        acc = new double[2];
                        harian.put(tgl, acc);
                        labelHarian.put(tgl, str(r[1]));
                    }
                    acc[0] += o;
                    acc[1] += m;
                }

                double omzet = 0, modal = 0;
                LinkedHashMap<String, Double> omzetMap = new LinkedHashMap<String, Double>();
                LinkedHashMap<String, Double> modalMap = new LinkedHashMap<String, Double>();
                List<Double> labaVals = new ArrayList<Double>();
                for (java.util.Map.Entry<String, double[]> en : harian.entrySet()) {
                    double o = en.getValue()[0];
                    double m = en.getValue()[1];
                    omzet += o;
                    modal += m;
                    String label = labelHarian.get(en.getKey());
                    omzetMap.put(label, o);
                    modalMap.put(label, m);
                    labaVals.add(o - m);
                }
                double laba = omzet - modal;
                double margin = omzet > 0 ? (laba / omzet) * 100.0 : 0;

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Omzet (Penjualan)", "Rp " + DashboardUiKit.money(omzet),
                        periodeLabelCache, DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Modal (HPP)", "Rp " + DashboardUiKit.money(modal),
                        "harga beli / rollup resep barang terjual", DashboardUiKit.WARN));
                kartu.add(new DashboardUiKit.Stat("Laba Kotor", "Rp " + DashboardUiKit.money(laba),
                        "omzet dikurangi modal", laba >= 0 ? DashboardUiKit.GOOD : DashboardUiKit.BAD));
                kartu.add(new DashboardUiKit.Stat("Margin", Math.round(margin) + "%",
                        "bagian untung dari omzet", DashboardUiKit.ACCENT));
                sb.append(DashboardUiKit.descChip(
                        "Hitung-hitungan untung kasar: dari setiap penjualan, berapa yang jadi keuntungan setelah dikurangi modal. "
                                + "Modal produk beli-jadi = harga beli; modal menu racikan = dihitung otomatis dari resep bahan bakunya."));
                sb.append(DashboardUiKit.cards(kartu));

                sb.append(DashboardUiKit.sparkline("Tren Laba Kotor Keseluruhan",
                        "Naik-turunnya keuntungan kotor dari hari ke hari.", labaVals,
                        DashboardUiKit.GOOD, "Belum ada data penjualan."));

                sb.append(DashboardUiKit.groupedBar("Perbandingan Omzet vs Modal",
                        "Membandingkan uang masuk (omzet) dengan biaya barang (modal) tiap hari — selisihnya adalah untung.",
                        omzetMap, modalMap, "Omzet", "Modal", DashboardUiKit.PRIMARY, DashboardUiKit.WARN, true));
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(html));

        // Performa pedagang: grafik batang + tabel + Unduh Excel
        List<Object[]> pedagang = qPerformaPedagang();
        LinkedHashMap<String, Double> barOmzet = new LinkedHashMap<String, Double>();
        for (int i = 0; i < pedagang.size() && i < 10; i++) {
            barOmzet.put(str(pedagang.get(i)[0]), num(pedagang.get(i)[1]));
        }
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.barList("Performa Pedagang Keseluruhan",
                "Tenant mana yang paling besar pemasukannya pada periode ini.", barOmzet,
                DashboardUiKit.PRIMARY, "", true, "Belum ada penjualan.")));
        appendRekapTable(panel, "Rekapitulasi Performa Pedagang",
                "Rincian pemasukan, jumlah transaksi, dan barang terjual tiap tenant.", "Performa Pedagang",
                new String[] { "Tenant", "Omzet", "Transaksi", "Barang Terjual" }, new int[] { 0, 2, 1, 1 }, pedagang);

        // Margin (keuntungan) per tenant -- SEBELUMNYA tab ini hanya membandingkan tenant dari sisi
        // OMZET (tabel di atas), belum dari sisi UNTUNG sesungguhnya -- padahal omzet besar tak selalu
        // berarti untung besar (bisa jadi margin tipis). Pakai peta HPP yang SAMA dgn kartu "Laba Kotor"
        // di atas (variabel `peta`, sumber tunggal bersama buildResepHpp) agar angkanya konsisten.
        // Kolom margin memakai kind=4 (badge Sehat/Tipis/Rugi), pola sama dgn tabel margin per-menu.
        LinkedHashMap<String, double[]> perTenant = new LinkedHashMap<String, double[]>();
        for (Object[] r : q("margin_tenant_detail", "SELECT b.nama, p.produk, COALESCE(SUM(p.total),0), "
                + "COALESCE(SUM(p.qty),0) FROM koperasi.pembelian p JOIN koperasi.toko b ON b.id = p.toko WHERE "
                + where + " GROUP BY b.nama, p.produk")) {
            String tenant = str(r[0]);
            double o = num(r[2]);
            double qtyProduk = num(r[3]);
            Double harga = peta.get(Long.valueOf(Math.round(num(r[1]))));
            double m = qtyProduk * (harga == null ? 0 : harga.doubleValue());
            double[] acc = perTenant.get(tenant);
            if (acc == null) {
                acc = new double[2];
                perTenant.put(tenant, acc);
            }
            acc[0] += o;
            acc[1] += m;
        }
        List<Object[]> marginTenant = new ArrayList<Object[]>();
        for (java.util.Map.Entry<String, double[]> en : perTenant.entrySet()) {
            double o = en.getValue()[0];
            double m = en.getValue()[1];
            double labaTenant = o - m;
            double marginTenantPct = o > 0 ? (labaTenant / o) * 100.0 : 0;
            marginTenant.add(new Object[] { en.getKey(), Double.valueOf(o), Double.valueOf(m),
                    Double.valueOf(labaTenant), Double.valueOf(marginTenantPct) });
        }
        java.util.Collections.sort(marginTenant, new java.util.Comparator<Object[]>() {
            @Override
            public int compare(Object[] a, Object[] b) {
                return Double.compare(num(b[1]), num(a[1]));
            }
        });
        appendRekapTable(panel, "Margin (Keuntungan) per Tenant",
                "Omzet, modal, laba, dan persentase margin tiap tenant -- pakai peta HPP yang sama dengan kartu "
                        + "ringkasan di atas, sehingga terlihat tenant mana yang omzetnya besar TAPI marginnya tipis.",
                "Margin per Tenant", new String[] { "Tenant", "Omzet", "Modal", "Laba", "Margin" },
                new int[] { 0, 2, 2, 2, 4 }, marginTenant);
    }

    // ---------- TAB 4: PRODUK & STOK ----------
    private void buildProdukStok(final Tabpanel panel) {
        final String where = wherePembelian();
        String html = htmlCached("tab2", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                // Kartu KPI ringkas -- SEBELUMNYA tab ini langsung ke grafik tanpa ringkasan angka
                // apa pun. Snapshot stok TIDAK terikat periode (data stok terkini), sedangkan produk
                // tanpa penjualan dihitung dalam periode yang sedang aktif di dasbor.
                Object[] snap = first(qShared("produkstoksnapshot", "SELECT COUNT(*), "
                        + "COALESCE(SUM(COALESCE(c.stok,0) * COALESCE(c.hargabeli,0)),0), "
                        + "COUNT(*) FILTER (WHERE COALESCE(c.stok,0) <= 5) "
                        + "FROM koperasi.produk c WHERE (c.aktif = true OR c.aktif IS NULL)" + andToko("c")));
                long totalSku = snap == null ? 0 : (long) num(snap[0]);
                double nilaiStok = snap == null ? 0 : num(snap[1]);
                long stokKritis = snap == null ? 0 : (long) num(snap[2]);

                Object[] tp = first(q("produktanpapenjualan", "SELECT COUNT(*) FROM koperasi.produk c "
                        + "WHERE (c.aktif = true OR c.aktif IS NULL)" + andToko("c") + " AND NOT EXISTS ("
                        + "SELECT 1 FROM koperasi.pembelian p WHERE p.produk = c.id AND p.aktif = true "
                        + "AND p.waktu >= (" + periodeSinceCache + ")" + andToko("p") + ")"));
                long tanpaPenjualan = tp == null ? 0 : (long) num(tp[0]);

                // Kartu "Kadaluarsa" (2026-07-26, gap analisis PDF klien) -- SEBELUMNYA field
                // Produk.tanggalExpired hanya terlihat lewat katalog "Laporan-Laporan" (harus dicari
                // manual), tidak pernah muncul sbg sinyal cepat di ringkasan dasbor. TIDAK terikat
                // periode dasbor (data kadaluarsa tetap relevan dilihat kapan pun dibuka).
                Object[] exp = first(qShared("produkkadaluarsa",
                        "SELECT COUNT(*) FILTER (WHERE c.tanggal_expired < CURRENT_DATE), "
                                + "COUNT(*) FILTER (WHERE c.tanggal_expired >= CURRENT_DATE AND c.tanggal_expired < CURRENT_DATE + 30) "
                                + "FROM koperasi.produk c WHERE (c.aktif = true OR c.aktif IS NULL) "
                                + "AND c.tanggal_expired IS NOT NULL" + andToko("c")));
                long sudahKadaluarsa = exp == null ? 0 : (long) num(exp[0]);
                long akanKadaluarsa = exp == null ? 0 : (long) num(exp[1]);

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Total SKU Aktif", DashboardUiKit.money(totalSku),
                        "produk aktif terdaftar", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Nilai Stok", "Rp " + DashboardUiKit.money(nilaiStok),
                        "estimasi modal tertahan di stok (harga beli)", DashboardUiKit.ACCENT));
                kartu.add(new DashboardUiKit.Stat("Stok Kritis", DashboardUiKit.money(stokKritis),
                        "sisa stok <= 5, perlu segera diisi ulang",
                        stokKritis > 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Tanpa Penjualan", DashboardUiKit.money(tanpaPenjualan),
                        "tidak terjual sama sekali pada " + periodeLabelCache,
                        tanpaPenjualan > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Sudah Kadaluarsa", DashboardUiKit.money(sudahKadaluarsa),
                        "WAJIB segera ditarik dari stok jual -- sudah diblokir otomatis saat checkout",
                        sudahKadaluarsa > 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Akan Kadaluarsa (30 Hari)", DashboardUiKit.money(akanKadaluarsa),
                        "perlu diprioritaskan terjual/diretur sebelum kadaluarsa",
                        akanKadaluarsa > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
                sb.append(DashboardUiKit.descChip(
                        "Ringkasan kondisi produk & stok keseluruhan -- sinyal cepat sebelum menelusuri rincian di bawah."));
                sb.append(DashboardUiKit.cards(kartu));

                sb.append(DashboardUiKit.openGrid(320));

                // 10 produk terlaris
                LinkedHashMap<String, Double> terlaris = new LinkedHashMap<String, Double>();
                for (Object[] r : q("terlaris", "SELECT c.nama, COALESCE(SUM(p.qty),0) q FROM koperasi.pembelian p "
                        + "JOIN koperasi.produk c ON c.id = p.produk WHERE " + where
                        + " GROUP BY c.nama ORDER BY q DESC LIMIT 10")) {
                    terlaris.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.barList("10 Produk Terlaris Keseluruhan",
                        "Barang yang paling banyak dibeli (jumlah item).", terlaris,
                        DashboardUiKit.GOOD, "item", false, "Belum ada penjualan."));

                // Produk kurang laku (slow-moving), termasuk yang belum pernah terjual
                LinkedHashMap<String, Double> kurang = new LinkedHashMap<String, Double>();
                for (Object[] r : q("slow", "SELECT c.nama, COALESCE(SUM(p.qty),0) q FROM koperasi.produk c "
                        + "JOIN koperasi.toko b ON b.id = c.toko "
                        + "LEFT JOIN koperasi.pembelian p ON (p.produk = c.id AND p.aktif = true AND p.waktu >= ("
                        + periodeSinceCache + ")) WHERE (c.aktif = true OR c.aktif IS NULL) " + andToko("c")
                        + " GROUP BY c.nama ORDER BY q ASC, c.nama ASC LIMIT 12")) {
                    kurang.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.barList("Produk Kurang Laku (Slow-Moving)",
                        "Barang yang paling sedikit/belum terjual — kandidat untuk promo atau dihentikan.", kurang,
                        DashboardUiKit.WARN, "item", false, "Belum ada data produk."));

                sb.append(DashboardUiKit.closeGrid());
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(html));

        // "Per Toko" (2026-07-26, lanjutan) -- ringkasan SKU/nilai stok/kritis/kadaluarsa dipecah per
        // toko, pola SAMA PERSIS dgn buildRingkasan(): TAMBAHAN di atas kartu KPI gabungan, hanya
        // muncul saat TIDAK sedang disaring ke satu toko (kalau sudah difilter, hasilnya cuma 1 baris).
        if (perTokoAktif && scopeTokoId == null) {
            List<Object[]> perToko = qShared("produkstok_pertoko", "SELECT COALESCE(t.nama,'-'), COUNT(*), "
                    + "COALESCE(SUM(COALESCE(c.stok,0)*COALESCE(c.hargabeli,0)),0), "
                    + "COUNT(*) FILTER (WHERE COALESCE(c.stok,0) <= 5), "
                    + "COUNT(*) FILTER (WHERE c.tanggal_expired < CURRENT_DATE) "
                    + "FROM koperasi.produk c LEFT JOIN koperasi.toko t ON t.id = c.toko "
                    + "WHERE (c.aktif = true OR c.aktif IS NULL)" + andToko("c") + " GROUP BY t.nama ORDER BY 3 DESC");
            appendRekapTable(panel, "Ringkasan Produk & Stok per Toko",
                    "Total SKU, nilai stok, stok kritis, dan produk kadaluarsa dipecah per toko -- centang \"Per Toko\" di atas untuk melihat/menyembunyikan.",
                    "Ringkasan Stok Per Toko",
                    new String[] { "Toko", "Total SKU", "Nilai Stok", "Stok Kritis", "Sudah Kadaluarsa" },
                    new int[] { 0, 1, 2, 1, 1 }, perToko);
        }

        // Rekap produk terlaris (tabel + Excel)
        appendRekapTable(panel, "Rekap Produk Terlaris Keseluruhan",
                "Rincian jumlah terjual dan pemasukan tiap produk, dikelompokkan per tenant.", "Produk Terlaris",
                new String[] { "Tenant", "Produk", "Qty Terjual", "Omzet" }, new int[] { 0, 0, 1, 2 }, qRekapProduk());

        // Peringatan stok (tabel + Excel), stok kecil disorot merah/oranye
        appendRekapTable(panel, "Peringatan Stok Keseluruhan",
                "Sisa stok tiap produk, diurutkan dari yang paling menipis. Merah = sangat sedikit, oranye = perlu diisi.",
                "Peringatan Stok", new String[] { "Tenant", "Produk", "Sisa Stok" }, new int[] { 0, 0, 3 }, qStok());

        // Estimasi habis bahan baku (tabel + Excel) -- ported dari formula PosApi.prosesDashboardProduk
        // (dipakai versi Desktop/Android): perHari = total pemakaian 30 hari terakhir / 30, estimasiHari
        // = sisaStok / perHari. Hanya bahan yang benar-benar terpakai dalam 30 hari terakhir yang
        // dimasukkan (HAVING SUM(qty) > 0) supaya tidak ada baris estimasi tak terhingga/menyesatkan.
        // Data 30-hari-terakhir ini TIDAK mengikuti filter periode dasbor (qShared), sama seperti tabel
        // Peringatan Stok di atas.
        // KE-FIX: ROUND(double precision, integer) TIDAK ADA di Postgres (hanya
        // round(numeric,int) atau round(double precision) 1-argumen) -> "function round(double
        // precision, integer) does not exist". SUM(pb.qty)/30.0 & pembagian di baris kedua
        // menghasilkan double precision (literal 30.0), jadi di-cast ::numeric dulu.
        List<Object[]> estimasi = qShared("estimasihabisbahan", "SELECT b.nama, pr.nama, COALESCE(pr.stok,0), "
                + "ROUND((SUM(pb.qty)/30.0)::numeric, 2), ROUND((COALESCE(pr.stok,0) / (SUM(pb.qty)/30.0))::numeric, 1) "
                + "FROM koperasi.pemakaian_bahan_baku pb "
                + "JOIN koperasi.produk pr ON pr.id = pb.produk "
                + "JOIN koperasi.toko b ON b.id = pb.toko "
                + "WHERE pb.waktu >= CURRENT_DATE - INTERVAL '30 days'" + andToko("pb")
                + " GROUP BY b.nama, pr.id, pr.nama, pr.stok HAVING SUM(pb.qty) > 0 ORDER BY 5 ASC LIMIT 50");
        appendRekapTable(panel, "Estimasi Habis Bahan Baku",
                "Perkiraan berapa hari lagi tiap bahan baku akan habis, berdasarkan rata-rata pemakaian 30 hari "
                        + "terakhir -- diurutkan dari yang paling mendesak. Merah = kurang dari 5 hari lagi, "
                        + "oranye = kurang dari 10 hari lagi.",
                "Estimasi Habis Bahan Baku",
                new String[] { "Tenant", "Bahan/Produk", "Sisa Stok", "Pemakaian/Hari", "Estimasi Habis (hari)" },
                new int[] { 0, 0, 1, 1, 3 }, estimasi);
    }

    // ---------- TAB 5: PELANGGAN ----------
    private void buildPelanggan(final Tabpanel panel) {
        final String where = wherePembelian();
        String html = htmlCached("tab3", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                // Kartu KPI utama -- SEBELUMNYA tab ini tak punya kartu KPI/tren sama sekali, cuma
                // barList loyal + tabel rekap (paling tipis dari semua tab dasbor, lihat riset). Total
                // pelanggan aktif + rata2 belanja/frekuensi dihitung dari SATU query summary member.
                Object[] s = first(q("pelanggansummary", "SELECT COUNT(DISTINCT p.anggota_koperasi), "
                        + "COALESCE(SUM(p.total),0), COUNT(p.id) FROM koperasi.pembelian p WHERE " + where
                        + " AND p.anggota_koperasi IS NOT NULL"));
                long pelangganAktif = s == null ? 0 : (long) num(s[0]);
                double omzetMember = s == null ? 0 : num(s[1]);
                long trxMember = s == null ? 0 : (long) num(s[2]);
                double rataBelanja = pelangganAktif > 0 ? omzetMember / pelangganAktif : 0;
                double rataFrekuensi = pelangganAktif > 0 ? (double) trxMember / pelangganAktif : 0;

                // Pelanggan BARU (transaksi pertama SEPANJANG SEJARAH-nya jatuh di dalam periode ini,
                // dibatasi per toko yg sama bila scope toko aktif) vs LAMA (pernah beli sebelum periode).
                Object[] bl = first(q("pelanggan_baru_lama", "SELECT "
                        + "COUNT(DISTINCT pertama.anggota_koperasi) FILTER (WHERE pertama.tgl >= (" + periodeSinceCache + ")), "
                        + "COUNT(DISTINCT pertama.anggota_koperasi) FILTER (WHERE pertama.tgl < (" + periodeSinceCache + ")) "
                        + "FROM (SELECT pp.anggota_koperasi, MIN(pp.waktu) tgl FROM koperasi.pembelian pp "
                        + "WHERE pp.aktif = true AND pp.anggota_koperasi IS NOT NULL" + andToko("pp")
                        + " GROUP BY pp.anggota_koperasi) pertama "
                        + "JOIN koperasi.pembelian p2 ON p2.anggota_koperasi = pertama.anggota_koperasi WHERE "
                        + where.replace("p.", "p2.") + " AND p2.anggota_koperasi IS NOT NULL"));
                long pelangganBaru = bl == null ? 0 : (long) num(bl[0]);
                long pelangganLama = bl == null ? 0 : (long) num(bl[1]);

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Pelanggan Aktif", DashboardUiKit.money(pelangganAktif),
                        periodeLabelCache, DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Pelanggan Baru", DashboardUiKit.money(pelangganBaru),
                        "transaksi pertama di periode ini", DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Rata-rata Belanja", "Rp " + DashboardUiKit.money(rataBelanja),
                        "per pelanggan aktif", DashboardUiKit.ACCENT));
                kartu.add(new DashboardUiKit.Stat("Rata-rata Frekuensi", String.format("%.1fx", rataFrekuensi),
                        "transaksi per pelanggan aktif", DashboardUiKit.WARN));
                sb.append(DashboardUiKit.descChip(
                        "Seberapa banyak dan seberapa sering member berbelanja pada " + periodeLabelCache
                                + " -- termasuk berapa yang benar-benar baru pertama kali belanja."));
                sb.append(DashboardUiKit.cards(kartu));

                sb.append(DashboardUiKit.openGrid(320));

                // Tren pelanggan baru per hari
                LinkedHashMap<String, Double> trenBaru = new LinkedHashMap<String, Double>();
                for (Object[] r : q("tren_pelanggan_baru", "SELECT TO_CHAR(hari,'DD Mon'), COUNT(*) FROM ("
                        + "SELECT anggota_koperasi, DATE(MIN(waktu)) hari FROM koperasi.pembelian "
                        + "WHERE aktif = true AND anggota_koperasi IS NOT NULL" + andToko("koperasi.pembelian")
                        + " GROUP BY anggota_koperasi HAVING DATE(MIN(waktu)) >= (" + periodeSinceCache + ")) x "
                        + "GROUP BY hari ORDER BY hari")) {
                    trenBaru.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.sparkline("Tren Pelanggan Baru",
                        "Berapa member yang belanja pertama kalinya tiap hari -- ukuran keberhasilan menarik pelanggan baru.",
                        new ArrayList<Double>(trenBaru.values()), DashboardUiKit.GOOD, "Belum ada pelanggan baru pada periode ini."));

                // Komposisi baru vs lama
                LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
                komposisi.put("Pelanggan Baru", (double) pelangganBaru);
                komposisi.put("Pelanggan Lama", (double) pelangganLama);
                sb.append(DashboardUiKit.donut("Komposisi Pelanggan Baru vs Lama",
                        "Perbandingan pembeli yang baru pertama kali belanja dengan yang sudah pernah belanja sebelumnya.",
                        komposisi, false, "Belum ada pelanggan pada periode ini."));

                sb.append(DashboardUiKit.closeGrid());

                LinkedHashMap<String, Double> loyal = new LinkedHashMap<String, Double>();
                for (Object[] r : q("loyal10", "SELECT ak.nama, COALESCE(SUM(p.total),0) FROM koperasi.pembelian p "
                        + "JOIN koperasi.anggota_koperasi ak ON ak.id = p.anggota_koperasi WHERE " + where
                        + " AND p.anggota_koperasi IS NOT NULL GROUP BY ak.nama ORDER BY 2 DESC LIMIT 10")) {
                    loyal.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.barList("10 Pembeli Terloyal Keseluruhan",
                        "Member dengan total belanja terbesar — cocok untuk diberi apresiasi/hadiah.", loyal,
                        DashboardUiKit.PRIMARY, "", true, "Belum ada belanja member."));
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(html));

        // "Per Toko" (2026-07-26, lanjutan) -- pelanggan aktif, omzet, dan rata-rata belanja dipecah
        // per toko. Rata-rata dihitung di Java (bukan AVG SQL) supaya pembaginya konsisten dgn definisi
        // "pelanggan aktif" (COUNT DISTINCT) yang dipakai kartu KPI di atas.
        if (perTokoAktif && scopeTokoId == null) {
            List<Object[]> perTokoRaw = qShared("pelanggan_pertoko", "SELECT COALESCE(t.nama,'-'), "
                    + "COUNT(DISTINCT p.anggota_koperasi), COALESCE(SUM(p.total),0), COUNT(p.id) "
                    + "FROM koperasi.pembelian p LEFT JOIN koperasi.toko t ON t.id = p.toko WHERE " + where
                    + " AND p.anggota_koperasi IS NOT NULL GROUP BY t.nama ORDER BY 3 DESC");
            List<Object[]> perToko = new ArrayList<Object[]>();
            for (Object[] r : perTokoRaw) {
                long aktifToko = (long) num(r[1]);
                double omzetToko = num(r[2]);
                double rata = aktifToko > 0 ? omzetToko / aktifToko : 0;
                perToko.add(new Object[] { str(r[0]), Long.valueOf(aktifToko), Double.valueOf(omzetToko),
                        Double.valueOf(rata) });
            }
            appendRekapTable(panel, "Ringkasan Pelanggan per Toko",
                    "Jumlah pelanggan aktif, total belanja, dan rata-rata belanja per pelanggan, dipecah per toko -- centang \"Per Toko\" di atas untuk melihat/menyembunyikan.",
                    "Ringkasan Pelanggan Per Toko",
                    new String[] { "Toko", "Pelanggan Aktif", "Total Belanja", "Rata-rata Belanja" },
                    new int[] { 0, 1, 2, 2 }, perToko);
        }

        appendRekapTable(panel, "Rekap Pelanggan Terloyal Keseluruhan",
                "Rincian seberapa sering dan seberapa besar tiap pelanggan berbelanja, dikelompokkan per tenant.",
                "Pelanggan Terloyal", new String[] { "Tenant", "Pelanggan", "Frekuensi", "Total Belanja" },
                new int[] { 0, 0, 1, 2 }, qRekapPelanggan());
    }

    // ---------- TAB 6: TRANSAKSI & PESANAN ----------
    private void buildTransaksiPesanan(Tabpanel panel) {
        final String where = wherePembelian();

        // Riwayat Transaksi Keseluruhan — ringkasan status pelayanan (selesai vs masih diproses).
        String htmlRiwayat = htmlCached("tab4", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();
                sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;margin:6px 0 4px;border-left:4px solid "
                        + DashboardUiKit.PRIMARY + ";padding-left:10px;'>Riwayat Transaksi Keseluruhan</div>");
                Object[] st = first(q("trxstatus", "SELECT COUNT(*), COALESCE(SUM(CASE WHEN selesai THEN 1 ELSE 0 END),0) "
                        + "FROM (SELECT (MIN(CASE WHEN COALESCE(p.terlayani,false) THEN 1 ELSE 0 END) = 1) AS selesai "
                        + "FROM koperasi.pembelian p WHERE " + where
                        + " GROUP BY COALESCE(p.pembelian_anggota_koperasi, p.id)) x"));
                long total = st == null ? 0 : (long) num(st[0]);
                long selesai = st == null ? 0 : (long) num(st[1]);
                long diproses = total - selesai;

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Total Transaksi", DashboardUiKit.money(total),
                        periodeLabelCache, DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Sudah Dilayani", DashboardUiKit.money(selesai),
                        "pesanan selesai", DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Masih Diproses", DashboardUiKit.money(diproses),
                        "perlu segera ditindaklanjuti", diproses > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
                sb.append(DashboardUiKit.descChip(
                        "Berapa banyak transaksi yang sudah selesai dilayani dan berapa yang masih perlu diproses."));
                sb.append(DashboardUiKit.cards(kartu));

                LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
                komposisi.put("Sudah Dilayani", (double) selesai);
                komposisi.put("Masih Diproses", (double) diproses);
                sb.append(DashboardUiKit.donut("Status Pelayanan Transaksi",
                        "Perbandingan transaksi yang sudah selesai dengan yang masih diproses.", komposisi, false,
                        "Belum ada transaksi."));

                // Tren harian Selesai vs Diproses -- SEBELUMNYA panel ini hanya kartu kumulatif + donut,
                // tak ada dimensi waktu sama sekali (naik/turunnya backlog dari hari ke hari tak terlihat).
                LinkedHashMap<String, Double> selesaiHarian = new LinkedHashMap<String, Double>();
                LinkedHashMap<String, Double> diprosesHarian = new LinkedHashMap<String, Double>();
                for (Object[] r : q("trxstatusharian", "SELECT TO_CHAR(hari,'DD Mon'), "
                        + "COUNT(*) FILTER (WHERE selesai), COUNT(*) FILTER (WHERE NOT selesai) FROM ("
                        + "SELECT DATE(MIN(p.waktu)) hari, (MIN(CASE WHEN COALESCE(p.terlayani,false) THEN 1 ELSE 0 END) = 1) selesai "
                        + "FROM koperasi.pembelian p WHERE " + where
                        + " GROUP BY COALESCE(p.pembelian_anggota_koperasi, p.id)) x GROUP BY hari ORDER BY hari")) {
                    selesaiHarian.put(str(r[0]), num(r[1]));
                    diprosesHarian.put(str(r[0]), num(r[2]));
                }
                sb.append(DashboardUiKit.groupedBar("Tren Harian: Selesai vs Diproses",
                        "Perkembangan jumlah transaksi selesai dibanding yang masih diproses dari hari ke hari -- "
                                + "naiknya batang \"Diproses\" berarti backlog pelayanan sedang bertambah.",
                        selesaiHarian, diprosesHarian, "Selesai", "Diproses", DashboardUiKit.GOOD, DashboardUiKit.WARN, true));
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(htmlRiwayat));

        // "Per Toko" (2026-07-26, lanjutan) -- total transaksi, sudah dilayani, dan masih diproses
        // dipecah per toko. Subquery SAMA PERSIS dgn kartu "Total Transaksi/Sudah Dilayani/Masih
        // Diproses" di atas (satu baris = satu nota gabungan lewat COALESCE(pembelian_anggota_koperasi,id)),
        // hanya ditambah p.toko supaya bisa di-GROUP BY per toko.
        if (perTokoAktif && scopeTokoId == null) {
            List<Object[]> perTokoRaw = qShared("trxstatus_pertoko", "SELECT COALESCE(t.nama,'-'), COUNT(*), "
                    + "COALESCE(SUM(CASE WHEN selesai THEN 1 ELSE 0 END),0) FROM ("
                    + "SELECT p.toko toko, (MIN(CASE WHEN COALESCE(p.terlayani,false) THEN 1 ELSE 0 END) = 1) AS selesai "
                    + "FROM koperasi.pembelian p WHERE " + where
                    + " GROUP BY COALESCE(p.pembelian_anggota_koperasi, p.id), p.toko) x "
                    + "LEFT JOIN koperasi.toko t ON t.id = x.toko GROUP BY t.nama ORDER BY 2 DESC");
            List<Object[]> perToko = new ArrayList<Object[]>();
            for (Object[] r : perTokoRaw) {
                long totalToko = (long) num(r[1]);
                long selesaiToko = (long) num(r[2]);
                perToko.add(new Object[] { str(r[0]), Long.valueOf(totalToko), Long.valueOf(selesaiToko),
                        Long.valueOf(totalToko - selesaiToko) });
            }
            appendRekapTable(panel, "Ringkasan Transaksi per Toko",
                    "Jumlah transaksi, yang sudah selesai dilayani, dan yang masih diproses, dipecah per toko -- centang \"Per Toko\" di atas untuk melihat/menyembunyikan.",
                    "Ringkasan Transaksi Per Toko",
                    new String[] { "Toko", "Total Transaksi", "Sudah Dilayani", "Masih Diproses" },
                    new int[] { 0, 1, 1, 1 }, perToko);
        }

        // Online vs Offline -> Sinkron — transaksi yang sempat tersimpan di perangkat kasir saat
        // internet mati sebelum akhirnya terkirim ke server, dibanding transaksi yang online biasa.
        // Sumber kolom: PembelianAnggotaKoperasi.sumberTransaksi/waktuSinkron (lihat
        // pos-sync-manual-tombol-status-riwayat-tab), diisi otomatis oleh pos_offline_service.jsp
        // saat sinkronisasi. Query di tabel HEADER (pembelian_anggota_koperasi), bukan koperasi.pembelian
        // seperti panel lain di tab ini -- kolom sumber/waktu sinkron hanya ada di tabel header.
        String htmlSinkron = htmlCached("tab4sinkron", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();
                Object[] st = first(q("sinkronringkasan",
                        "SELECT COUNT(*) FILTER (WHERE COALESCE(a.sumber_transaksi,'ONLINE')='ONLINE'), "
                                + "COUNT(*) FILTER (WHERE a.sumber_transaksi='OFFLINE_SYNC'), "
                                + "AVG(EXTRACT(EPOCH FROM (a.waktu_sinkron - a.tanggal_pembayaran))) "
                                + "FILTER (WHERE a.sumber_transaksi='OFFLINE_SYNC') "
                                + "FROM koperasi.pembelian_anggota_koperasi a "
                                + "WHERE a.tanggal_pembayaran >= (" + periodeSinceCache + ")" + andToko("a")));
                long online = st == null ? 0 : (long) num(st[0]);
                long offlineSync = st == null ? 0 : (long) num(st[1]);
                Double rataKeterlambatanDetik = (st == null || st[2] == null) ? null : num(st[2]);

                LinkedHashMap<String, Double> komposisi = new LinkedHashMap<String, Double>();
                komposisi.put("Online", (double) online);
                komposisi.put("Offline -> Sinkron", (double) offlineSync);

                String ket = "Berapa transaksi yang langsung online saat dibayar, dibanding yang sempat "
                        + "tersimpan di kasir saat internet mati lalu terkirim belakangan.";
                if (offlineSync > 0 && rataKeterlambatanDetik != null) {
                    ket += " Rata-rata baru terkirim setelah " + formatJeda(rataKeterlambatanDetik) + ".";
                }
                sb.append(DashboardUiKit.donut("Transaksi Online vs Offline -> Sinkron", ket, komposisi, false,
                        "Belum ada transaksi pada periode ini."));
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(htmlSinkron));

        // Daftar transaksi keseluruhan (tabel + Excel) — rincian tiap transaksi
        List<Object[]> trx = q("daftartrx", "SELECT TO_CHAR(MAX(p.waktu),'DD Mon YYYY HH24:MI'), MAX(t.nama), "
                + "COALESCE(MAX(ak.nama),'Umum'), COALESCE(SUM(p.total),0), "
                + "CASE WHEN MIN(CASE WHEN COALESCE(p.terlayani,false) THEN 1 ELSE 0 END) = 1 THEN 'Selesai' ELSE 'Diproses' END "
                + "FROM koperasi.pembelian p LEFT JOIN koperasi.toko t ON t.id = p.toko "
                + "LEFT JOIN koperasi.anggota_koperasi ak ON ak.id = p.anggota_koperasi WHERE " + wherePembelian()
                + " GROUP BY COALESCE(p.pembelian_anggota_koperasi, p.id) ORDER BY MAX(p.waktu) DESC LIMIT 100");
        appendRekapTable(panel, "Daftar Transaksi Keseluruhan",
                "Riwayat transaksi terbaru beserta tenant, pembeli, total, dan status pelayanannya (maksimal 100 terbaru).",
                "Daftar Transaksi", new String[] { "Waktu", "Tenant", "Pembeli", "Total", "Status" },
                new int[] { 0, 0, 0, 2, 0 }, trx);

        // Ringkasan pesanan draft (belum dibayar) -- SEBELUMNYA tabel draft di bawah tak punya ringkasan
        // angka sama sekali; pimpinan harus scroll & hitung manual utk tahu seberapa besar backlog-nya.
        // TIDAK terikat filter periode di atas (qShared), sama seperti tabel draft-nya sendiri di bawah --
        // ini semua pesanan yang masih menunggu, sejak kapan pun dibuat.
        String htmlDraftKpi = htmlCached("tab4draftkpi", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();
                Object[] dk = first(qShared("draftkpi", "SELECT COUNT(*), COALESCE(SUM(d.total_biaya),0), "
                        + "COALESCE(MAX(EXTRACT(EPOCH FROM (NOW() - d.tanggal_pembayaran))),0) "
                        + "FROM koperasi.draft_pembelian_anggota_koperasi d WHERE d.lunas IS NULL" + andToko("d")));
                long jumlahDraft = dk == null ? 0 : (long) num(dk[0]);
                double nilaiDraft = dk == null ? 0 : num(dk[1]);
                double terlamaDetik = dk == null ? 0 : num(dk[2]);
                double rataNilai = jumlahDraft > 0 ? nilaiDraft / jumlahDraft : 0;

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Pesanan Menunggu", DashboardUiKit.money(jumlahDraft),
                        "belum dibayar/diselesaikan", jumlahDraft > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Nilai Menunggu", "Rp " + DashboardUiKit.money(nilaiDraft),
                        "total nilai pesanan draft", DashboardUiKit.ACCENT));
                kartu.add(new DashboardUiKit.Stat("Rata-rata Nilai", "Rp " + DashboardUiKit.money(rataNilai),
                        "per pesanan draft", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Menunggu Paling Lama",
                        jumlahDraft > 0 ? formatJeda(terlamaDetik) : "-",
                        "perlu segera ditindaklanjuti", jumlahDraft > 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
                sb.append(DashboardUiKit.descChip("Ringkasan pesanan online yang sudah dibuat member tapi belum "
                        + "dibayar/diselesaikan -- tidak terikat filter periode di atas."));
                sb.append(DashboardUiKit.cards(kartu));
                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(htmlDraftKpi));

        // Monitor pesanan online yang belum dibayar (draft)
        List<Object[]> draft = qShared("draft", "SELECT TO_CHAR(d.tanggal_pembayaran,'DD Mon YYYY HH24:MI'), "
                + "COALESCE(ak.nama,'-'), COALESCE(t.nama,'-'), COALESCE(d.total_biaya,0), COALESCE(d.keterangan,'') "
                + "FROM koperasi.draft_pembelian_anggota_koperasi d "
                + "LEFT JOIN koperasi.anggota_koperasi ak ON ak.id = d.anggota_koperasi "
                + "LEFT JOIN koperasi.toko t ON t.id = d.toko WHERE d.lunas IS NULL " + andToko("d")
                + " ORDER BY d.tanggal_pembayaran DESC LIMIT 100");
        appendRekapTable(panel, "Monitor Pesanan Online (Draft)",
                "Pesanan online yang sudah dibuat member tapi belum dibayar/diselesaikan — perlu segera ditindaklanjuti.",
                "Pesanan Draft", new String[] { "Waktu", "Pemesan", "Tenant", "Total", "Catatan" },
                new int[] { 0, 0, 0, 2, 0 }, draft);
    }

    // ---------- TAB 8: LAPORAN-LAPORAN ----------
    /**
     * Katalog ~157 laporan Kantin/POS, versi ZK NATIF -- lihat JavaDoc lengkap di
     * {@link ais.action.master.koperasi.helper.LaporanKantinZkPanel}. SEBELUMNYA ditautkan via
     * {@code <iframe>} ke {@code laporan_laporan.jsp} (JSP dimuat apa adanya); diganti 2026-07-26
     * karena Dasbor Kantin tidak boleh lagi menautkan halaman JSP di mana pun. Mesin query (
     * {@link ais.action.master.koperasi.helper.LaporanKantinUtil}) TIDAK berubah sama sekali -- hanya
     * lapisan tampilan yang dibangun ulang di ZK, jadi hasil laporan tetap identik dgn versi JSP/Desktop.
     */
    private void buildLaporanLaporan(Tabpanel panel) {
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Katalog lengkap seluruh laporan rinci Kantin/Koperasi (penjualan, kas, pembelian, persediaan, akuntansi, dst) "
                        + "-- cari atau pilih kategori, klik salah satu laporan untuk mengatur filter & menjalankannya, atau unduh langsung sbg PDF/Excel.")));
        Div wrap = new Div();
        wrap.setParent(panel);
        ais.action.master.koperasi.helper.LaporanKantinZkPanel.build(wrap);
    }

    /** TAB Laporan Keuangan: katalog terkurasi khusus akuntansi/keuangan (subset Laporan-Laporan). */
    private void buildLaporanKeuangan(Tabpanel panel) throws Exception {
        panel.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Laporan Keuangan: Neraca, Laba Rugi, Arus Kas, Buku Besar &amp; Jurnal, Kas &amp; Bank, Piutang, "
                        + "Utang/Pengadaan, Pajak, Anggaran, Gaji, dan Rekonsiliasi -- cari/pilih kategori lalu "
                        + "jalankan atau unduh PDF/Excel.")));
        Div wrap = new Div();
        wrap.setParent(panel);
        ais.action.master.koperasi.helper.LaporanKantinZkPanel.build(wrap,
                ais.action.master.koperasi.helper.LaporanKatalogData.katalogKeuangan());
    }

    // ---------- TAB 9-13: DASBOR LAIN YANG DIPAKAI ULANG (Kepatuhan, Ramalan, Promo, Stok/Mutasi, Kas Kasir) ----------
    /**
     * Muat SATU dasbor ZK lain yang sudah ada apa adanya ke dalam tab ini via {@link MyInclude} --
     * pola SAMA PERSIS dengan {@code DashboardKoperasiAction.muat()} (dasbor pengumpul Simpan Pinjam
     * dkk). SEBELUMNYA dasbor-dasbor ini (Ramalan Penjualan/forecast, Monitor Promo &amp; Cashback,
     * Stok &amp; Mutasi + rekonsiliasi Aset, Kas Kasir) hanya bisa dibuka lewat menunya sendiri-sendiri,
     * terpisah dari Dasbor Kantin -- pimpinan harus tahu ke-4 menu itu ada. Dipakai ulang LANGSUNG
     * (bukan dibangun ulang query/grafiknya) supaya setiap perbaikan pada dasbor aslinya otomatis ikut
     * tampil di sini, dan tak ada dua sumber kebenaran utk logika yang sama.
     */
    /**
     * @param teruskanScopeToko bila {@code true}, tempel parameter {@code tokoId}/{@code perToko} ke
     *     URL include sesuai pilihan "Cari Toko"/"Per Toko" admin di atas -- dasbor tujuan (lihat
     *     {@code resolveScopeTokoDenganParam} di masing-masing composer-nya) menghormati parameter ini
     *     HANYA bila pengguna belum terkunci ke toko sendiri (pedagang), jadi tak bisa dipakai membajak
     *     kunci keamanan yang sudah ada -- sekadar mewariskan pilihan admin, bukan mengganti otorisasi.
     *     {@code false} utk dasbor yang bukan laporan lintas-toko (mis. Kas Kasir).
     */
    private void muatDasborLain(Tabpanel panel, String url, boolean teruskanScopeToko) {
        if (teruskanScopeToko) {
            url += (url.contains("?") ? "&" : "?") + "tokoId=" + (scopeTokoId == null ? "" : scopeTokoId)
                    + "&perToko=" + perTokoAktif;
        }
        try {
            MyInclude include = new MyInclude(url);
            include.setWidth("100%");
            include.setParent(panel);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            panel.appendChild(DashboardUiKit.html(
                    DashboardUiKit.descChip("Dasbor ini gagal dimuat. Silakan buka menunya secara langsung.")));
        }
    }

    // ======================== Query panel (di-cache L3) ========================

    private List<Object[]> qPerformaPedagang() {
        return q("pedagang", "SELECT b.nama, COALESCE(SUM(p.total),0) omzet, COUNT(DISTINCT p.id) trx, "
                + "COALESCE(SUM(p.qty),0) qty FROM koperasi.pembelian p JOIN koperasi.toko b ON b.id = p.toko WHERE "
                + wherePembelian() + " GROUP BY b.nama ORDER BY omzet DESC");
    }

    private List<Object[]> qRekapProduk() {
        return q("rekapproduk", "SELECT b.nama, c.nama, COALESCE(SUM(p.qty),0), COALESCE(SUM(p.total),0) "
                + "FROM koperasi.pembelian p JOIN koperasi.toko b ON b.id = p.toko "
                + "JOIN koperasi.produk c ON c.id = p.produk WHERE " + wherePembelian()
                + " GROUP BY b.nama, c.nama ORDER BY b.nama ASC, 3 DESC");
    }

    private List<Object[]> qRekapPelanggan() {
        return q("rekappelanggan", "SELECT t.nama, ak.nama, COUNT(p.id), COALESCE(SUM(p.total),0) "
                + "FROM koperasi.pembelian p JOIN koperasi.toko t ON t.id = p.toko "
                + "JOIN koperasi.anggota_koperasi ak ON ak.id = p.anggota_koperasi WHERE " + wherePembelian()
                + " GROUP BY t.nama, ak.nama ORDER BY t.nama ASC, 4 DESC");
    }

    private List<Object[]> qStok() {
        return qShared("stok", "SELECT b.nama, c.nama, COALESCE(c.stok,0) FROM koperasi.produk c "
                + "JOIN koperasi.toko b ON b.id = c.toko WHERE (c.aktif = true OR c.aktif IS NULL) " + andToko("c")
                + " ORDER BY COALESCE(c.stok,0) ASC, b.nama ASC LIMIT 150");
    }

    // ======================== Tabel rekap + Unduh Excel (reusable) ========================

    /**
     * Tempel satu blok rekap ke {@code host}: judul, penjelasan, tombol Unduh Excel,
     * lalu grid datanya. {@code kinds}: 0=teks, 1=angka, 2=rupiah, 3=stok (diberi warna).
     */
    // ---------- TAB 7: RESEP, HPP & MARGIN ----------
    /**
     * Peta id produk -&gt; HPP per unit sesungguhnya: kalau produk ber-resep (kolom {@code bahanbaku}
     * terisi), HPP = rollup resep (&Sigma; qty resep &times; hargabeli tiap bahan baku, pola parsing
     * JSON persis {@code BahanBakuUtil.konsumsiBahanBaku}); kalau tidak, HPP = hargabeli produk itu
     * sendiri (produk beli-jadi yang memang dibeli seharga itu, mis. minuman kemasan titipan).
     * Sumber tunggal dipakai bersama oleh tab "Keuangan &amp; Laba" ({@link #buildKeuangan}) dan tab
     * "Resep, HPP &amp; Margin" ({@link #buildResepHpp}) agar kedua tab selalu menunjukkan angka modal
     * yang konsisten utk produk yang sama -- sebelumnya keduanya salah dengan cara yang sama
     * (memakai hargabeli produk jadi langsung, yang umumnya 0/kosong utk menu racikan).
     */
    private java.util.Map<Long, Double> petaHargaPokok() {
        final List<Object[]> semua = qShared("peta_hpp_semua", "SELECT id, COALESCE(bahanbaku,''), COALESCE(hargabeli,0) "
                + "FROM koperasi.produk p WHERE 1=1" + andToko("p"));
        java.util.Map<Long, Double> peta = new java.util.HashMap<Long, Double>();
        java.util.List<Object[]> perluResep = new java.util.ArrayList<Object[]>();
        java.util.Set<Long> idBahan = new java.util.HashSet<Long>();
        for (Object[] r : semua) {
            long id = Math.round(num(r[0]));
            String resepJson = str(r[1]).trim();
            double beli = num(r[2]);
            org.json.JSONArray items = null;
            if (!resepJson.isEmpty() && !resepJson.equals("[]")) {
                try {
                    org.json.JSONArray parsedItems = new org.json.JSONArray(resepJson);
                    if (parsedItems.length() > 0) {
                        items = parsedItems;
                    }
                } catch (Exception e) {
                    items = null;
                }
            }
            if (items == null) {
                peta.put(Long.valueOf(id), Double.valueOf(beli));
                continue;
            }
            perluResep.add(new Object[] { Long.valueOf(id), items });
            for (int i = 0; i < items.length(); i++) {
                org.json.JSONObject b = items.optJSONObject(i);
                long pid = b == null ? 0 : b.optLong("produk", 0);
                if (pid > 0) {
                    idBahan.add(Long.valueOf(pid));
                }
            }
        }
        java.util.Map<Long, Double> hargaBahan = new java.util.HashMap<Long, Double>();
        if (!idBahan.isEmpty()) {
            StringBuilder ids = new StringBuilder();
            for (Long id : idBahan) {
                if (ids.length() > 0) {
                    ids.append(',');
                }
                ids.append(id);
            }
            for (Object[] r : rows("SELECT id, COALESCE(hargabeli,0) FROM koperasi.produk WHERE id IN (" + ids + ")")) {
                hargaBahan.put(Long.valueOf(Math.round(num(r[0]))), Double.valueOf(num(r[1])));
            }
        }
        for (Object[] pr : perluResep) {
            long id = ((Long) pr[0]).longValue();
            org.json.JSONArray items = (org.json.JSONArray) pr[1];
            double hpp = 0;
            for (int j = 0; j < items.length(); j++) {
                org.json.JSONObject b = items.optJSONObject(j);
                if (b == null) {
                    continue;
                }
                Double harga = hargaBahan.get(Long.valueOf(b.optLong("produk", 0)));
                hpp += b.optDouble("qty", 0) * (harga == null ? 0 : harga.doubleValue());
            }
            peta.put(Long.valueOf(id), Double.valueOf(hpp));
        }
        return peta;
    }

    private void buildResepHpp(final Tabpanel panel) {
        // Peta HPP per unit SEMUA produk (rollup resep utk yang ber-resep, hargabeli utk sisanya) --
        // sumber tunggal yang sama dipakai buildKeuangan(), supaya angka modal antar-tab konsisten.
        final java.util.Map<Long, Double> peta = petaHargaPokok();

        // Produk ber-resep (konfigurasi produk; tidak tergantung periode) -- tarik RESEPNYA
        // (bahanbaku, JSON [{"produk":id,"qty":n}, ...]) beserta kategori, hanya utk menampilkan
        // jumlah bahan; HPP-nya sendiri diambil dari `peta` (satu sumber kebenaran).
        final List<Object[]> resepMentah = qShared("resep_mentah", "SELECT p.id, COALESCE(t.nama,'-'), p.nama, "
                + "p.bahanbaku, COALESCE(p.hargajual,0), COALESCE(jp.nama,'Lainnya') "
                + "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id = p.toko "
                + "LEFT JOIN koperasi.jenis_produk jp ON jp.id = p.jenis_produk "
                + "WHERE p.bahanbaku IS NOT NULL AND TRIM(p.bahanbaku) NOT IN ('', '[]')" + andToko("p")
                + " ORDER BY p.nama");

        // Resep terhitung: [0]=tenant [1]=menu [2]=HPP(dari peta) [3]=harga jual [4]=jml bahan [5]=kategori.
        // Indeks 0..3 SENGAJA sama dengan skema lama agar sisa method (kartu, sortable, grid) tetap valid.
        final List<Object[]> resep = new ArrayList<Object[]>();
        for (Object[] r : resepMentah) {
            long id = Math.round(num(r[0]));
            Double hpp = peta.get(Long.valueOf(id));
            int jmlBahan;
            try {
                jmlBahan = new org.json.JSONArray(str(r[3]).trim()).length();
            } catch (Exception e) {
                jmlBahan = 0;
            }
            resep.add(new Object[] { str(r[1]), str(r[2]), Double.valueOf(hpp == null ? 0 : hpp.doubleValue()),
                    Double.valueOf(num(r[4])), Integer.valueOf(jmlBahan), str(r[5]) });
        }

        // Pemakaian bahan baku per (tenant, bahan) untuk grid — tergantung periode.
        final List<Object[]> pakaiDetail = q("pemakaian", "SELECT COALESCE(t.nama,'-'), COALESCE(pr.nama,'-'), "
                + "COALESCE(SUM(pb.qty),0), COALESCE(SUM(pb.qty*COALESCE(pr.hargabeli,0)),0) "
                + "FROM koperasi.pemakaian_bahan_baku pb "
                + "LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
                + "LEFT JOIN koperasi.toko t ON t.id = pb.toko "
                + "WHERE pb.waktu >= (" + periodeSinceCache + ")" + andToko("pb")
                + " GROUP BY t.nama, pr.nama ORDER BY 3 DESC");

        String html = htmlCached("tab5", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                if (resep.isEmpty()) {
                    sb.append(DashboardUiKit.descChip("Belum ada menu yang memakai resep/bahan baku. Buka Pengaturan "
                            + "Barang, pilih sebuah produk, lalu isi bagian Bahan Baku (Resep) agar modal & untungnya terhitung otomatis."));
                    return sb.toString();
                }

                // Hitung margin tiap menu + siapkan data batang (urut untung terbesar).
                double totalMargin = 0;
                int nMargin = 0;
                double minMargin = Double.MAX_VALUE;
                String minNama = "-";
                java.util.List<Object[]> sortable = new java.util.ArrayList<Object[]>();
                for (Object[] r : resep) {
                    double hpp = num(r[2]);
                    double jual = num(r[3]);
                    double m = jual > 0 ? (jual - hpp) / jual * 100.0 : 0;
                    if (jual > 0) {
                        totalMargin += m;
                        nMargin++;
                        if (m < minMargin) {
                            minMargin = m;
                            minNama = str(r[1]);
                        }
                    }
                    sortable.add(new Object[] { str(r[1]), Double.valueOf(m) });
                }
                java.util.Collections.sort(sortable, new java.util.Comparator<Object[]>() {
                    @Override
                    public int compare(Object[] a, Object[] b) {
                        return Double.compare(num(b[1]), num(a[1]));
                    }
                });
                LinkedHashMap<String, Double> mapMargin = new LinkedHashMap<String, Double>();
                int capBar = Math.min(sortable.size(), 12);
                for (int i = 0; i < capBar; i++) {
                    mapMargin.put(DashboardUiKit.shorten(str(sortable.get(i)[0]), 18), Double.valueOf(num(sortable.get(i)[1])));
                }
                double avgMargin = nMargin > 0 ? totalMargin / nMargin : 0;
                if (minMargin == Double.MAX_VALUE) {
                    minMargin = 0;
                }
                double nilaiPakai = 0;
                for (Object[] r : pakaiDetail) {
                    nilaiPakai += num(r[3]);
                }

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Menu Ber-Resep", DashboardUiKit.money(resep.size()),
                        "menu dihitung dari bahan", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Rata-rata Margin", Math.round(avgMargin) + "%",
                        "rata-rata untung tiap menu", DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Margin Tertipis", Math.round(minMargin) + "%",
                        DashboardUiKit.shorten(minNama, 18), DashboardUiKit.BAD));
                kartu.add(new DashboardUiKit.Stat("Nilai Bahan Baku Terpakai", "Rp " + DashboardUiKit.money(nilaiPakai),
                        periodeLabelCache, DashboardUiKit.WARN));
                sb.append(DashboardUiKit.descChip("Modal (HPP) dihitung otomatis dari resep: harga tiap bahan baku "
                        + "dikalikan jumlah yang dipakai, dijumlahkan jadi modal per menu -- bukan angka yang diisi "
                        + "manual. Dari situ kelihatan menu mana yang untungnya sehat dan mana yang nyaris tidak untung."));
                sb.append(DashboardUiKit.cards(kartu));

                // Tren pemakaian bahan baku harian.
                List<Double> tren = new ArrayList<Double>();
                for (Object[] r : q("tren_pakai", "SELECT TO_CHAR(DATE(pb.waktu),'DD Mon'), COALESCE(SUM(pb.qty),0) "
                        + "FROM koperasi.pemakaian_bahan_baku pb WHERE pb.waktu >= (" + periodeSinceCache + ")"
                        + andToko("pb") + " GROUP BY DATE(pb.waktu) ORDER BY DATE(pb.waktu)")) {
                    tren.add(Double.valueOf(num(r[1])));
                }
                sb.append(DashboardUiKit.sparkline("Tren Pemakaian Bahan Baku",
                        "Banyaknya bahan baku yang terpakai dari hari ke hari.", tren, DashboardUiKit.ACCENT,
                        "Belum ada pemakaian bahan baku pada periode ini."));

                sb.append(DashboardUiKit.openGrid(320));

                sb.append(DashboardUiKit.barList("Untung (Margin) Tiap Menu",
                        "Menu mana yang paling untung, dan mana yang nyaris tidak untung.", mapMargin,
                        DashboardUiKit.GOOD, "%", false, "Belum ada menu ber-resep."));

                LinkedHashMap<String, Double> mapPakai = new LinkedHashMap<String, Double>();
                for (Object[] r : q("pakai_top", "SELECT COALESCE(pr.nama,'-'), COALESCE(SUM(pb.qty),0) "
                        + "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
                        + "WHERE pb.waktu >= (" + periodeSinceCache + ")" + andToko("pb")
                        + " GROUP BY pr.nama ORDER BY 2 DESC LIMIT 12")) {
                    mapPakai.put(DashboardUiKit.shorten(str(r[0]), 18), Double.valueOf(num(r[1])));
                }
                sb.append(DashboardUiKit.barList("Bahan Baku Paling Banyak Terpakai",
                        "Bahan yang paling cepat habis — pastikan stoknya selalu siap.", mapPakai,
                        DashboardUiKit.ACCENT, "unit", false, "Belum ada pemakaian bahan baku."));

                sb.append(DashboardUiKit.closeGrid());

                // Jaring laba-laba: kekuatan margin per kategori menu -- dihitung dari HPP resep
                // (variabel `resep`, sudah benar) yang dikelompokkan per kategori, BUKAN query
                // terpisah yang tanpa sadar mengulang bug lama (hargajual-hargabeli produk jadi).
                LinkedHashMap<String, double[]> sumKat = new LinkedHashMap<String, double[]>();
                for (Object[] r : resep) {
                    double jual = num(r[3]);
                    if (jual <= 0) {
                        continue;
                    }
                    double m = (jual - num(r[2])) / jual * 100.0;
                    String namaKat = str(r[5]);
                    double[] acc = sumKat.get(namaKat);
                    if (acc == null) {
                        acc = new double[2];
                        sumKat.put(namaKat, acc);
                    }
                    acc[0] += m;
                    acc[1] += 1;
                }
                List<Object[]> kat = new java.util.ArrayList<Object[]>();
                for (java.util.Map.Entry<String, double[]> en : sumKat.entrySet()) {
                    kat.add(new Object[] { en.getKey(), Double.valueOf(en.getValue()[0] / en.getValue()[1]) });
                }
                java.util.Collections.sort(kat, new java.util.Comparator<Object[]>() {
                    @Override
                    public int compare(Object[] a, Object[] b) {
                        return Double.compare(num(b[1]), num(a[1]));
                    }
                });
                if (kat.size() >= 3) {
                    double maxM = 0;
                    int n = Math.min(kat.size(), 7);
                    for (int i = 0; i < n; i++) {
                        maxM = Math.max(maxM, num(kat.get(i)[1]));
                    }
                    String[] labels = new String[n];
                    int[] vals = new int[n];
                    for (int i = 0; i < n; i++) {
                        labels[i] = DashboardUiKit.shorten(str(kat.get(i)[0]), 12);
                        vals[i] = maxM <= 0 ? 0 : (int) Math.round(num(kat.get(i)[1]) * 100.0 / maxM);
                    }
                    sb.append(DashboardUiKit.spider("Kekuatan Margin per Kategori",
                            "Kelompok menu mana yang untungnya paling tebal.", labels, vals));
                }

                return sb.toString();
            }
        });
        panel.appendChild(DashboardUiKit.html(html));

        if (resep.isEmpty()) {
            return;
        }

        // Grid 1: rekap resep, HPP & margin per menu (dengan tombol Unduh Excel).
        List<Object[]> gridResep = new ArrayList<Object[]>();
        for (Object[] r : resep) {
            double hpp = num(r[2]);
            double jual = num(r[3]);
            double untung = jual - hpp;
            double m = jual > 0 ? (untung / jual * 100.0) : 0;
            gridResep.add(new Object[] { str(r[0]), str(r[1]), Integer.valueOf((int) num(r[4])), Double.valueOf(hpp),
                    Double.valueOf(jual), Double.valueOf(untung), Long.valueOf(Math.round(m)) });
        }
        appendRekapTable(panel, "Rekap Resep, HPP & Margin per Menu",
                "Modal (HPP) dihitung otomatis: harga tiap bahan baku dikalikan jumlah yang dipakai di resep, "
                        + "dijumlahkan jadi modal per menu -- bukan angka yang diisi manual.", "Resep HPP Margin",
                new String[] { "Tenant", "Menu", "Bahan", "HPP (Modal)", "Harga Jual", "Untung (Rp)", "Margin %" },
                new int[] { 0, 0, 1, 2, 2, 2, 4 }, gridResep);

        // Grid 2: rekap pemakaian bahan baku pada periode.
        appendRekapTable(panel, "Rekap Pemakaian Bahan Baku",
                "Berapa banyak tiap bahan baku terpakai beserta nilainya pada " + periodeLabelCache + ".",
                "Pemakaian Bahan Baku",
                new String[] { "Tenant", "Bahan Baku", "Qty Terpakai", "Nilai (Rp)" },
                new int[] { 0, 0, 1, 2 }, pakaiDetail);

        // "Per Toko" (2026-07-26, lanjutan) -- rata-rata margin seluruh menu ber-resep dipecah per
        // toko, dihitung dari `resep` (variabel yang SAMA dipakai jaring laba-laba kategori di atas),
        // bukan query baru.
        if (perTokoAktif && scopeTokoId == null) {
            LinkedHashMap<String, double[]> sumToko = new LinkedHashMap<String, double[]>();
            for (Object[] r : resep) {
                double jual = num(r[3]);
                if (jual <= 0) {
                    continue;
                }
                double m = (jual - num(r[2])) / jual * 100.0;
                String tenant = str(r[0]);
                double[] acc = sumToko.get(tenant);
                if (acc == null) {
                    acc = new double[2];
                    sumToko.put(tenant, acc);
                }
                acc[0] += m;
                acc[1] += 1;
            }
            List<Object[]> perToko = new ArrayList<Object[]>();
            for (java.util.Map.Entry<String, double[]> en : sumToko.entrySet()) {
                perToko.add(new Object[] { en.getKey(), Integer.valueOf((int) en.getValue()[1]),
                        Double.valueOf(en.getValue()[0] / en.getValue()[1]) });
            }
            java.util.Collections.sort(perToko, new java.util.Comparator<Object[]>() {
                @Override
                public int compare(Object[] a, Object[] b) {
                    return Double.compare(num(b[2]), num(a[2]));
                }
            });
            appendRekapTable(panel, "Rata-rata Margin Menu per Toko",
                    "Rata-rata persentase margin seluruh menu ber-resep, dipecah per toko -- centang \"Per Toko\" di atas untuk melihat/menyembunyikan.",
                    "Margin Menu Per Toko", new String[] { "Toko", "Jumlah Menu", "Rata-rata Margin" },
                    new int[] { 0, 1, 4 }, perToko);
        }
    }

    private void appendRekapTable(Component host, String judul, String desc, final String sheetName,
            final String[] headers, final int[] kinds, final List<Object[]> data) {
        host.appendChild(DashboardUiKit.html(
                "<div style='font-size:14px;font-weight:800;color:#0f172a;margin:18px 0 4px;border-left:4px solid "
                        + DashboardUiKit.PRIMARY + ";padding-left:10px;'>" + DashboardUiKit.esc(judul) + "</div>"));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(desc)));

        if (data == null || data.isEmpty()) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:#64748b;padding:10px 4px;'>Belum ada data untuk ditampilkan.</div>"));
            return;
        }

        // Wadah per-tabel: tombol + grid dikumpulkan dalam satu Div supaya tombol "Cetak PDF"
        // mencetak HANYA tabel ini, bukan seluruh tabel yang ada di tab.
        Div wadah = new Div();
        wadah.setParent(host);

        Div bar = new Div();
        bar.setStyle("text-align:right;margin:2px 0 6px;");
        // Cetak PDF memakai mesin bersama DashboardGridExportHelper (membaca isi grid apa adanya).
        // Tombol Excel di bawah TIDAK diganti dgn milik helper krn versi lokal ini menulis angka
        // sebagai sel numerik asli dari data mentah, bukan teks hasil pembacaan grid.
        ais.ui.util.DashboardGridExportHelper.pasangTombolPdf(bar, wadah, judul);
        MyToolbarbuttonConfig dl = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.png");
        dl.setTooltiptext("Unduh tabel ini sebagai berkas Excel");
        dl.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                try {
                    XSSFWorkbook wb = new XSSFWorkbook();
                    writeSheet(wb, sheetName, sheetName + " - " + periodeLabelCache, headers, kinds, data);
                    Filedownload.save(saveWorkbook(wb, "rekap_kantin_" + sanitize(sheetName)), XLSX_MIME);
                } catch (Exception ex) {
                    Common.tampilErrorJikaAdmin(ex);
                }
            }
        });
        dl.setParent(bar);
        bar.setParent(wadah);

        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setStyle("border:0;background:transparent;");
        if (data.size() > 25) {
            grid.setMold("paging");
            grid.setPageSize(25);
        }
        Columns columns = new Columns();
        columns.setParent(grid);
        columns.setSizable(true);
        addCol(columns, "No.", "56px");
        for (String h : headers) {
            addCol(columns, h, null);
        }
        Rows rows = new Rows();
        rows.setParent(grid);
        int no = 1;
        for (Object[] r : data) {
            Row row = new Row();
            row.setValign("middle");
            row.setParent(rows);
            new Label(String.valueOf(no++)).setParent(row);
            for (int i = 0; i < headers.length; i++) {
                Object v = i < r.length ? r[i] : null;
                Label lbl;
                int k = kinds[i];
                if (k == 2) {
                    lbl = new Label("Rp " + DashboardUiKit.money(num(v)));
                    lbl.setStyle("font-weight:700;color:" + DashboardUiKit.GOOD + ";");
                } else if (k == 3) {
                    double st = num(v);
                    lbl = new Label(DashboardUiKit.money(st));
                    String warna = st <= 5 ? DashboardUiKit.BAD : (st <= 10 ? DashboardUiKit.WARN : DashboardUiKit.INK);
                    lbl.setStyle("font-weight:800;color:" + warna + ";");
                } else if (k == 1) {
                    lbl = new Label(DashboardUiKit.money(num(v)));
                } else if (k == 4) {
                    // Persentase margin + lencana status (sehat/tipis/rugi) -- ambang batas sama
                    // dengan yang dipakai grafik batang margin di atas, supaya konsisten.
                    double m = num(v);
                    String warna = m < 0 ? DashboardUiKit.BAD : (m < 15 ? DashboardUiKit.WARN : DashboardUiKit.GOOD);
                    String label = m < 0 ? "Rugi" : (m < 15 ? "Tipis" : "Sehat");
                    org.zkoss.zul.Html badge = new org.zkoss.zul.Html(
                            "<div style='display:flex;align-items:center;justify-content:flex-end;gap:6px;'>"
                                    + "<span style='font-weight:800;color:" + warna + ";'>" + Math.round(m) + "%</span>"
                                    + "<span style='display:inline-block;padding:.22em .6em;border-radius:999px;"
                                    + "font-size:10.5px;font-weight:800;background:" + warna + "1F;color:" + warna
                                    + ";'>" + label + "</span></div>");
                    badge.setParent(row);
                    continue;
                } else {
                    lbl = new Label(str(v));
                    if (i == 0) {
                        lbl.setStyle("font-weight:700;");
                    }
                }
                lbl.setParent(row);
            }
        }
        grid.setParent(wadah);
    }

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
    }

    // ======================== Penulisan Excel ========================

    private void writeSheet(XSSFWorkbook wb, String sheetName, String judul, String[] headers, int[] kinds,
            List<Object[]> data) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        sheet.setDefaultColumnWidth(22);
        int ri = 0;
        sheet.createRow(ri++).createCell(0).setCellValue(judul == null ? sheetName : judul);
        XSSFRow head = sheet.createRow(ri++);
        head.createCell(0).setCellValue("No.");
        for (int i = 0; i < headers.length; i++) {
            head.createCell(i + 1).setCellValue(headers[i]);
        }
        int no = 1;
        if (data != null) {
            for (Object[] r : data) {
                XSSFRow row = sheet.createRow(ri++);
                row.createCell(0).setCellValue(no++);
                for (int i = 0; i < headers.length; i++) {
                    Object v = i < r.length ? r[i] : null;
                    if (kinds[i] == 0) {
                        row.createCell(i + 1).setCellValue(str(v));
                    } else {
                        row.createCell(i + 1).setCellValue(num(v));
                    }
                }
            }
        }
        Common.setStyled(sheet);
    }

    private File saveWorkbook(XSSFWorkbook wb, String prefix) throws Exception {
        String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/" + prefix + "_"
                + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
                + ".xlsx");
        File file = new File(filename);
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(filename);
        try {
            wb.write(out);
        } finally {
            out.close();
        }
        return file;
    }

    private static String sanitize(String s) {
        return s == null ? "data" : s.replaceAll("[^A-Za-z0-9]", "_");
    }

    // ======================== Helper cache & SQL ========================

    private interface HtmlBuilder {
        String build();
    }

    /** Bangun & cache potongan HTML grafik per-tab di L1 (90 dtk) agar buka-tutup tab instan. */
    private String htmlCached(String tabKey, final HtmlBuilder builder) {
        try {
            return DashboardCache.l1("kantin.html." + tabKey + "." + periodeKeyCache + "." + scopeKey(),
                    new DashboardCache.Loader<String>() {
                        @Override
                        public String load() throws Exception {
                            return builder.build();
                        }
                    });
        } catch (Exception e) {
            return builder.build();
        }
    }

    /** Hasil query agregat per-panel, di-cache L3 (30 mnt, dipakai bersama), per periode. */
    private List<Object[]> q(String panelKey, final String sql) {
        try {
            return DashboardCache.l3("kantin." + panelKey + "." + periodeKeyCache + "." + scopeKey(),
                    new DashboardCache.Loader<List<Object[]>>() {
                        @Override
                        public List<Object[]> load() throws Exception {
                            return rows(sql);
                        }
                    });
        } catch (Exception e) {
            return rows(sql);
        }
    }

    /** Seperti {@link #q} tapi tanpa pembeda periode (data yang sama untuk semua periode, mis. stok). */
    private List<Object[]> qShared(String panelKey, final String sql) {
        try {
            return DashboardCache.l3("kantin." + panelKey + "." + scopeKey(), new DashboardCache.Loader<List<Object[]>>() {
                @Override
                public List<Object[]> load() throws Exception {
                    return rows(sql);
                }
            });
        } catch (Exception e) {
            return rows(sql);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        // Baca via JDBC ResultSet (getObject) agar TIDAK bergantung pada autodiscovery tipe kolom
        // Hibernate untuk native SQL. Autodiscovery sempat memetakan kolom teks (mis. metode bayar
        // "Tunai") sebagai double sehingga melempar:
        //   org.postgresql.util.PSQLException: Bad value for type double : Tunai
        // getObject() mengembalikan tipe natural (String untuk teks, Number untuk numerik) yang
        // langsung kompatibel dengan helper num()/str(). Tiap baris selalu dibungkus Object[]
        // sepanjang jumlah kolom (1 kolom -> Object[]{nilai}) sehingga semua pemanggil tetap aman.
        // Memakai openSession() (session terisolasi) yang WAJIB ditutup di finally. Cache hasil
        // tetap ditangani di lapis aplikasi (DashboardCache l1/l3), jadi melepas query cache
        // Hibernate tidak mengurangi performa.
        org.hibernate.Session session = null;
        java.sql.Statement st = null;
        java.sql.ResultSet rs = null;
        List<Object[]> out = new ArrayList<Object[]>();
        try {
            session = HibernateUtil.openSession();
            java.sql.Connection conn = session.connection();
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] r = new Object[cols];
                for (int i = 1; i <= cols; i++) {
                    r[i - 1] = rs.getObject(i);
                }
                out.add(r);
            }
            return out;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardKantinAction.java:1043");}
            try { if (st != null) st.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardKantinAction.java:1044");}
            // closeSessionQuietly: rollback (autocommit=false) + disconnect + close, aman bila null.
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private Object[] first(List<Object[]> r) {
        return r == null || r.isEmpty() ? null : r.get(0);
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** Format jeda waktu (detik) jadi teks awam ("2 jam 14 menit", "45 detik") — dipakai panel Online vs Offline-Sinkron. */
    private static String formatJeda(double detik) {
        long d = Math.max(0, Math.round(detik));
        if (d < 60) {
            return d + " detik";
        }
        long menit = d / 60;
        if (menit < 60) {
            return menit + " menit";
        }
        long jam = menit / 60;
        long sisaMenit = menit % 60;
        return jam + " jam" + (sisaMenit > 0 ? (" " + sisaMenit + " menit") : "");
    }
}

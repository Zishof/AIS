package ais.action.master.koperasi;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
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

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardCache;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h2>Dasbor Kepatuhan Operasional Kantin/POS (2026-07-26).</h2>
 *
 * <p><b>Kenapa dasbor ini ada.</b> Seluruh dasbor Kantin yang sudah ada menjawab pertanyaan
 * "berapa penjualannya" (omzet, laba, produk terlaris, peringkat mitra). Tidak satu pun menjawab
 * pertanyaan pengawasan: <i>"apakah prosedur benar-benar dijalankan di lapangan, dan adakah pola
 * yang perlu diperiksa?"</i>. Dasbor ini khusus menyajikan SINYAL RISIKO &amp; KEPATUHAN untuk
 * pimpinan/pengawas, bukan angka penjualan.</p>
 *
 * <p><b>Empat sinyal yang disajikan</b> (semuanya dari data yang SUDAH tercatat, tanpa tabel baru):
 * <ol>
 *   <li><b>Stok opname tidak dijalankan</b> — toko yang belum pernah/terlalu lama tidak menutup sesi
 *       stok opname ({@code koperasi.sesi_stok_opname} status SELESAI).</li>
 *   <li><b>Selisih stok opname</b> — nilai rupiah barang hilang/lebih saat opname
 *       ({@code koperasi.stok_opname.selisih} dikalikan harga beli produk).</li>
 *   <li><b>Sesi kas kasir</b> — sesi yang lupa ditutup (masih BUKA berjam-jam/berhari-hari) dan
 *       sesi tertutup yang uang fisiknya tidak cocok ({@code selisih} bukan nol).</li>
 *   <li><b>Diskon manual tanpa aturan</b> — baris penjualan yang diberi diskon padahal TIDAK
 *       terhubung ke satu pun {@code AturanDiskon} ({@code koperasi.pembelian.diskon &gt; 0 AND
 *       aturan_diskon IS NULL}), dikelompokkan per kasir. Ini diskon yang diketik manual oleh kasir,
 *       bukan diskon yang dihitung otomatis dari aturan yang disetujui.</li>
 * </ol>
 *
 * <p><b>Sinyal ke-5: pembatalan transaksi.</b> Sampai 2026-07-26 laporan ini tidak mungkin dibuat:
 * pembatalan transaksi menghapus datanya secara fisik tanpa jejak apa pun (tidak ada kolom
 * status/void/batal di {@code PembelianAnggotaKoperasi}). Sejak diperkenalkannya arsip
 * {@code koperasi.pembatalan_transaksi} (lihat {@code PembatalanTransaksiKantin} dan
 * {@code PembatalanTransaksiUtil}), setiap pembatalan wajib disertai ALASAN dan seluruh isi
 * transaksinya dipotret ke arsip sebelum dihapus — sehingga panel "Pembatalan Transaksi" di bawah
 * kini menyajikan data sungguhan: siapa membatalkan apa, kapan, senilai berapa, dan kenapa.</p>
 *
 * <p><b>Pembatasan toko.</b> Mengikuti pola dasbor Kantin lain: pedagang yang tidak boleh melihat
 * toko lain otomatis terkunci ke tokonya sendiri, dan saat dimuat sebagai tab di Dasbor Kantin
 * parameter {@code tokoId} dari sana ikut dihormati (hanya bila belum terkunci).</p>
 *
 * <p><b>Pembacaan data.</b> Memakai pola {@code rows()} berbasis JDBC {@code getObject()} yang sama
 * dengan {@code DashboardKantinAction} — sengaja BUKAN {@code SQLQuery} auto-detect tipe, karena
 * query di sini banyak mencampur kolom teks dan numerik dalam satu SELECT (pola yang pernah memicu
 * {@code PSQLException: Bad value for type double} di produksi), dan karena memakai
 * {@code openSession()} terisolasi sebuah query yang gagal tidak akan meracuni transaksi
 * thread-bound milik halaman.</p>
 */
public class DashboardKepatuhanKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Ambang "sesi kas terlalu lama dibiarkan terbuka" (jam) -- di atas ini disorot merah. */
    private static final double AMBANG_SESI_KAS_JAM = 24.0;
    /** Ambang "toko terlambat stok opname" (hari sejak opname terakhir selesai). */
    private static final double AMBANG_OPNAME_HARI = 30.0;
    /** Nilai penanda "belum pernah opname sama sekali" (dipakai agar urutan SQL menaruhnya paling atas). */
    private static final double OPNAME_BELUM_PERNAH = 99999.0;

    private Combobox searchPeriode;
    private Div dashHost;

    private String periodeSinceCache = "CURRENT_DATE - INTERVAL '29 days'";
    private String periodeKeyCache = "30_Hari_Terakhir";
    private String periodeLabelCache = "30 Hari Terakhir";

    private Long scopeTokoId = null;
    private String scopeTokoNama = null;

    // ======================== Lifecycle ========================

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

        String judul = scopeTokoId == null ? "Dasbor Kepatuhan Operasional"
                : "Dasbor Kepatuhan Operasional — " + scopeTokoNama;
        String desc = "Sinyal pengawasan dari data yang sudah tercatat: toko yang belum stok opname, "
                + "selisih hasil opname, sesi kas kasir yang lupa ditutup atau uangnya tidak cocok, "
                + "serta diskon yang diketik manual tanpa aturan diskon. Bukan laporan penjualan — "
                + "halaman ini khusus untuk menemukan hal yang perlu ditindaklanjuti.";
        if (scopeTokoId != null) {
            desc = "Khusus data toko " + scopeTokoNama + ". " + desc;
        }
        DashboardUiKit.attachIntro(comp, judul, desc);

        isiPeriode();
        buildDashboard();
    }

    /**
     * Kunci lingkup toko. Pedagang yang tidak boleh melihat toko lain terkunci ke tokonya sendiri;
     * bila belum terkunci, parameter URL {@code tokoId} (diteruskan Dasbor Kantin saat halaman ini
     * dimuat sebagai tab) dihormati. Urutan ini penting: parameter URL TIDAK PERNAH bisa membuka
     * kunci pedagang, hanya menyaring lebih jauh saat pengguna memang berhak melihat semua toko.
     */
    private void resolveScopeToko() {
        try {
            Toko ct = Common.getCurrentToko();
            if (ct != null && ct.getId() != null) {
                Boolean b = ct.getBolehMelihatTokolain();
                if (b == null || !b.booleanValue()) {
                    scopeTokoId = ct.getId();
                    scopeTokoNama = ct.getNama();
                }
            }
        } catch (Exception ignore) {
            ais.common.ErrorAuditUtil.record(ignore,
                    "auto-audit(empty-catch) DashboardKepatuhanKantinAction.resolveScopeToko");
        }
        if (scopeTokoId == null) {
            try {
                String tp = org.zkoss.zk.ui.Executions.getCurrent().getParameter("tokoId");
                if (tp != null && tp.trim().length() > 0) {
                    Toko t = (Toko) HibernateUtil.currentSession().get(Toko.class, Long.valueOf(tp.trim()));
                    if (t != null) {
                        scopeTokoId = t.getId();
                        scopeTokoNama = t.getNama();
                    }
                }
            } catch (Exception ignore) {
                ais.common.ErrorAuditUtil.record(ignore,
                        "auto-audit(empty-catch) DashboardKepatuhanKantinAction.tokoIdParam");
            }
        }
    }

    /** Pembatas toko untuk tabel yang punya KOLOM FK bernama {@code toko}. */
    private String andToko(String alias) {
        return scopeTokoId == null ? "" : (" AND " + alias + ".toko = " + scopeTokoId);
    }

    /** Pembatas toko untuk tabel {@code koperasi.toko} itu sendiri (kuncinya {@code id}, bukan {@code toko}). */
    private String andTokoSelf(String alias) {
        return scopeTokoId == null ? "" : (" AND " + alias + ".id = " + scopeTokoId);
    }

    private String scopeKey() {
        return scopeTokoId == null ? "all" : ("t" + scopeTokoId);
    }

    // ======================== Periode ========================

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

    // ======================== Aksi ========================

    public void onRefresh(Event event) throws Exception {
        buildDashboard();
    }

    public void onDownload(Event event) throws Exception {
        try {
            XSSFWorkbook wb = new XSSFWorkbook();
            writeSheet(wb, "Kepatuhan Opname", "Kepatuhan Stok Opname per Toko", HEAD_OPNAME, KIND_OPNAME,
                    barisOpname(qOpnameTerakhir()));
            writeSheet(wb, "Selisih Opname", "Selisih Stok Opname - " + periodeLabelCache, HEAD_SELISIH_OPNAME,
                    KIND_SELISIH_OPNAME, qSelisihOpname());
            writeSheet(wb, "Sesi Kas Terbuka", "Sesi Kas Masih Terbuka", HEAD_KAS_BUKA, KIND_KAS_BUKA,
                    qSesiKasTerbuka());
            writeSheet(wb, "Selisih Kas", "Selisih Kas Kasir - " + periodeLabelCache, HEAD_KAS_SELISIH,
                    KIND_KAS_SELISIH, qSelisihKas());
            writeSheet(wb, "Diskon Manual", "Diskon Manual Tanpa Aturan - " + periodeLabelCache, HEAD_DISKON,
                    KIND_DISKON, qDiskonManual());
            Filedownload.save(saveWorkbook(wb, "rekap_kepatuhan_kantin"), XLSX_MIME);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Mohon maaf, gagal menyiapkan berkas Excel. Silakan muat ulang halaman dan "
                    + "coba lagi; hubungi Administrator bila masalah berlanjut.", "Informasi",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        }
    }

    // ======================== Build ========================

    private static final String[] HEAD_OPNAME =
            { "Toko", "Opname Terakhir", "Selang (hari)", "Sesi Selesai (periode)", "Status" };
    private static final int[] KIND_OPNAME = { 0, 0, 0, 1, 0 };
    private static final String[] HEAD_SELISIH_OPNAME =
            { "Toko", "Item Diperiksa", "Item Berselisih", "Nilai Selisih" };
    private static final int[] KIND_SELISIH_OPNAME = { 0, 1, 1, 5 };
    private static final String[] HEAD_KAS_BUKA = { "Toko", "Kasir", "Dibuka", "Sudah Berjalan (jam)" };
    private static final int[] KIND_KAS_BUKA = { 0, 0, 0, 6 };
    private static final String[] HEAD_KAS_SELISIH =
            { "Toko", "Kasir", "Ditutup", "Modal Awal", "Uang Fisik", "Selisih" };
    private static final int[] KIND_KAS_SELISIH = { 0, 0, 0, 2, 2, 5 };
    private static final String[] HEAD_DISKON = { "Toko", "Kasir", "Transaksi", "Total Diskon Manual" };
    private static final int[] KIND_DISKON = { 0, 0, 1, 2 };
    private static final String[] HEAD_HAPUS =
            { "Waktu Dibatalkan", "Dibatalkan Oleh", "Toko", "Pembeli", "Nilai Transaksi", "Alasan", "Sudah Diposting" };
    private static final int[] KIND_HAPUS = { 0, 0, 0, 0, 2, 0, 0 };

    private void buildDashboard() {
        if (dashHost == null) {
            return;
        }
        dashHost.getChildren().clear();
        periodeSinceCache = periodeSince();
        periodeLabelCache = periodeLabel();
        periodeKeyCache = periodeLabelCache.replaceAll("[^a-zA-Z0-9]", "_");

        final List<Object[]> opnameRaw = qOpnameTerakhir();
        final List<Object[]> selisihOpname = qSelisihOpname();
        final List<Object[]> kasTerbuka = qSesiKasTerbuka();
        final List<Object[]> selisihKas = qSelisihKas();
        final List<Object[]> diskonManual = qDiskonManual();

        // ── Kartu KPI ringkas (satu tatapan: ada berapa hal yang perlu ditindaklanjuti) ──
        String html = htmlCached("kpi", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                int tokoTelatOpname = 0;
                for (Object[] r : opnameRaw) {
                    if (num(r[2]) >= AMBANG_OPNAME_HARI) {
                        tokoTelatOpname++;
                    }
                }
                int kasLama = 0;
                for (Object[] r : kasTerbuka) {
                    if (num(r[3]) >= AMBANG_SESI_KAS_JAM) {
                        kasLama++;
                    }
                }
                double totalSelisihKas = 0;
                for (Object[] r : selisihKas) {
                    totalSelisihKas += Math.abs(num(r[5]));
                }
                double nilaiSelisihOpname = 0;
                for (Object[] r : selisihOpname) {
                    nilaiSelisihOpname += num(r[3]);
                }
                double totalDiskonManual = 0;
                for (Object[] r : diskonManual) {
                    totalDiskonManual += num(r[3]);
                }

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Toko Telat Opname", DashboardUiKit.money(tokoTelatOpname),
                        "belum opname lebih dari " + (int) AMBANG_OPNAME_HARI + " hari",
                        tokoTelatOpname > 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Sesi Kas Lupa Ditutup", DashboardUiKit.money(kasLama),
                        "masih terbuka lebih dari " + (int) AMBANG_SESI_KAS_JAM + " jam",
                        kasLama > 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Selisih Kas", "Rp " + DashboardUiKit.money(totalSelisihKas),
                        "total ketidakcocokan uang fisik, " + periodeLabelCache,
                        totalSelisihKas > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Selisih Stok Opname",
                        "Rp " + DashboardUiKit.money(nilaiSelisihOpname),
                        "nilai selisih fisik vs sistem, " + periodeLabelCache,
                        nilaiSelisihOpname < 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Diskon Manual", "Rp " + DashboardUiKit.money(totalDiskonManual),
                        "diskon tanpa aturan, " + periodeLabelCache,
                        totalDiskonManual > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
                sb.append(DashboardUiKit.descChip("Ringkasan hal yang perlu ditindaklanjuti. Angka nol berwarna "
                        + "hijau berarti tidak ada temuan pada lingkup &amp; periode ini."));
                sb.append(DashboardUiKit.cards(kartu));
                return sb.toString();
            }
        });
        dashHost.appendChild(DashboardUiKit.html(html));

        // ── 1. Kepatuhan stok opname per toko ──
        appendRekapTable(dashHost, "Kepatuhan Stok Opname per Toko",
                "Kapan tiap toko terakhir MENYELESAIKAN sesi stok opname. Merah = belum pernah atau sudah lebih "
                        + "dari " + (int) AMBANG_OPNAME_HARI + " hari. Tidak terikat filter periode di atas — "
                        + "yang dilihat adalah kondisi terkini tiap toko.",
                "Kepatuhan Opname", HEAD_OPNAME, KIND_OPNAME, barisOpname(opnameRaw));

        // ── 2. Selisih hasil opname ──
        appendRekapTable(dashHost, "Selisih Hasil Stok Opname",
                "Nilai rupiah selisih antara stok fisik dan stok sistem saat opname pada " + periodeLabelCache
                        + " (selisih dikalikan harga beli). Angka MINUS berarti barang kurang dari catatan — "
                        + "inilah yang paling perlu ditelusuri.",
                "Selisih Opname", HEAD_SELISIH_OPNAME, KIND_SELISIH_OPNAME, selisihOpname);

        // ── 3. Sesi kas yang masih terbuka ──
        appendRekapTable(dashHost, "Sesi Kas Kasir Masih Terbuka",
                "Sesi kas yang belum ditutup sampai sekarang. Merah = sudah lebih dari "
                        + (int) AMBANG_SESI_KAS_JAM + " jam, biasanya berarti kasir lupa menutup sesi — "
                        + "akibatnya laporan kas hari itu tidak pernah dicocokkan. Tidak terikat filter periode.",
                "Sesi Kas Terbuka", HEAD_KAS_BUKA, KIND_KAS_BUKA, kasTerbuka);

        // ── 4. Selisih kas saat penutupan ──
        appendRekapTable(dashHost, "Selisih Kas Saat Penutupan Sesi",
                "Sesi kas yang sudah ditutup TAPI uang fisiknya tidak sama dengan yang seharusnya, pada "
                        + periodeLabelCache + ". Diurutkan dari selisih terbesar. Minus = uang kurang.",
                "Selisih Kas", HEAD_KAS_SELISIH, KIND_KAS_SELISIH, selisihKas);

        // ── 5. Diskon manual tanpa aturan, per kasir ──
        appendRekapTable(dashHost, "Diskon Manual Tanpa Aturan (per Kasir)",
                "Penjualan yang diberi potongan harga padahal TIDAK terhubung ke satu pun Aturan Diskon yang "
                        + "disetujui — artinya potongan itu diketik manual saat transaksi. Wajar dalam jumlah kecil, "
                        + "tetapi nilai yang besar atau terpusat pada satu kasir pantas ditanyakan. Periode: "
                        + periodeLabelCache + ".",
                "Diskon Manual", HEAD_DISKON, KIND_DISKON, diskonManual);

        // ── 6. Transaksi dihapus (jejak audit Envers) -- lihat CATATAN PENTING di JavaDoc kelas ──
        buildTransaksiDihapus(dashHost);
    }

    /**
     * Panel "Pembatalan Transaksi" — dibaca dari arsip pembatalan
     * ({@code koperasi.pembatalan_transaksi}, lihat {@code PembatalanTransaksiKantin}).
     *
     * <p>Sebelum arsip itu ada (2026-07-26), panel ini terpaksa membaca tabel audit Envers karena
     * pembatalan menghapus transaksi tanpa jejak apa pun. Sekarang sumbernya adalah tabel aplikasi
     * biasa yang memuat hal terpenting bagi pengawasan: <b>alasan</b> pembatalan. Tetap dijaga
     * pengecekan keberadaan tabel supaya instalasi yang belum menjalankan migrasi tidak error,
     * melainkan mendapat keterangan yang jelas.</p>
     */
    private void buildTransaksiDihapus(Component host) {
        final String tabelArsip = "koperasi.pembatalan_transaksi";
        org.hibernate.Session cek = null;
        boolean ada;
        try {
            cek = HibernateUtil.openSession();
            ada = tabelAda(cek, tabelArsip);
        } catch (Exception e) {
            ada = false;
        } finally {
            HibernateUtil.closeSessionQuietly(cek);
        }

        if (!ada) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='font-size:14px;font-weight:800;color:#0f172a;margin:18px 0 4px;border-left:4px solid "
                            + DashboardUiKit.PRIMARY + ";padding-left:10px;'>Pembatalan Transaksi</div>"));
            host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                    "Tabel arsip pembatalan belum tersedia di basis data ini. Jalankan berkas migrasi "
                            + "MIGRASI_PEMBATALAN_TRANSAKSI.sql terlebih dahulu, setelah itu setiap pembatalan "
                            + "transaksi beserta alasannya akan otomatis tercatat dan tampil di sini.")));
            return;
        }

        List<Object[]> data = qPembatalanTransaksi();
        appendRekapTable(host, "Pembatalan Transaksi",
                "Transaksi penjualan yang DIBATALKAN pada " + periodeLabelCache + ", lengkap dengan alasan yang "
                        + "wajib diisi petugas saat membatalkan. Pembatalan transaksi yang sudah dibayar sebaiknya "
                        + "jarang terjadi — pola yang sering, bernilai besar, atau alasannya seragam/tidak masuk "
                        + "akal pantas ditanyakan. Kolom \"Sudah Diposting\" bertanda Ya berarti transaksi itu "
                        + "sudah masuk jurnal akuntansi saat dibatalkan, sehingga perlu jurnal koreksi tersendiri.",
                "Pembatalan Transaksi", HEAD_HAPUS, KIND_HAPUS, data);
    }

    // ======================== Query ========================

    /**
     * Opname terakhir per toko. Kolom: [0]=nama toko [1]=tanggal opname terakhir (teks, '' bila belum
     * pernah) [2]=selang hari sejak opname terakhir ({@link #OPNAME_BELUM_PERNAH} bila belum pernah,
     * supaya toko yang belum pernah opname selalu berada di urutan teratas) [3]=jumlah sesi selesai
     * dalam periode.
     */
    private List<Object[]> qOpnameTerakhir() {
        return q("opname_terakhir", "SELECT t.nama, "
                + "COALESCE(TO_CHAR(MAX(s.tanggalselesai), 'dd-MM-yyyy'), ''), "
                + "COALESCE(EXTRACT(DAY FROM (NOW() - MAX(s.tanggalselesai))), " + (long) OPNAME_BELUM_PERNAH + "), "
                + "COUNT(s.id) FILTER (WHERE s.tanggalselesai >= (" + periodeSinceCache + ")) "
                + "FROM koperasi.toko t "
                + "LEFT JOIN koperasi.sesi_stok_opname s ON s.toko = t.id AND s.status = 'SELESAI' "
                + "WHERE (t.aktif IS NULL OR t.aktif = true)" + andTokoSelf("t")
                + " GROUP BY t.nama ORDER BY 3 DESC, t.nama ASC");
    }

    /** Ubah hasil {@link #qOpnameTerakhir()} jadi baris tampilan (kolom "Status" dalam bahasa awam). */
    private List<Object[]> barisOpname(List<Object[]> raw) {
        List<Object[]> out = new ArrayList<Object[]>();
        for (Object[] r : raw) {
            double hari = num(r[2]);
            String tglTeks;
            String status;
            if (hari >= OPNAME_BELUM_PERNAH) {
                tglTeks = "Belum pernah";
                status = "Belum pernah opname";
            } else {
                tglTeks = str(r[1]);
                if (hari >= AMBANG_OPNAME_HARI) {
                    status = "Terlambat (" + (long) hari + " hari)";
                } else {
                    status = "Patuh";
                }
            }
            String selang = hari >= OPNAME_BELUM_PERNAH ? "-" : String.valueOf((long) hari);
            out.add(new Object[] { str(r[0]), tglTeks, selang, Long.valueOf((long) num(r[3])), status });
        }
        return out;
    }

    /** Selisih hasil opname per toko, dinilai dengan harga beli produk. */
    private List<Object[]> qSelisihOpname() {
        return q("selisih_opname", "SELECT COALESCE(t.nama,'-'), COUNT(*), "
                + "COUNT(*) FILTER (WHERE COALESCE(so.selisih,0) <> 0), "
                + "COALESCE(SUM(COALESCE(so.selisih,0) * COALESCE(pr.hargabeli,0)),0) "
                + "FROM koperasi.stok_opname so "
                + "LEFT JOIN koperasi.toko t ON t.id = so.toko "
                + "LEFT JOIN koperasi.produk pr ON pr.id = so.produk "
                + "WHERE so.waktuopname >= (" + periodeSinceCache + ")" + andToko("so")
                + " GROUP BY t.nama ORDER BY 4 ASC");
    }

    /**
     * Sesi kas yang masih terbuka SAAT INI (bukan terikat periode). Penanda terbuka adalah
     * {@code status}, bukan {@code waktututup}: {@code SesiKasUtil.sesiTerbuka} memperlakukan
     * {@code status IS NULL} sebagai BUKA (data lama sebelum kolom status dipakai), jadi filter di
     * sini harus ikut memperlakukannya begitu agar sesi lama tidak luput dari pengawasan.
     */
    private List<Object[]> qSesiKasTerbuka() {
        return qShared("kas_terbuka", "SELECT COALESCE(t.nama,'-'), COALESCE(k.kasir_nama,'-'), "
                + "COALESCE(TO_CHAR(k.waktubuka,'dd-MM-yyyy HH24:MI'),'-'), "
                + "COALESCE(ROUND(CAST((EXTRACT(EPOCH FROM (NOW() - k.waktubuka))/3600.0) AS numeric), 1), 0) "
                + "FROM koperasi.sesi_kas_kasir k LEFT JOIN koperasi.toko t ON t.id = k.toko "
                + "WHERE (k.status = 'BUKA' OR k.status IS NULL)" + andToko("k")
                + " ORDER BY 4 DESC LIMIT 200");
    }

    /** Sesi kas yang sudah ditutup tapi uang fisiknya tidak cocok, dalam periode. */
    private List<Object[]> qSelisihKas() {
        return q("selisih_kas", "SELECT COALESCE(t.nama,'-'), COALESCE(k.kasir_nama,'-'), "
                + "COALESCE(TO_CHAR(k.waktututup,'dd-MM-yyyy HH24:MI'),'-'), "
                + "COALESCE(k.modalawal,0), COALESCE(k.uangfisik,0), COALESCE(k.selisih,0) "
                + "FROM koperasi.sesi_kas_kasir k LEFT JOIN koperasi.toko t ON t.id = k.toko "
                + "WHERE k.status = 'TUTUP' AND k.waktututup >= (" + periodeSinceCache + ") "
                + "AND COALESCE(k.selisih,0) <> 0" + andToko("k")
                + " ORDER BY ABS(COALESCE(k.selisih,0)) DESC LIMIT 200");
    }

    /**
     * Diskon yang diberikan TANPA aturan diskon, dikelompokkan per (toko, kasir). Kuncinya
     * {@code p.aturan_diskon IS NULL} -- diskon yang dihitung otomatis dari sebuah AturanDiskon
     * SELALU menyimpan FK-nya (lihat {@code PembelianAnggotaKoperasi.simpanRinci}), jadi baris
     * berdiskon tanpa FK berarti potongan itu diketik manual saat transaksi.
     */
    private List<Object[]> qDiskonManual() {
        return q("diskon_manual", "SELECT COALESCE(t.nama,'-'), COALESCE(p.oleh,'-'), "
                + "COUNT(DISTINCT COALESCE(p.pembelian_anggota_koperasi, p.id)), COALESCE(SUM(p.diskon),0) "
                + "FROM koperasi.pembelian p LEFT JOIN koperasi.toko t ON t.id = p.toko "
                + "WHERE p.aktif = true AND p.waktu >= (" + periodeSinceCache + ") "
                + "AND COALESCE(p.diskon,0) > 0 AND p.aturan_diskon IS NULL" + andToko("p")
                + " GROUP BY t.nama, p.oleh ORDER BY 4 DESC LIMIT 200");
    }

    /**
     * Pembatalan transaksi dari arsip {@code koperasi.pembatalan_transaksi}. Nama pembeli/kasir
     * dibaca dari kolom potret teks pada arsip (bukan lewat JOIN ke tabel anggota/pengguna) supaya
     * arsip tetap terbaca utuh walaupun anggota atau penggunanya kelak dihapus/diganti nama.
     * Pemanggil WAJIB memastikan tabelnya ada dulu.
     */
    private List<Object[]> qPembatalanTransaksi() {
        return q("pembatalan_trx", "SELECT COALESCE(TO_CHAR(b.tanggal_dibatalkan,'dd-MM-yyyy HH24:MI'),'-'), "
                + "COALESCE(b.dibatalkan_oleh,'-'), COALESCE(t.nama,'-'), COALESCE(b.nama_anggota,'-'), "
                + "COALESCE(b.total_biaya,0), COALESCE(b.alasan,'-'), "
                + "CASE WHEN b.sudah_diposting THEN 'Ya' ELSE 'Tidak' END "
                + "FROM koperasi.pembatalan_transaksi b "
                + "LEFT JOIN koperasi.toko t ON t.id = b.toko "
                + "WHERE b.tanggal_dibatalkan >= (" + periodeSinceCache + ")" + andToko("b")
                + " ORDER BY b.tanggal_dibatalkan DESC LIMIT 200");
    }

    /**
     * Cek keberadaan tabel lewat {@code information_schema.tables}. SENGAJA tidak memakai
     * {@code to_regclass()} -- fungsi itu baru ada di PostgreSQL 9.4 sedangkan server produksi 9.3,
     * dan bila dipanggil query-nya GAGAL serta meracuni transaksi sehingga semua query berikutnya
     * ikut error (pola bug yang sudah pernah terjadi, lihat catatan sama di {@code LaporanKantinUtil}).
     */
    private boolean tabelAda(org.hibernate.Session session, String namaTabel) {
        try {
            String schema = "public", tbl = namaTabel;
            int dot = namaTabel.indexOf('.');
            if (dot > 0) {
                schema = namaTabel.substring(0, dot);
                tbl = namaTabel.substring(dot + 1);
            }
            org.hibernate.SQLQuery qq = session.createSQLQuery(
                    "select 1 from information_schema.tables where table_schema = :sch and table_name = :tbl");
            qq.setParameter("sch", schema.trim().toLowerCase());
            qq.setParameter("tbl", tbl.trim().toLowerCase());
            qq.setMaxResults(1);
            return qq.uniqueResult() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== Tabel + Excel ========================

    private void appendRekapTable(Component host, String judul, String desc, final String sheetName,
            final String[] headers, final int[] kinds, final List<Object[]> data) {
        host.appendChild(DashboardUiKit.html(
                "<div style='font-size:14px;font-weight:800;color:#0f172a;margin:18px 0 4px;border-left:4px solid "
                        + DashboardUiKit.PRIMARY + ";padding-left:10px;'>" + DashboardUiKit.esc(judul) + "</div>"));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(desc)));

        if (data == null || data.isEmpty()) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:" + DashboardUiKit.GOOD + ";font-weight:700;padding:10px 4px;'>"
                            + "Tidak ada temuan pada lingkup &amp; periode ini.</div>"));
            return;
        }

        // Wadah per-tabel supaya "Cetak PDF" mencetak HANYA tabel ini (lihat pola sama di
        // DashboardKantinAction.appendRekapTable).
        Div wadah = new Div();
        wadah.setParent(host);

        Div bar = new Div();
        bar.setStyle("text-align:right;margin:2px 0 6px;");
        ais.ui.util.DashboardGridExportHelper.pasangTombolPdf(bar, wadah, judul);
        MyToolbarbuttonConfig dl = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.png");
        dl.setTooltiptext("Unduh tabel ini sebagai berkas Excel");
        dl.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                try {
                    XSSFWorkbook wb = new XSSFWorkbook();
                    writeSheet(wb, sheetName, sheetName + " - " + periodeLabelCache, headers, kinds, data);
                    Filedownload.save(saveWorkbook(wb, "rekap_kepatuhan_" + sanitize(sheetName)), XLSX_MIME);
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
                int kind = kinds[i];
                if (kind == 2) {
                    lbl = new Label("Rp " + DashboardUiKit.money(num(v)));
                    lbl.setStyle("font-weight:700;");
                } else if (kind == 1) {
                    lbl = new Label(DashboardUiKit.money(num(v)));
                } else if (kind == 5) {
                    // Selisih bernilai uang: nol = aman (hijau), minus = kurang (merah), plus = lebih (oranye).
                    double s = num(v);
                    lbl = new Label("Rp " + DashboardUiKit.money(s));
                    String warna = s == 0 ? DashboardUiKit.GOOD : (s < 0 ? DashboardUiKit.BAD : DashboardUiKit.WARN);
                    lbl.setStyle("font-weight:800;color:" + warna + ";");
                } else if (kind == 6) {
                    // Lama sesi kas terbuka (jam): makin lama makin merah.
                    double j = num(v);
                    lbl = new Label(DashboardUiKit.money(j));
                    String warna = j >= AMBANG_SESI_KAS_JAM ? DashboardUiKit.BAD
                            : (j >= (AMBANG_SESI_KAS_JAM / 2) ? DashboardUiKit.WARN : DashboardUiKit.INK);
                    lbl.setStyle("font-weight:800;color:" + warna + ";");
                } else {
                    String teks = str(v);
                    lbl = new Label(teks);
                    if (i == 0) {
                        lbl.setStyle("font-weight:700;");
                    } else if (teks.startsWith("Terlambat") || teks.startsWith("Belum pernah")) {
                        lbl.setStyle("font-weight:800;color:" + DashboardUiKit.BAD + ";");
                    } else if ("Patuh".equals(teks)) {
                        lbl.setStyle("font-weight:800;color:" + DashboardUiKit.GOOD + ";");
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

    // ======================== Cache & SQL ========================

    private interface HtmlBuilder {
        String build();
    }

    private String htmlCached(String key, final HtmlBuilder builder) {
        try {
            return DashboardCache.l1("kepatuhan.html." + key + "." + periodeKeyCache + "." + scopeKey(),
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

    /** Hasil query per-panel, di-cache L3 per periode + lingkup toko. */
    private List<Object[]> q(String panelKey, final String sql) {
        try {
            return DashboardCache.l3("kepatuhan." + panelKey + "." + periodeKeyCache + "." + scopeKey(),
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

    /** Seperti {@link #q} tapi tanpa pembeda periode (data kondisi terkini, mis. sesi kas terbuka). */
    private List<Object[]> qShared(String panelKey, final String sql) {
        try {
            return DashboardCache.l3("kepatuhan." + panelKey + "." + scopeKey(),
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

    /**
     * Baca hasil native SQL lewat JDBC {@code getObject()} pada {@code openSession()} terisolasi --
     * pola SAMA dengan {@code DashboardKantinAction.rows()}, dipilih sengaja karena dua alasan:
     * (1) {@code getObject()} mengembalikan tipe alami kolom sehingga kebal terhadap salah-tebak tipe
     * oleh autodiscovery Hibernate ({@code PSQLException: Bad value for type double}, bug produksi
     * yang pernah terjadi pada query yang mencampur kolom teks &amp; numerik seperti di kelas ini);
     * (2) sesi terisolasi berarti satu query yang gagal (mis. tabel audit tak ada) TIDAK meracuni
     * transaksi thread-bound halaman ini.
     */
    private List<Object[]> rows(String sql) {
        org.hibernate.Session session = null;
        java.sql.Statement st = null;
        java.sql.ResultSet rs = null;
        List<Object[]> out = new ArrayList<Object[]>();
        try {
            session = HibernateUtil.openSession();
            st = session.connection().createStatement();
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
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception ig) {
                ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) DashboardKepatuhanKantinAction.rows.rs");
            }
            try {
                if (st != null) {
                    st.close();
                }
            } catch (Exception ig) {
                ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) DashboardKepatuhanKantinAction.rows.st");
            }
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static double num(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(o).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}

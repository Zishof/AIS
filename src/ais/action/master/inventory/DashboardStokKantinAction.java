package ais.action.master.inventory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
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
 * Dasbor Stok &amp; Mutasi Kantin (ZK) — konversi dari {@code modul/kantin/stok/*}.
 *
 * <p>Menampilkan sisa stok tiap produk, barang masuk (pengadaan), dan barang keluar (penjualan)
 * dalam satu layar: kartu ringkasan + grafik HTML/CSS + tabel rekap dengan tombol Unduh Excel.
 * Memakai pola yang sama dengan Dasbor Kantin (cache L1/L2/L3, toko-scope).</p>
 *
 * <p>Sumber data: {@code koperasi.produk} (stok), {@code koperasi.pengadaan_produk} (barang masuk),
 * {@code koperasi.pembelian} (barang keluar).</p>
 */
public class DashboardStokKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Combobox searchPeriode;
    private Div dashHost;

    private String periodeSinceCache = "CURRENT_DATE - INTERVAL '29 days'";
    private String periodeKeyCache = "30_Hari_Terakhir";
    private String periodeLabelCache = "30 Hari Terakhir";

    private Long scopeTokoId = null;
    private String scopeTokoNama = null;

    /**
     * Diisi dari parameter URL {@code perToko} saat dasbor ini dimuat sbg tab "Stok &amp; Mutasi" dari
     * Dasbor Kantin (lewat {@code MyInclude}) -- mewarisi centang "Per Toko" admin di sana. Tak ada
     * checkbox sendiri di layar ini (dasbor ini bisa dibuka langsung lewat menunya sendiri juga,
     * lihat javadoc {@link #resolveScopeToko()}).
     */
    private boolean perTokoAktif = false;

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

        String judul = scopeTokoId == null ? "Dasbor Stok & Mutasi" : "Dasbor Stok & Mutasi — " + scopeTokoNama;
        String desc = "Pantau sisa stok tiap produk, barang yang masuk (pembelian/pengadaan), dan barang yang "
                + "keluar (terjual). Stok yang menipis ditandai warna agar mudah ditindaklanjuti.";
        if (scopeTokoId != null) {
            desc = "Khusus data toko Anda (" + scopeTokoNama + "). " + desc;
        }
        DashboardUiKit.attachIntro(comp, judul, desc);

        isiPeriode();
        buildDashboard();
    }

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
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/DashboardStokKantinAction.java:101");
        }
        // 2026-07-26: saat dasbor ini dimuat sbg tab "Stok & Mutasi" dari Dasbor Kantin (lewat
        // MyInclude), warisi pilihan "Cari Toko"/"Per Toko" admin di sana lewat parameter URL
        // "tokoId"/"perToko" -- HANYA kalau pengguna belum terkunci sendiri ke satu toko di atas
        // (scopeTokoId masih null), supaya parameter dari URL tak pernah bisa membajak kunci
        // keamanan pedagang.
        if (scopeTokoId == null) {
            try {
                org.zkoss.zk.ui.Execution exec = org.zkoss.zk.ui.Executions.getCurrent();
                String tp = exec.getParameter("tokoId");
                if (tp != null && tp.trim().length() > 0) {
                    Long tid = Long.valueOf(tp.trim());
                    Toko t = (Toko) HibernateUtil.currentSession().get(Toko.class, tid);
                    if (t != null) {
                        scopeTokoId = t.getId();
                        scopeTokoNama = t.getNama();
                    }
                }
                perTokoAktif = "true".equalsIgnoreCase(exec.getParameter("perToko"));
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/DashboardStokKantinAction.java:tokoIdParam");
            }
        }
    }

    private String andToko(String alias) {
        return scopeTokoId == null ? "" : (" AND " + alias + ".toko = " + scopeTokoId);
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
            writeSheet(wb, "Stok", "Stok Saat Ini",
                    new String[] { "Toko", "Produk", "Sisa Stok", "Harga Modal", "Harga Jual" },
                    new int[] { 0, 0, 1, 2, 2 }, qStok());
            writeSheet(wb, "Barang Masuk", "Barang Masuk - " + periodeLabelCache,
                    new String[] { "Toko", "No. Faktur", "Waktu", "Produk", "Supplier", "Qty", "Total" },
                    new int[] { 0, 0, 0, 0, 0, 1, 2 }, qMasuk());
            writeSheet(wb, "Barang Keluar", "Barang Keluar - " + periodeLabelCache,
                    new String[] { "Toko", "No. Transaksi", "Waktu", "Produk", "Qty", "Subtotal" },
                    new int[] { 0, 0, 0, 0, 1, 2 }, qKeluar());
            Filedownload.save(saveWorkbook(wb, "rekap_stok_kantin"), XLSX_MIME);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Gagal menyiapkan berkas Excel.", "Informasi",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        }
    }

    // ======================== Build ========================

    private void buildDashboard() {
        if (dashHost == null) {
            return;
        }
        dashHost.getChildren().clear();
        periodeSinceCache = periodeSince();
        periodeLabelCache = periodeLabel();
        periodeKeyCache = periodeLabelCache.replaceAll("[^a-zA-Z0-9]", "_");

        final String since = periodeSinceCache;
        String html = htmlCached("stok", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();

                Object[] s = first(q("ringkasanstok", "SELECT COUNT(*), COALESCE(SUM(COALESCE(c.stok,0)),0), "
                        + "COALESCE(SUM(COALESCE(c.stok,0)*COALESCE(c.hargabeli,0)),0) FROM koperasi.produk c "
                        + "WHERE (c.aktif = true OR c.aktif IS NULL)" + andToko("c")));
                long jenis = s == null ? 0 : (long) num(s[0]);
                double unit = s == null ? 0 : num(s[1]);
                double nilai = s == null ? 0 : num(s[2]);

                Object[] m = first(q("ringkasanmasuk", "SELECT COALESCE(SUM(a.qty),0), COALESCE(SUM(a.totalharga),0) "
                        + "FROM koperasi.pengadaan_produk a WHERE a.waktupengadaan >= (" + since + ")" + andToko("a")));
                double masukUnit = m == null ? 0 : num(m[0]);
                double masukNilai = m == null ? 0 : num(m[1]);

                Object[] k = first(q("ringkasankeluar", "SELECT COALESCE(SUM(a.qty),0), "
                        + "COALESCE(SUM(a.qty*COALESCE(a.hargajual,0)),0) FROM koperasi.pembelian a "
                        + "WHERE a.aktif = true AND a.waktu >= (" + since + ")" + andToko("a")));
                double keluarUnit = k == null ? 0 : num(k[0]);
                double keluarNilai = k == null ? 0 : num(k[1]);

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Jenis Produk", DashboardUiKit.money(jenis),
                        "produk terdaftar", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Total Unit Stok", DashboardUiKit.money(unit),
                        "unit tersedia", DashboardUiKit.ACCENT));
                kartu.add(new DashboardUiKit.Stat("Nilai Stok (Modal)", "Rp " + DashboardUiKit.money(nilai),
                        "nilai barang di gudang", DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Barang Masuk", DashboardUiKit.money(masukUnit),
                        "Rp " + DashboardUiKit.money(masukNilai) + " · " + periodeLabelCache, DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Barang Keluar", DashboardUiKit.money(keluarUnit),
                        "Rp " + DashboardUiKit.money(keluarNilai) + " · " + periodeLabelCache, DashboardUiKit.WARN));
                sb.append(DashboardUiKit.descChip("Ringkasan stok dan pergerakan barang pada " + periodeLabelCache + "."));
                sb.append(DashboardUiKit.cards(kartu));

                sb.append(DashboardUiKit.openGrid(320));

                LinkedHashMap<String, Double> menipis = new LinkedHashMap<String, Double>();
                for (Object[] r : q("menipis", "SELECT c.nama, COALESCE(c.stok,0) st FROM koperasi.produk c "
                        + "WHERE (c.aktif = true OR c.aktif IS NULL)" + andToko("c")
                        + " ORDER BY st ASC LIMIT 10")) {
                    menipis.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.barList("Stok Paling Menipis",
                        "Produk dengan sisa stok paling sedikit — perlu segera diisi ulang.", menipis,
                        DashboardUiKit.BAD, "unit", false, "Belum ada data produk."));

                LinkedHashMap<String, Double> topMasuk = new LinkedHashMap<String, Double>();
                for (Object[] r : q("topmasuk", "SELECT COALESCE(p.nama,'-'), COALESCE(SUM(a.qty),0) q "
                        + "FROM koperasi.pengadaan_produk a LEFT JOIN koperasi.produk p ON p.id = a.produk "
                        + "WHERE a.waktupengadaan >= (" + since + ")" + andToko("a")
                        + " GROUP BY p.nama ORDER BY q DESC LIMIT 10")) {
                    topMasuk.put(str(r[0]), num(r[1]));
                }
                sb.append(DashboardUiKit.barList("Barang Terbanyak Masuk",
                        "Produk yang paling banyak ditambahkan stoknya pada periode ini.", topMasuk,
                        DashboardUiKit.GOOD, "unit", false, "Belum ada barang masuk."));

                sb.append(DashboardUiKit.closeGrid());
                return sb.toString();
            }
        });
        dashHost.appendChild(DashboardUiKit.html(html));

        // Bahan baku & estimasi habis — terisi otomatis dari pemakaian saat produk ber-resep terjual.
        buildBahanBakuStok(dashHost, since);

        // Rekonsiliasi stok produk kantin vs stok persediaan modul Aset (untuk produk yang ditautkan).
        buildRekonsiliasiAset(dashHost);

        appendRekapTable(dashHost, "Stok Saat Ini",
                "Sisa stok tiap produk. Merah = sangat sedikit, oranye = perlu diisi.", "Stok",
                new String[] { "Toko", "Produk", "Sisa Stok", "Harga Modal", "Harga Jual" },
                new int[] { 0, 0, 3, 2, 2 }, qStok());

        appendRekapTable(dashHost, "Barang Masuk (Pengadaan)",
                "Daftar barang yang masuk dari pemasok beserta jumlah dan nilainya pada periode ini.", "Barang Masuk",
                new String[] { "Toko", "No. Faktur", "Waktu", "Produk", "Supplier", "Qty", "Total" },
                new int[] { 0, 0, 0, 0, 0, 1, 2 }, qMasuk());

        appendRekapTable(dashHost, "Barang Keluar (Penjualan)",
                "Daftar barang yang keluar karena terjual pada periode ini.", "Barang Keluar",
                new String[] { "Toko", "No. Transaksi", "Waktu", "Produk", "Qty", "Subtotal" },
                new int[] { 0, 0, 0, 0, 1, 2 }, qKeluar());

        // "Per Toko" (2026-07-26) -- ringkasan jenis produk/nilai stok, barang masuk, dan barang
        // keluar dipecah per toko. Diwariskan lewat parameter URL "perToko" dari Dasbor Kantin (lihat
        // javadoc {@link #resolveScopeToko()}); hanya muncul saat TIDAK sedang disaring ke satu toko.
        if (perTokoAktif && scopeTokoId == null) {
            // addScalar eksplisit (TIPE_SDD) -- WAJIB di sini krn ketiga query ini mencampur
            // COALESCE(teks,'-') dgn COALESCE(angka,0) dlm satu SELECT, persis pola yg pernah memicu
            // "PSQLException: Bad value for type double" (error produksi ID 150, lihat qKeluar()/qMasuk()
            // di atas) -- pgjdbc bisa salah menebak tipe kolom teks kalau dibiarkan auto-detect.
            List<Object[]> stokPerToko = qShared("stok_pertoko", "SELECT COALESCE(t.nama,'-') AS c0, COUNT(*) AS c1, "
                    + "COALESCE(SUM(COALESCE(c.stok,0)*COALESCE(c.hargabeli,0)),0) AS c2 FROM koperasi.produk c "
                    + "LEFT JOIN koperasi.toko t ON t.id = c.toko WHERE (c.aktif = true OR c.aktif IS NULL) "
                    + "GROUP BY t.nama", TIPE_SDD);
            List<Object[]> masukPerToko = q("masuk_pertoko", "SELECT COALESCE(t.nama,'-') AS c0, "
                    + "COALESCE(SUM(a.qty),0) AS c1, COALESCE(SUM(a.totalharga),0) AS c2 "
                    + "FROM koperasi.pengadaan_produk a LEFT JOIN koperasi.toko t ON t.id = a.toko "
                    + "WHERE a.waktupengadaan >= (" + since + ") GROUP BY t.nama", TIPE_SDD);
            List<Object[]> keluarPerToko = q("keluar_pertoko", "SELECT COALESCE(t.nama,'-') AS c0, "
                    + "COALESCE(SUM(a.qty),0) AS c1, COALESCE(SUM(a.qty*COALESCE(a.hargajual,0)),0) AS c2 "
                    + "FROM koperasi.pembelian a LEFT JOIN koperasi.toko t ON t.id = a.toko "
                    + "WHERE a.aktif = true AND a.waktu >= (" + since + ") GROUP BY t.nama", TIPE_SDD);

            LinkedHashMap<String, Object[]> gabung = new LinkedHashMap<String, Object[]>();
            for (Object[] r : stokPerToko) {
                gabung.put(str(r[0]), new Object[] { str(r[0]), num(r[1]), num(r[2]), 0.0, 0.0, 0.0, 0.0 });
            }
            for (Object[] r : masukPerToko) {
                Object[] acc = gabung.get(str(r[0]));
                if (acc == null) {
                    acc = new Object[] { str(r[0]), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
                    gabung.put(str(r[0]), acc);
                }
                acc[3] = num(r[1]);
                acc[4] = num(r[2]);
            }
            for (Object[] r : keluarPerToko) {
                Object[] acc = gabung.get(str(r[0]));
                if (acc == null) {
                    acc = new Object[] { str(r[0]), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
                    gabung.put(str(r[0]), acc);
                }
                acc[5] = num(r[1]);
                acc[6] = num(r[2]);
            }
            List<Object[]> perToko = new ArrayList<Object[]>(gabung.values());
            java.util.Collections.sort(perToko, new java.util.Comparator<Object[]>() {
                @Override
                public int compare(Object[] a, Object[] b) {
                    return Double.compare(num(b[2]), num(a[2]));
                }
            });
            appendRekapTable(dashHost, "Ringkasan Stok & Mutasi per Toko",
                    "Jenis produk, nilai stok, serta barang masuk/keluar pada " + periodeLabelCache
                            + ", dipecah per toko.",
                    "Ringkasan Stok Per Toko",
                    new String[] { "Toko", "Jenis Produk", "Nilai Stok", "Qty Masuk", "Nilai Masuk", "Qty Keluar",
                            "Nilai Keluar" },
                    new int[] { 0, 1, 2, 1, 2, 1, 2 }, perToko);
        }
    }

    // ======================== Query ========================

    /** Tipe kolom {@code [teks, angka]} -- lihat javadoc {@link #rows(String, org.hibernate.type.Type[])}. */
    private static final org.hibernate.type.Type[] TIPE_SDDDD = { org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.DOUBLE, org.hibernate.Hibernate.DOUBLE,
            org.hibernate.Hibernate.DOUBLE };
    private static final org.hibernate.type.Type[] TIPE_SSSSDD = { org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.DOUBLE, org.hibernate.Hibernate.DOUBLE };
    private static final org.hibernate.type.Type[] TIPE_SSSDD = { org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.DOUBLE,
            org.hibernate.Hibernate.DOUBLE };
    /** Tipe kolom {@code [teks, angka, angka]} -- dipakai 3 query "per toko" (2026-07-26). */
    private static final org.hibernate.type.Type[] TIPE_SDD = { org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.DOUBLE, org.hibernate.Hibernate.DOUBLE };

    private List<Object[]> qStok() {
        return qShared("stoklist", "SELECT COALESCE(t.nama,'-') AS c0, c.nama AS c1, COALESCE(c.stok,0) AS c2, "
                + "COALESCE(c.hargabeli,0) AS c3, COALESCE(c.hargajual,0) AS c4 FROM koperasi.produk c "
                + "LEFT JOIN koperasi.toko t ON t.id = c.toko WHERE (c.aktif = true OR c.aktif IS NULL)" + andToko("c")
                + " ORDER BY COALESCE(c.stok,0) ASC LIMIT 300", TIPE_SDDDD);
    }

    /** Tipe kolom {@code [teks x5, angka x2]} -- {@link #qMasuk()} dgn kolom Toko baru (2026-07-26). */
    private static final org.hibernate.type.Type[] TIPE_SSSSSDD = { org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.DOUBLE, org.hibernate.Hibernate.DOUBLE };
    /** Tipe kolom {@code [teks x4, angka x2]} -- {@link #qKeluar()} dgn kolom Toko baru (2026-07-26). */
    private static final org.hibernate.type.Type[] TIPE_SSSSDD2 = { org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING, org.hibernate.Hibernate.STRING,
            org.hibernate.Hibernate.DOUBLE, org.hibernate.Hibernate.DOUBLE };

    private List<Object[]> qMasuk() {
        return q("masuklist", "SELECT COALESCE(t.nama,'-') AS c0, COALESCE(a.nomorfaktur,'') AS c1, "
                + "TO_CHAR(a.waktupengadaan,'dd-MM-yyyy HH24:MI') AS c2, COALESCE(p.nama,'') AS c3, "
                + "COALESCE(a.namasupplier,'') AS c4, COALESCE(a.qty,0) AS c5, COALESCE(a.totalharga,0) AS c6 "
                + "FROM koperasi.pengadaan_produk a LEFT JOIN koperasi.produk p ON p.id = a.produk "
                + "LEFT JOIN koperasi.toko t ON t.id = a.toko "
                + "WHERE a.waktupengadaan >= (" + periodeSinceCache + ")" + andToko("a")
                + " ORDER BY a.waktupengadaan DESC LIMIT 300", TIPE_SSSSSDD);
    }

    private List<Object[]> qKeluar() {
        return q("keluarlist", "SELECT COALESCE(t.nama,'-') AS c0, COALESCE(a.kode,'') AS c1, "
                + "TO_CHAR(a.waktu,'dd-MM-yyyy HH24:MI') AS c2, COALESCE(p.nama,'') AS c3, COALESCE(a.qty,0) AS c4, "
                + "COALESCE(a.qty*COALESCE(a.hargajual,0),0) AS c5 "
                + "FROM koperasi.pembelian a LEFT JOIN koperasi.produk p ON p.id = a.produk "
                + "LEFT JOIN koperasi.toko t ON t.id = a.toko "
                + "WHERE a.aktif = true AND a.waktu >= (" + periodeSinceCache + ")" + andToko("a")
                + " ORDER BY a.waktu DESC LIMIT 300", TIPE_SSSSDD2);
    }

    // ======================== Bahan Baku & Estimasi Habis ========================

    /**
     * Memprediksi kapan tiap bahan baku habis dari laju pemakaian (buku besar
     * {@code koperasi.pemakaian_bahan_baku}). Estimasi hari = sisa stok / pemakaian rata-rata per hari.
     * Reuse penuh DashboardUiKit + appendRekapTable; gagal-aman (tabel belum ada → bagian kosong).
     */
    private void buildBahanBakuStok(Component host, final String since) {
        final List<Object[]> bb = q("bb_forecast", "SELECT COALESCE(pr.nama,'-'), COALESCE(pr.stok,0), "
                + "COALESCE(SUM(pb.qty),0), COALESCE(pr.hargabeli,0), "
                + "COALESCE(SUM(pb.qty),0) / GREATEST(1, (CURRENT_DATE - DATE(" + since + ")) + 1) "
                + "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
                + "WHERE pb.waktu >= (" + since + ")" + andToko("pb")
                + " GROUP BY pr.id, pr.nama, pr.stok, pr.hargabeli ORDER BY 3 DESC");

        String html = htmlCached("bahanbaku", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();
                sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;margin:18px 0 4px;border-left:4px solid "
                        + DashboardUiKit.PRIMARY + ";padding-left:10px;'>Bahan Baku &amp; Estimasi Habis</div>");
                if (bb.isEmpty()) {
                    sb.append(DashboardUiKit.descChip("Belum ada pemakaian bahan baku pada " + periodeLabelCache
                            + ". Bagian ini terisi otomatis setelah produk ber-resep terjual."));
                    return sb.toString();
                }

                int mendesak = 0;
                double nilai = 0;
                String urgenNama = "-";
                double urgenHari = Double.MAX_VALUE;
                java.util.List<Object[]> lajuSort = new java.util.ArrayList<Object[]>();
                java.util.List<Object[]> hariSort = new java.util.ArrayList<Object[]>();
                for (Object[] r : bb) {
                    double sisa = num(r[1]);
                    double hb = num(r[3]);
                    double perHari = num(r[4]);
                    double hari = perHari > 0 ? sisa / perHari : Double.MAX_VALUE;
                    nilai += sisa * hb;
                    if (hari < 7) {
                        mendesak++;
                    }
                    if (hari < urgenHari) {
                        urgenHari = hari;
                        urgenNama = str(r[0]);
                    }
                    lajuSort.add(new Object[] { str(r[0]), Double.valueOf(perHari) });
                    hariSort.add(new Object[] { str(r[0]), Double.valueOf(hari) });
                }

                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Bahan Baku Dipantau", DashboardUiKit.money(bb.size()),
                        "punya catatan pemakaian", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Segera Habis (< 7 hari)", DashboardUiKit.money(mendesak),
                        "perlu segera dibeli", DashboardUiKit.BAD));
                kartu.add(new DashboardUiKit.Stat("Paling Mendesak",
                        urgenHari == Double.MAX_VALUE ? "Aman" : (Math.round(urgenHari) + " hari"),
                        DashboardUiKit.shorten(urgenNama, 18), DashboardUiKit.WARN));
                kartu.add(new DashboardUiKit.Stat("Nilai Stok Bahan Baku", "Rp " + DashboardUiKit.money(nilai),
                        "modal bahan tersisa", DashboardUiKit.GOOD));
                sb.append(DashboardUiKit.descChip("Perkiraan kapan tiap bahan baku habis, supaya bisa belanja sebelum kehabisan."));
                sb.append(DashboardUiKit.cards(kartu));

                sb.append(DashboardUiKit.openGrid(320));

                java.util.Collections.sort(lajuSort, new java.util.Comparator<Object[]>() {
                    @Override
                    public int compare(Object[] a, Object[] b) {
                        return Double.compare(num(b[1]), num(a[1]));
                    }
                });
                LinkedHashMap<String, Double> laju = new LinkedHashMap<String, Double>();
                for (int i = 0; i < Math.min(lajuSort.size(), 10); i++) {
                    laju.put(DashboardUiKit.shorten(str(lajuSort.get(i)[0]), 18), Double.valueOf(num(lajuSort.get(i)[1])));
                }
                sb.append(DashboardUiKit.barList("Laju Pemakaian Bahan Baku (per hari)",
                        "Bahan yang paling cepat berkurang tiap harinya.", laju, DashboardUiKit.ACCENT, "unit/hari",
                        false, "Belum ada pemakaian."));

                java.util.Collections.sort(hariSort, new java.util.Comparator<Object[]>() {
                    @Override
                    public int compare(Object[] a, Object[] b) {
                        return Double.compare(num(a[1]), num(b[1]));
                    }
                });
                LinkedHashMap<String, Double> sisaHari = new LinkedHashMap<String, Double>();
                for (int i = 0; i < Math.min(hariSort.size(), 10); i++) {
                    double h = num(hariSort.get(i)[1]);
                    if (h == Double.MAX_VALUE) {
                        continue;
                    }
                    sisaHari.put(DashboardUiKit.shorten(str(hariSort.get(i)[0]), 18), Double.valueOf(Math.round(h)));
                }
                sb.append(DashboardUiKit.barList("Perkiraan Hari Stok Tersisa",
                        "Berapa hari lagi tiap bahan baku bertahan sebelum habis (makin kecil makin mendesak).",
                        sisaHari, DashboardUiKit.WARN, "hari", false, "Belum ada pemakaian."));

                sb.append(DashboardUiKit.closeGrid());
                return sb.toString();
            }
        });
        host.appendChild(DashboardUiKit.html(html));

        if (bb.isEmpty()) {
            return;
        }

        java.util.List<Object[]> grid = new java.util.ArrayList<Object[]>();
        for (Object[] r : bb) {
            double sisa = num(r[1]);
            double terp = num(r[2]);
            double perHari = num(r[4]);
            long hari = perHari > 0 ? Math.round(sisa / perHari) : 999999L;
            grid.add(new Object[] { str(r[0]), Double.valueOf(sisa), Double.valueOf(terp), Long.valueOf(hari) });
        }
        java.util.Collections.sort(grid, new java.util.Comparator<Object[]>() {
            @Override
            public int compare(Object[] a, Object[] b) {
                return Long.compare(((Number) a[3]).longValue(), ((Number) b[3]).longValue());
            }
        });
        appendRekapTable(host, "Estimasi Bahan Baku Habis",
                "Perkiraan berapa hari lagi tiap bahan baku habis dari laju pemakaian. Merah = segera habis.",
                "Estimasi Bahan Baku",
                new String[] { "Bahan Baku", "Sisa Stok", "Terpakai (" + periodeLabelCache + ")",
                        "Estimasi Habis (hari)" },
                new int[] { 0, 3, 1, 3 }, grid);
    }

    // ======================== Rekonsiliasi Stok Aset ↔ Kantin ========================

    /**
     * Membandingkan stok produk kantin dengan stok persediaan modul Aset untuk produk yang sudah
     * ditautkan ({@code Produk.masterAsset}). Stok aset = Σ((qty+qtybonus) × tanda jenis transaksi)
     * dari {@code asset.detail_transaksi_asset}. Read-only (Fase 0) — hanya menampilkan selisih.
     */
    private void buildRekonsiliasiAset(Component host) {
        final List<Object[]> data = qShared("rekonaset", "SELECT COALESCE(t.nama,'-'), p.nama, "
                + "COALESCE(ma.kode,'') || ' - ' || COALESCE(ma.nama,''), COALESCE(p.stok,0), "
                + "COALESCE((SELECT SUM((a.qty+a.qtybonus)*b.jenis) FROM asset.detail_transaksi_asset a "
                + "INNER JOIN library.kode_transaksi b ON a.kode_transaksi = b.id WHERE a.master_asset = p.master_asset),0) "
                + "FROM koperasi.produk p INNER JOIN asset.master_asset ma ON ma.id = p.master_asset "
                + "LEFT JOIN koperasi.toko t ON t.id = p.toko WHERE p.master_asset IS NOT NULL" + andToko("p")
                + " ORDER BY p.nama");

        String html = htmlCached("rekonaset_h", new HtmlBuilder() {
            @Override
            public String build() {
                StringBuilder sb = new StringBuilder();
                sb.append("<div style='font-size:14px;font-weight:800;color:#0f172a;margin:18px 0 4px;border-left:4px solid "
                        + DashboardUiKit.PRIMARY + ";padding-left:10px;'>Rekonsiliasi Stok Persediaan (Aset &harr; Kantin)</div>");
                if (data.isEmpty()) {
                    sb.append(DashboardUiKit.descChip("Belum ada produk kantin yang ditautkan ke barang persediaan (Aset). "
                            + "Buka Pengaturan Barang, edit produk, lalu isi \"Barang Persediaan (Aset)\"."));
                    return sb.toString();
                }
                int cocok = 0;
                int beda = 0;
                for (Object[] r : data) {
                    if (Math.abs(num(r[3]) - num(r[4])) < 0.001) {
                        cocok++;
                    } else {
                        beda++;
                    }
                }
                List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
                kartu.add(new DashboardUiKit.Stat("Produk Tertaut", DashboardUiKit.money(data.size()),
                        "terhubung ke Aset", DashboardUiKit.PRIMARY));
                kartu.add(new DashboardUiKit.Stat("Stok Cocok", DashboardUiKit.money(cocok),
                        "kantin = aset", DashboardUiKit.GOOD));
                kartu.add(new DashboardUiKit.Stat("Perlu Dicek", DashboardUiKit.money(beda),
                        "ada selisih stok", DashboardUiKit.BAD));
                sb.append(DashboardUiKit.descChip("Membandingkan stok versi kantin dengan stok persediaan di modul Aset, untuk menemukan selisih."));
                sb.append(DashboardUiKit.cards(kartu));
                return sb.toString();
            }
        });
        host.appendChild(DashboardUiKit.html(html));

        if (data.isEmpty()) {
            return;
        }

        List<Object[]> grid = new ArrayList<Object[]>();
        for (Object[] r : data) {
            double sk = num(r[3]);
            double sa = num(r[4]);
            grid.add(new Object[] { str(r[0]), str(r[1]), str(r[2]), Double.valueOf(sk), Double.valueOf(sa),
                    Double.valueOf(sk - sa) });
        }
        appendRekapTable(host, "Rekonsiliasi Stok Persediaan (Aset vs Kantin)",
                "Bandingkan stok kantin dengan stok persediaan aset. Selisih bukan nol berarti perlu dicek.",
                "Rekonsiliasi Aset",
                new String[] { "Tenant", "Produk", "Barang Persediaan (Aset)", "Stok Kantin", "Stok Aset", "Selisih" },
                new int[] { 0, 0, 0, 1, 1, 1 }, grid);
    }

    // ======================== Tabel + Excel (reusable lokal) ========================

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
                    Filedownload.save(saveWorkbook(wb, "rekap_stok_" + sanitize(sheetName)), XLSX_MIME);
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
                    lbl.setStyle("font-weight:700;color:" + DashboardUiKit.GOOD + ";");
                } else if (kind == 3) {
                    double st = num(v);
                    lbl = new Label(DashboardUiKit.money(st));
                    String warna = st <= 5 ? DashboardUiKit.BAD : (st <= 10 ? DashboardUiKit.WARN : DashboardUiKit.INK);
                    lbl.setStyle("font-weight:800;color:" + warna + ";");
                } else if (kind == 1) {
                    lbl = new Label(DashboardUiKit.money(num(v)));
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

    private String htmlCached(String tabKey, final HtmlBuilder builder) {
        try {
            return DashboardCache.l1("kantinstok.html." + tabKey + "." + periodeKeyCache + "." + scopeKey(),
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

    private List<Object[]> q(String panelKey, final String sql) {
        return q(panelKey, sql, null);
    }

    private List<Object[]> q(String panelKey, final String sql, final org.hibernate.type.Type[] tipos) {
        try {
            return DashboardCache.l3("kantinstok." + panelKey + "." + periodeKeyCache + "." + scopeKey(),
                    new DashboardCache.Loader<List<Object[]>>() {
                        @Override
                        public List<Object[]> load() throws Exception {
                            return rows(sql, tipos);
                        }
                    });
        } catch (Exception e) {
            return rows(sql, tipos);
        }
    }

    private List<Object[]> qShared(String panelKey, final String sql) {
        return qShared(panelKey, sql, null);
    }

    private List<Object[]> qShared(String panelKey, final String sql, final org.hibernate.type.Type[] tipos) {
        try {
            return DashboardCache.l3("kantinstok." + panelKey + "." + scopeKey(),
                    new DashboardCache.Loader<List<Object[]>>() {
                        @Override
                        public List<Object[]> load() throws Exception {
                            return rows(sql, tipos);
                        }
                    });
        } catch (Exception e) {
            return rows(sql, tipos);
        }
    }

    private List<Object[]> rows(String sql) {
        return rows(sql, null);
    }

    /**
     * <h3>Kenapa {@code tipos} ada (2026-07-26, fix bug produksi ID 150).</h3>
     *
     * <p>Tanpa deklarasi tipe eksplisit, {@code SQLQuery} native menyerahkan penentuan tipe tiap
     * kolom skalar ke {@code ResultSetMetaData} driver JDBC -- untuk query yang mencampur
     * {@code COALESCE(kolom_teks, '')} dengan beberapa {@code COALESCE(kolom_numerik, 0)} dalam
     * SELECT list yang sama, driver PostgreSQL/pgjdbc TERBUKTI kadang salah menebak tipe kolom teks
     * sbg {@code double} (literal string kosong {@code ''} tak bertipe ikut memengaruhi resolusi tipe
     * ekspresi {@code COALESCE} di planner) -- menghasilkan
     * {@code PSQLException: Bad value for type double : <isi kode transaksi>} persis error yang
     * dilaporkan (log produksi ID 150, {@code qKeluar()}). Dengan {@code tipos} diisi, tipe kolom
     * DIPAKSA eksplisit lewat {@code addScalar}, sepenuhnya melewati tebakan driver yang keliru itu --
     * pemanggil WAJIB memberi alias {@code c0,c1,c2,...} pada tiap kolom SELECT-nya (lihat
     * {@link #qStok()}/{@link #qMasuk()}/{@link #qKeluar()} sbg contoh) supaya {@code addScalar} bisa
     * menunjuk kolom yang benar. {@code tipos == null} = perilaku lama (auto-detect), dipertahankan
     * apa adanya utk seluruh panel lain yang tidak mencampur teks+angka dlm pola berisiko ini.</p>
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, org.hibernate.type.Type[] tipos) {
        try {
            SQLQuery query = HibernateUtil.currentSession().createSQLQuery(sql);
            if (tipos != null) {
                for (int i = 0; i < tipos.length; i++) {
                    query.addScalar("c" + i, tipos[i]);
                }
            }
            try {
                query.setCacheable(true);
            } catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/DashboardStokKantinAction.java:693");
            }
            return query.list();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
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
}

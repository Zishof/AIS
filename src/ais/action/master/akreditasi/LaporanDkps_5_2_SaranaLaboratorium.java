package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;

/**
 * DKPS Tabel 5.2 — Sarana Laboratorium dan Pembelajaran Program Studi.
 *
 * <p>Tabel ini menyajikan inventarisasi sarana pembelajaran khususnya peralatan
 * laboratorium dan fasilitas penunjang proses pembelajaran yang dimiliki atau
 * digunakan oleh program studi. Ketersediaan dan kondisi sarana laboratorium
 * merupakan indikator kritis dalam penilaian akreditasi LAMDIK 2.0 karena
 * berpengaruh langsung terhadap kualitas pembelajaran berbasis praktikum.</p>
 *
 * <p>Penilaian sarana meliputi aspek: jumlah unit yang tersedia, status
 * kepemilikan (milik sendiri atau sewa), kondisi fisik peralatan
 * (Sangat Baik / Baik / Cukup / Kurang), dan tingkat utilisasi peralatan
 * dalam persen terhadap kapasitas maksimalnya. Utilisasi yang rendah dapat
 * mengindikasikan adanya sarana yang tidak terpakai secara optimal.</p>
 *
 * <p>Sarana yang diinventarisasi mencakup: perangkat laboratorium komputer,
 * peralatan LCD proyektor, laptop/PC, papan tulis/whiteboard, dan
 * peralatan laboratorium IPA atau bidang keilmuan program studi.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code inventaris} atau {@code aset} dengan
 * filter kategori laboratorium/pembelajaran dan join ke {@code jurusan}.
 * Jika tidak tersedia, ditampilkan 5 baris template sarana standar.</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Jenis Sarana</li>
 *   <li>Jumlah Unit</li>
 *   <li>Kepemilikan (Milik/Sewa)</li>
 *   <li>Kondisi</li>
 *   <li>Utilisasi (%)</li>
 *   <li>Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_5_2_SaranaLaboratorium extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-5.2";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_5_2_SaranaLaboratorium() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Konstruktor dengan parameter tampilan jendela.
     *
     * @param title    judul jendela
     * @param border   gaya border ZK
     * @param closable {@code true} jika jendela dapat ditutup
     * @throws Exception jika ZK gagal membuat komponen
     */
    public LaporanDkps_5_2_SaranaLaboratorium(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    /** @return kode sheet {@value #sheetCode} */
    @Override
    protected String getSheetCode() {
        return sheetCode;
    }

    /**
     * Membangun baris filter: Fakultas dan Program Studi.
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Memuat data sarana laboratorium dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code inventaris} atau {@code aset} dengan
     * filter kategori LABORATORIUM/PEMBELAJARAN. Jika tidak ada data,
     * ditampilkan 5 baris template sarana standar LAMDIK.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data sarana laboratorium ..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        session = HibernateUtil.currentNativeSession();

                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 5; i++) {
                            datas.add(new ArrayList());
                        }

                        boolean loaded = false;

                        // Coba tabel inventaris
                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='inventaris'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = "";
                                if (selectedJurusan != null) {
                                    jurusanFilter = " AND j.id = " + selectedJurusan.getId();
                                }
                                String sql = "SELECT inv.nama AS jenis_sarana,"
                                    + " COUNT(*) AS jumlah,"
                                    + " CASE WHEN inv.kepemilikan='SEWA' THEN 'Sewa' ELSE 'Milik' END AS kepemilikan,"
                                    + " COALESCE(inv.kondisi, 'Baik') AS kondisi,"
                                    + " COALESCE(CAST(inv.utilisasi AS text), '-') AS utilisasi"
                                    + " FROM inventaris inv"
                                    + " LEFT JOIN jurusan j ON inv.jurusan = j.id"
                                    + " WHERE (UPPER(COALESCE(inv.kategori,'')) LIKE '%LABORATORIUM%'"
                                    + "   OR UPPER(COALESCE(inv.kategori,'')) LIKE '%PEMBELAJARAN%')"
                                    + jurusanFilter
                                    + " GROUP BY inv.nama, inv.kepemilikan, inv.kondisi, inv.utilisasi"
                                    + " ORDER BY inv.nama"
                                    + " LIMIT 30";

                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(noUrut);
                                    sub.add(obj[0] != null ? obj[0].toString() : "");
                                    sub.add(obj[1] != null ? obj[1].toString() : "0");
                                    sub.add(obj[2] != null ? obj[2].toString() : "Milik");
                                    sub.add(obj[3] != null ? obj[3].toString() : "Baik");
                                    sub.add(obj[4] != null ? obj[4].toString() : "-");
                                    sub.add("");
                                    sub.add(""); // kolom ke-8
                                    sub.add(""); // kolom ke-9
                                    sub.add(""); // kolom ke-10
                                    datas.add(sub);
                                    noUrut++;
                                }
                                loaded = rows != null && rows.size() > 0;
                            }
                        } catch (Exception exQ1) {
                            exQ1.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ1, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_2_SaranaLaboratorium.java:179");
                        }

                        // Coba tabel aset bila inventaris kosong
                        if (!loaded) {
                            try {
                                String checkSql2 = "SELECT COUNT(*) FROM information_schema.tables "
                                    + "WHERE table_schema='public' AND table_name='aset'";
                                Object cnt2 = session.createSQLQuery(checkSql2).uniqueResult();
                                boolean tableExists2 = cnt2 != null && Long.parseLong(cnt2.toString()) > 0;

                                if (tableExists2) {
                                    String jurusanFilter = "";
                                    if (selectedJurusan != null) {
                                        jurusanFilter = " AND j.id = " + selectedJurusan.getId();
                                    }
                                    String sql2 = "SELECT a.nama AS jenis_sarana,"
                                        + " COUNT(*) AS jumlah,"
                                        + " CASE WHEN a.status_kepemilikan='SEWA' THEN 'Sewa' ELSE 'Milik' END AS kepemilikan,"
                                        + " COALESCE(a.kondisi, 'Baik') AS kondisi,"
                                        + " '-' AS utilisasi"
                                        + " FROM aset a"
                                        + " LEFT JOIN jurusan j ON a.jurusan = j.id"
                                        + " WHERE (UPPER(COALESCE(a.kategori,'')) LIKE '%LABORATORIUM%'"
                                        + "   OR UPPER(COALESCE(a.kategori,'')) LIKE '%PEMBELAJARAN%')"
                                        + jurusanFilter
                                        + " GROUP BY a.nama, a.status_kepemilikan, a.kondisi"
                                        + " ORDER BY a.nama"
                                        + " LIMIT 30";
                                    List<Object[]> rows2 = session.createSQLQuery(sql2).list();
                                    int noUrut = 1;
                                    for (Object[] obj : rows2) {
                                        List sub = new ArrayList();
                                        sub.add(noUrut);
                                        sub.add(obj[0] != null ? obj[0].toString() : "");
                                        sub.add(obj[1] != null ? obj[1].toString() : "0");
                                        sub.add(obj[2] != null ? obj[2].toString() : "Milik");
                                        sub.add(obj[3] != null ? obj[3].toString() : "Baik");
                                        sub.add(obj[4] != null ? obj[4].toString() : "-");
                                        sub.add("");
                                        sub.add(""); // kolom ke-8
                                        sub.add(""); // kolom ke-9
                                        sub.add(""); // kolom ke-10
                                        datas.add(sub);
                                        noUrut++;
                                    }
                                    loaded = rows2 != null && rows2.size() > 0;
                                }
                            } catch (Exception exQ2) {
                                exQ2.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ2, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_2_SaranaLaboratorium.java:228");
                            }
                        }

                        if (!loaded) {
                            // Template 5 sarana standar
                            String[][] template = {
                                {"Laboratorium Komputer", "Milik", "Baik"},
                                {"LCD Proyektor", "Milik", "Baik"},
                                {"Laptop/PC", "Milik", "Baik"},
                                {"Papan Tulis/Whiteboard", "Milik", "Sangat Baik"},
                                {"Peralatan Laboratorium IPA", "Milik", "Baik"}
                            };
                            for (int i = 0; i < template.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add(template[i][0]);
                                sub.add(0);           // Jumlah
                                sub.add(template[i][1]); // Kepemilikan
                                sub.add(template[i][2]); // Kondisi
                                sub.add("-");         // Utilisasi
                                sub.add("");          // Keterangan
                                sub.add("");          // kolom ke-8
                                sub.add("");          // kolom ke-9
                                sub.add("");          // kolom ke-10
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_2_SaranaLaboratorium.java:260");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_2_SaranaLaboratorium.java:264");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_2_SaranaLaboratorium.java:265");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 10);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

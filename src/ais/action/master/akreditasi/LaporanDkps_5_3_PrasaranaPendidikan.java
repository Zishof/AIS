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
 * DKPS Tabel 5.3 — Prasarana Pendidikan Program Studi.
 *
 * <p>Tabel ini menyajikan inventarisasi prasarana pendidikan yang digunakan
 * oleh program studi, mencakup ruang kuliah, ruang dosen, ruang prodi,
 * toilet, tempat ibadah, dan fasilitas parkir. Ketersediaan prasarana yang
 * memadai merupakan indikator kritis dalam penilaian akreditasi LAMDIK 2.0
 * untuk menilai kelayakan dan kenyamanan lingkungan belajar mahasiswa.</p>
 *
 * <p>Standar minimum prasarana yang wajib ada meliputi: rasio luas ruang
 * kuliah minimal 2 m² per mahasiswa, ruang dosen dengan luas minimal 4 m²
 * per dosen, dan ruang prodi/administrasi yang memadai. Kondisi prasarana
 * dikategorikan ke dalam empat tingkat: Sangat Baik, Baik, Cukup, dan Kurang.</p>
 *
 * <p>Status kepemilikan prasarana penting untuk akreditasi: prasarana milik
 * sendiri mendapat penilaian lebih tinggi dibandingkan yang disewa atau
 * pinjam-pakai. Luas total dicatat dalam satuan meter persegi (m²).</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code gedung} atau {@code ruangan}
 * dengan GROUP BY jenis prasarana. Jika tidak tersedia, ditampilkan
 * 6 baris template jenis prasarana standar LAMDIK.</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Jenis Prasarana</li>
 *   <li>Jumlah Unit</li>
 *   <li>Luas Total (m²)</li>
 *   <li>Kepemilikan (Milik/Sewa)</li>
 *   <li>Kondisi (Sangat Baik/Baik/Cukup/Kurang)</li>
 *   <li>Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_5_3_PrasaranaPendidikan extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-5.3";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_5_3_PrasaranaPendidikan() {
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
    public LaporanDkps_5_3_PrasaranaPendidikan(String title, String border, boolean closable) throws Exception {
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
     * Memuat data prasarana pendidikan dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code gedung} atau {@code ruangan}
     * dengan GROUP BY jenis. Jika tidak ada data, ditampilkan 6 baris
     * template prasarana standar LAMDIK.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data prasarana pendidikan ..."));

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

                        // Coba tabel ruangan
                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='ruangan'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = "";
                                if (selectedJurusan != null) {
                                    jurusanFilter = " AND r.jurusan = " + selectedJurusan.getId();
                                }
                                String sql = "SELECT COALESCE(r.jenis, r.nama) AS jenis_prasarana,"
                                    + " COUNT(*) AS jumlah,"
                                    + " COALESCE(SUM(r.luas), 0) AS luas_total,"
                                    + " CASE WHEN r.kepemilikan='SEWA' THEN 'Sewa' ELSE 'Milik' END AS kepemilikan,"
                                    + " COALESCE(r.kondisi, 'Baik') AS kondisi"
                                    + " FROM ruangan r"
                                    + " WHERE 1=1" + jurusanFilter
                                    + " GROUP BY COALESCE(r.jenis, r.nama), r.kepemilikan, r.kondisi"
                                    + " ORDER BY jenis_prasarana"
                                    + " LIMIT 30";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(noUrut);
                                    sub.add(obj[0] != null ? obj[0].toString() : "");
                                    sub.add(obj[1] != null ? obj[1].toString() : "0");
                                    sub.add(obj[2] != null ? obj[2].toString() : "0");
                                    sub.add(obj[3] != null ? obj[3].toString() : "Milik");
                                    sub.add(obj[4] != null ? obj[4].toString() : "Baik");
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
                            exQ1.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ1, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_3_PrasaranaPendidikan.java:174");
                        }

                        // Coba tabel gedung bila ruangan kosong
                        if (!loaded) {
                            try {
                                String checkSql2 = "SELECT COUNT(*) FROM information_schema.tables "
                                    + "WHERE table_schema='public' AND table_name='gedung'";
                                Object cnt2 = session.createSQLQuery(checkSql2).uniqueResult();
                                boolean tableExists2 = cnt2 != null && Long.parseLong(cnt2.toString()) > 0;

                                if (tableExists2) {
                                    String sql2 = "SELECT g.jenis AS jenis_prasarana,"
                                        + " COUNT(*) AS jumlah,"
                                        + " COALESCE(SUM(g.luas), 0) AS luas_total,"
                                        + " 'Milik' AS kepemilikan,"
                                        + " COALESCE(g.kondisi, 'Baik') AS kondisi"
                                        + " FROM gedung g"
                                        + " GROUP BY g.jenis, g.kondisi"
                                        + " ORDER BY g.jenis"
                                        + " LIMIT 30";
                                    List<Object[]> rows2 = session.createSQLQuery(sql2).list();
                                    int noUrut = 1;
                                    for (Object[] obj : rows2) {
                                        List sub = new ArrayList();
                                        sub.add(noUrut);
                                        sub.add(obj[0] != null ? obj[0].toString() : "");
                                        sub.add(obj[1] != null ? obj[1].toString() : "0");
                                        sub.add(obj[2] != null ? obj[2].toString() : "0");
                                        sub.add(obj[3] != null ? obj[3].toString() : "Milik");
                                        sub.add(obj[4] != null ? obj[4].toString() : "Baik");
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
                                exQ2.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ2, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_3_PrasaranaPendidikan.java:215");
                            }
                        }

                        if (!loaded) {
                            // Template 6 jenis prasarana standar LAMDIK
                            String[] jenisPrasarana = {
                                "Ruang Kuliah",
                                "Ruang Dosen",
                                "Ruang Prodi",
                                "Toilet",
                                "Mushalla/Tempat Ibadah",
                                "Parkir"
                            };
                            for (int i = 0; i < jenisPrasarana.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add(jenisPrasarana[i]);
                                sub.add(0);       // Jumlah
                                sub.add(0);       // Luas Total (m²)
                                sub.add("Milik"); // Kepemilikan
                                sub.add("Baik");  // Kondisi
                                sub.add("");      // Keterangan
                                sub.add("");      // kolom ke-8
                                sub.add("");      // kolom ke-9
                                sub.add("");      // kolom ke-10
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_3_PrasaranaPendidikan.java:248");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_3_PrasaranaPendidikan.java:252");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_3_PrasaranaPendidikan.java:253");}
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

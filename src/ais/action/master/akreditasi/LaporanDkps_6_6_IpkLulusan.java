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
 * DKPS Tabel 6.6 — Distribusi IPK Lulusan Program Studi.
 *
 * <p>Tabel ini menyajikan distribusi Indeks Prestasi Kumulatif (IPK) lulusan
 * program studi dikelompokkan per tahun kelulusan dan per rentang nilai IPK.
 * Data ini merupakan indikator utama keberhasilan proses pembelajaran dan
 * kualitas lulusan program studi dalam penilaian akreditasi LAMDIK 2.0.</p>
 *
 * <p>IPK lulusan dikategorikan ke dalam empat rentang nilai berdasarkan standar
 * LAMDIK: (1) IPK kurang dari 2,75 dikategorikan sebagai IPK Rendah yang
 * menunjukkan lulusan dengan prestasi di bawah standar minimal; (2) IPK antara
 * 2,75 hingga di bawah 3,00 dikategorikan sebagai IPK Cukup; (3) IPK antara
 * 3,00 hingga di bawah 3,50 dikategorikan sebagai IPK Baik yang merupakan
 * mayoritas lulusan program studi yang sehat; serta (4) IPK 3,50 ke atas
 * dikategorikan sebagai IPK Sangat Baik (Cumlaude/Sangat Memuaskan).</p>
 *
 * <p>Program studi yang memiliki rata-rata IPK lulusan di atas 3,00 dan
 * proporsi lulusan dengan IPK di bawah 2,75 kurang dari 10% mendapatkan
 * penilaian baik pada indikator ini. Tren peningkatan rata-rata IPK dari
 * tahun ke tahun juga menjadi pertimbangan tambahan dalam penilaian kualitas
 * lulusan secara keseluruhan.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code mahasiswa} dengan filter mahasiswa yang
 * telah lulus (memiliki {@code tanggal_lulus} dan {@code ipk} tidak null),
 * dikelompokkan berdasarkan tahun kelulusan ({@code EXTRACT(year FROM tanggal_lulus)}).
 * Hasil dibatasi 5 tahun terakhir untuk relevansi laporan akreditasi.</p>
 *
 * <h3>Format baris (maxCols=8)</h3>
 * <ol>
 *   <li>Tahun Lulus</li>
 *   <li>Jumlah Lulusan</li>
 *   <li>IPK Rendah (&lt;2,75)</li>
 *   <li>IPK Cukup (2,75–&lt;3,00)</li>
 *   <li>IPK Baik (3,00–&lt;3,50)</li>
 *   <li>IPK Sangat Baik (≥3,50)</li>
 *   <li>Rata-rata IPK</li>
 *   <li>Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_6_IpkLulusan extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.6";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_6_IpkLulusan() {
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
    public LaporanDkps_6_6_IpkLulusan(String title, String border, boolean closable) throws Exception {
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
     * Memuat distribusi IPK lulusan dari tabel mahasiswa di background thread.
     *
     * <p>Query mengagregasi IPK per tahun lulus dengan menghitung distribusi
     * per rentang IPK dan rata-rata IPK. Dibatasi 5 tahun terakhir.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data ..."));

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

                        String jurusanFilter = "";
                        if (selectedJurusan != null) {
                            jurusanFilter = " AND j.id = " + selectedJurusan.getId();
                        }

                        String sql = "SELECT EXTRACT(year FROM m.tanggal_lulus) as tahun_lulus, "
                            + "COUNT(*) as jml_lulusan, "
                            + "COUNT(CASE WHEN m.ipk < 2.75 THEN 1 END) as ipk_rendah, "
                            + "COUNT(CASE WHEN m.ipk >= 2.75 AND m.ipk < 3.0 THEN 1 END) as ipk_cukup, "
                            + "COUNT(CASE WHEN m.ipk >= 3.0 AND m.ipk < 3.5 THEN 1 END) as ipk_baik, "
                            + "COUNT(CASE WHEN m.ipk >= 3.5 THEN 1 END) as ipk_sangat_baik, "
                            + "ROUND(AVG(m.ipk)::numeric, 2) as rata_ipk "
                            + "FROM mahasiswa m INNER JOIN jurusan j ON m.jurusan=j.id "
                            + "WHERE m.ipk IS NOT NULL AND m.tanggal_lulus IS NOT NULL"
                            + jurusanFilter
                            + " GROUP BY EXTRACT(year FROM m.tanggal_lulus)"
                            + " ORDER BY tahun_lulus DESC LIMIT 5";

                        try {
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(obj[0] != null ? obj[0].toString() : "");  // Tahun Lulus
                                sub.add(obj[1] != null ? obj[1].toString() : "0"); // Jml Lulusan
                                sub.add(obj[2] != null ? obj[2].toString() : "0"); // IPK < 2.75
                                sub.add(obj[3] != null ? obj[3].toString() : "0"); // 2.75<=IPK<3.0
                                sub.add(obj[4] != null ? obj[4].toString() : "0"); // 3.0<=IPK<3.5
                                sub.add(obj[5] != null ? obj[5].toString() : "0"); // IPK >= 3.5
                                sub.add(obj[6] != null ? obj[6].toString() : "0"); // Rata-rata IPK
                                datas.add(sub);
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_6_IpkLulusan.java:167");
                        }

                        if (datas.size() <= 5) {
                            int tahun = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                            for (int i = 0; i < 3; i++) {
                                List sub = new ArrayList();
                                sub.add(String.valueOf(tahun - i));
                                sub.add(0); sub.add(0); sub.add(0);
                                sub.add(0); sub.add(0); sub.add(0);
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_6_IpkLulusan.java:184");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_6_IpkLulusan.java:188");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_6_IpkLulusan.java:189");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 7);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

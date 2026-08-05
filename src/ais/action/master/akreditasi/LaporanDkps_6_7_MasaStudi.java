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
 * DKPS Tabel 6.7 — Masa Studi Lulusan Program Studi.
 *
 * <p>Tabel ini menyajikan data masa studi rata-rata lulusan program studi
 * dikelompokkan per angkatan (tahun masuk), yang merupakan salah satu indikator
 * kinerja kunci (Key Performance Indicator) program studi dalam penilaian
 * akreditasi LAMDIK 2.0. Masa studi yang tepat waktu mencerminkan efektivitas
 * proses pembelajaran dan efisiensi pengelolaan akademik program studi.</p>
 *
 * <p>Ketentuan masa studi yang ideal menurut standar BAN-PT/LAMDIK adalah:
 * Program S1 = 8 semester (4 tahun), dengan toleransi maksimal 14 semester
 * (7 tahun). Program S2 = 4 semester (2 tahun), maksimal 8 semester (4 tahun).
 * Program S3 = 6 semester (3 tahun), maksimal 14 semester (7 tahun).
 * Mahasiswa yang menyelesaikan studi tepat waktu atau lebih cepat dari rata-rata
 * standar memberikan kontribusi positif terhadap nilai akreditasi program studi.</p>
 *
 * <p>Masa studi dihitung dalam satuan semester, diperoleh dari selisih antara
 * tahun masuk (angkatan) dengan tahun kelulusan, kemudian dikonversi ke semester.
 * Rata-rata masa studi per angkatan dihitung dari seluruh lulusan pada angkatan
 * tersebut. Kecenderungan penurunan rata-rata masa studi dari angkatan ke angkatan
 * menunjukkan perbaikan kualitas pengelolaan akademik program studi.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code mahasiswa} dengan filter mahasiswa yang
 * telah lulus (memiliki {@code tanggal_lulus}), dikelompokkan berdasarkan
 * {@code tahunangkatan}. Masa studi rata-rata dihitung menggunakan selisih
 * tahun lulus dengan tahun angkatan dikalikan 2 (konversi ke semester).
 * Dibatasi pada 5 angkatan terakhir yang telah lulus.</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>Tahun Angkatan</li>
 *   <li>Jumlah Lulusan dari Angkatan ini</li>
 *   <li>Masa Studi Minimum (semester)</li>
 *   <li>Masa Studi Maksimum (semester)</li>
 *   <li>Masa Studi Rata-rata (semester)</li>
 *   <li>Persen Lulus Tepat Waktu (%)</li>
 *   <li>Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_7_MasaStudi extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.7";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_7_MasaStudi() {
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
    public LaporanDkps_6_7_MasaStudi(String title, String border, boolean closable) throws Exception {
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
     * Memuat data masa studi lulusan dari tabel mahasiswa di background thread.
     *
     * <p>Query mengagregasi data lulusan per angkatan untuk menghitung masa
     * studi rata-rata dalam semester. Dibatasi pada 5 angkatan terakhir.</p>
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

                        // DKPS-6.7: Tahun Masuk, Jml Diterima, Lulus TS-4/TS-3/TS-2/TS-1/TS,
                        //            Total Lulusan s.d. Akhir TS, Rata-rata Masa Studi
                        int tsYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                        int ts4 = tsYear - 4;
                        int ts3 = tsYear - 3;
                        int ts2 = tsYear - 2;
                        int ts1 = tsYear - 1;

                        String sql = "SELECT m.tahunangkatan,"
                            + " COUNT(*) as jml_diterima,"
                            + " COUNT(CASE WHEN EXTRACT(year FROM m.tanggal_lulus)=" + ts4 + " THEN 1 END) as lulus_ts4,"
                            + " COUNT(CASE WHEN EXTRACT(year FROM m.tanggal_lulus)=" + ts3 + " THEN 1 END) as lulus_ts3,"
                            + " COUNT(CASE WHEN EXTRACT(year FROM m.tanggal_lulus)=" + ts2 + " THEN 1 END) as lulus_ts2,"
                            + " COUNT(CASE WHEN EXTRACT(year FROM m.tanggal_lulus)=" + ts1 + " THEN 1 END) as lulus_ts1,"
                            + " COUNT(CASE WHEN EXTRACT(year FROM m.tanggal_lulus)=" + tsYear + " THEN 1 END) as lulus_ts,"
                            + " COUNT(CASE WHEN m.tanggal_lulus IS NOT NULL"
                            + "   AND EXTRACT(year FROM m.tanggal_lulus)<=" + tsYear + " THEN 1 END) as total_lulus,"
                            + " ROUND(AVG(CASE WHEN m.tanggal_lulus IS NOT NULL"
                            + "   THEN (EXTRACT(year FROM m.tanggal_lulus)-m.tahunangkatan)*2 END)::numeric,1) as rata_smt"
                            + " FROM mahasiswa m INNER JOIN jurusan j ON m.jurusan=j.id"
                            + " WHERE m.tahunangkatan IS NOT NULL"
                            + jurusanFilter
                            + " GROUP BY m.tahunangkatan"
                            + " ORDER BY m.tahunangkatan DESC LIMIT 5";

                        try {
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(obj[0] != null ? obj[0].toString() : ""); // Tahun Masuk
                                sub.add(obj[1] != null ? obj[1].toString() : "0"); // Jml Diterima
                                sub.add(obj[2] != null ? obj[2].toString() : "0"); // Lulus TS-4
                                sub.add(obj[3] != null ? obj[3].toString() : "0"); // Lulus TS-3
                                sub.add(obj[4] != null ? obj[4].toString() : "0"); // Lulus TS-2
                                sub.add(obj[5] != null ? obj[5].toString() : "0"); // Lulus TS-1
                                sub.add(obj[6] != null ? obj[6].toString() : "0"); // Lulus TS
                                sub.add(obj[7] != null ? obj[7].toString() : "0"); // Total Lulusan s.d. Akhir TS
                                sub.add(obj[8] != null ? obj[8].toString() : "-"); // Rata-rata Masa Studi
                                datas.add(sub);
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_7_MasaStudi.java:181");
                        }

                        if (datas.size() <= 5) {
                            for (int i = 0; i < 5; i++) {
                                List sub = new ArrayList();
                                sub.add(String.valueOf(tsYear - 4 + i));
                                sub.add(0); sub.add(0); sub.add(0); sub.add(0);
                                sub.add(0); sub.add(0); sub.add(0); sub.add("-");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_7_MasaStudi.java:197");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_7_MasaStudi.java:201");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_7_MasaStudi.java:202");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 9);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

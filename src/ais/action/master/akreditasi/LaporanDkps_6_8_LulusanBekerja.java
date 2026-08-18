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
 * DKPS Tabel 6.8 — Lulusan yang Bekerja, Studi Lanjut, dan Wirausaha.
 *
 * <p>Tabel ini menyajikan data luaran program studi berupa status kegiatan
 * lulusan setelah menyelesaikan studi, mencakup jumlah yang bekerja,
 * melanjutkan studi, berwirausaha, dan yang belum bekerja, dikelompokkan
 * per tahun kelulusan. Instrumen ini berbeda dengan waktu tunggu (6.9) karena
 * berfokus pada JENIS kegiatan lulusan, bukan durasi mendapatkan pekerjaan.</p>
 *
 * <p>Data ini merupakan salah satu indikator kinerja utama (IKU) program studi
 * yang paling diperhatikan dalam akreditasi LAMDIK 2.0. Proporsi lulusan yang
 * bekerja mencerminkan daya serap lulusan di dunia kerja, sedangkan proporsi
 * yang studi lanjut menggambarkan kualitas lulusan yang diakui oleh perguruan
 * tinggi jenjang lebih tinggi.</p>
 *
 * <p>Kategori "Bekerja" mencakup bekerja di bidang yang sesuai maupun tidak
 * sesuai dengan bidang studi, baik sebagai pegawai tetap, kontrak, atau
 * honorer. Kategori "Wirausaha" khusus untuk lulusan yang secara aktif
 * mengelola usaha sendiri sebagai pekerjaan utama.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code tracer_study} dengan kolom
 * {@code status_kerja}, {@code bekerja}, atau {@code studi_lanjut},
 * dikelompokkan berdasarkan tahun lulus mahasiswa. Jika tidak tersedia,
 * ditampilkan template 5 tahun terakhir dengan nilai nol sebagai panduan
 * pengisian manual dari data tracer study offline.</p>
 *
 * <h3>Format baris (maxCols=6)</h3>
 * <ol>
 *   <li>Tahun Lulus</li>
 *   <li>Jumlah Lulusan</li>
 *   <li>Jumlah Bekerja</li>
 *   <li>Jumlah Studi Lanjut</li>
 *   <li>Jumlah Wirausaha</li>
 *   <li>Jumlah Belum Bekerja</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_8_LulusanBekerja extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.8";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_8_LulusanBekerja() {
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
    public LaporanDkps_6_8_LulusanBekerja(String title, String border, boolean closable) throws Exception {
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
     * Memuat data lulusan bekerja/studi lanjut dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code tracer_study} JOIN {@code mahasiswa}
     * dan {@code jurusan}, menghitung jumlah per status kegiatan per tahun
     * kelulusan. Jika tidak ada data, ditampilkan template 5 tahun terakhir
     * dengan nilai nol sebagai panduan pengisian manual.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data lulusan bekerja dan studi lanjut ..."));

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

                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='tracer_study'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = "";
                                if (selectedJurusan != null) {
                                    jurusanFilter = " AND j.id = " + selectedJurusan.getId();
                                }
                                // DKPS-6.8: Tahun Lulus, Jml Lulusan, Terlacak, Bekerja sesuai Bidang,
                                //            Usaha Mandiri, Studi Lanjut S2, Mengikuti PPG
                                String sql = "SELECT EXTRACT(year FROM m.tanggal_lulus) AS tahun_lulus,"
                                    + " COUNT(*) AS total_lulusan,"
                                    + " COUNT(ts.id) AS terlacak,"
                                    + " COUNT(CASE WHEN UPPER(COALESCE(ts.status_kerja,'')) IN ('BEKERJA','KERJA') THEN 1 END) AS bekerja_bidang,"
                                    + " COUNT(CASE WHEN UPPER(COALESCE(ts.status_kerja,''))='WIRAUSAHA' THEN 1 END) AS usaha_mandiri,"
                                    + " COUNT(CASE WHEN (ts.studi_lanjut=true OR UPPER(COALESCE(ts.status_kerja,''))='STUDI LANJUT') THEN 1 END) AS studi_s2,"
                                    + " COUNT(CASE WHEN UPPER(COALESCE(ts.status_kerja,''))='PPG' THEN 1 END) AS ikut_ppg"
                                    + " FROM mahasiswa m"
                                    + " INNER JOIN jurusan j ON m.jurusan = j.id"
                                    + " LEFT JOIN tracer_study ts ON ts.mahasiswa = m.id"
                                    + " WHERE m.tanggal_lulus IS NOT NULL" + jurusanFilter
                                    + " GROUP BY EXTRACT(year FROM m.tanggal_lulus)"
                                    + " ORDER BY tahun_lulus DESC"
                                    + " LIMIT 5";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(obj[0] != null ? obj[0].toString() : "-");  // Tahun Lulus
                                    sub.add(obj[1] != null ? obj[1].toString() : "0");  // Jml Lulusan
                                    sub.add(obj[2] != null ? obj[2].toString() : "0");  // Terlacak
                                    sub.add(obj[3] != null ? obj[3].toString() : "0");  // Bekerja sesuai Bidang
                                    sub.add(obj[4] != null ? obj[4].toString() : "0");  // Usaha Mandiri
                                    sub.add(obj[5] != null ? obj[5].toString() : "0");  // Studi Lanjut S2
                                    sub.add(obj[6] != null ? obj[6].toString() : "0");  // Mengikuti PPG
                                    datas.add(sub);
                                }
                                loaded = rows != null && rows.size() > 0;
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_8_LulusanBekerja.java:178");
                        }

                        if (!loaded) {
                            int tahunSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                            for (int y = tahunSekarang - 4; y <= tahunSekarang; y++) {
                                List sub = new ArrayList();
                                sub.add(y); sub.add(0); sub.add(0); sub.add(0);
                                sub.add(0); sub.add(0); sub.add(0);
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_8_LulusanBekerja.java:194");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_8_LulusanBekerja.java:198");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_8_LulusanBekerja.java:199");}
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

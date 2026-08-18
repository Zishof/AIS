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
 * DKPS Tabel 6.3 — Pembimbingan Magang Kependidikan (PPL/PLP/PPG).
 *
 * <p>Tabel ini mendokumentasikan kegiatan praktik mengajar atau magang
 * kependidikan mahasiswa di sekolah mitra, yang dikenal dengan nama PPL
 * (Praktik Pengalaman Lapangan), PLP (Pengenalan Lapangan Persekolahan),
 * atau PPG (Pendidikan Profesi Guru) tergantung program dan jenjangnya.
 * Kegiatan ini wajib ada pada program studi kependidikan (FKIP) dalam
 * akreditasi LAMDIK 2.0 sebagai bukti implementasi kurikulum berbasis praktik.</p>
 *
 * <p>Setiap mahasiswa yang mengikuti magang kependidikan harus mendapatkan
 * pembimbing dari dua pihak: (1) Dosen Pembimbing dari Program Studi yang
 * bertugas membimbing secara metodologi dan akademis; dan (2) Guru Pamong
 * dari sekolah mitra yang membimbing langsung pelaksanaan mengajar di kelas.
 * Kolaborasi antara kedua pembimbing ini mencerminkan kemitraan efektif
 * antara perguruan tinggi dan sekolah.</p>
 *
 * <p>Penilaian dilakukan secara komprehensif mencakup aspek perencanaan
 * pembelajaran, pelaksanaan pembelajaran, pengelolaan kelas, evaluasi,
 * dan profesionalisme mahasiswa selama praktik.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code magang_kependidikan} atau
 * {@code pkl} dengan filter jenis PPL atau PLP, di-join dengan tabel
 * {@code mahasiswa} dan {@code jurusan}. Jika tidak tersedia, ditampilkan
 * 5 baris template kosong sebagai panduan pengisian manual.</p>
 *
 * <h3>Format baris (maxCols=8)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Mahasiswa</li>
 *   <li>NIM</li>
 *   <li>Tempat Magang (Sekolah)</li>
 *   <li>Nama Pembimbing PS</li>
 *   <li>Nama Guru Pamong</li>
 *   <li>Semester/Tahun</li>
 *   <li>Nilai</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_3_MagangKependidikan extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.3";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_3_MagangKependidikan() {
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
    public LaporanDkps_6_3_MagangKependidikan(String title, String border, boolean closable) throws Exception {
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
     * Memuat data magang kependidikan dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code magang_kependidikan} atau {@code pkl}
     * dengan filter jenis PPL/PLP, di-join mahasiswa dan jurusan.
     * Jika tidak ada data, ditampilkan 5 baris template kosong.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data magang kependidikan ..."));

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

                        // DKPS-6.3: per-Dosen — No, Nama Dosen Pembimbing,
                        // Jml Mhs TS-2/TS-1/TS, Jml Pertemuan TS-2/TS-1/TS, Lama Magang (bulan)
                        int tsYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                        int ts2yr = tsYear - 2;
                        int ts1yr = tsYear - 1;

                        boolean loaded = false;

                        // Coba dari tabel magang_kependidikan (aggregated per-Dosen)
                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='magang_kependidikan'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = selectedJurusan != null
                                    ? " AND j.id = " + selectedJurusan.getId() : "";
                                String sql = "SELECT d.nama AS nama_dosen,"
                                    + " COUNT(CASE WHEN EXTRACT(year FROM mk.tanggal_pelaksanaan)=" + ts2yr + " THEN 1 END) AS mhs_ts2,"
                                    + " COUNT(CASE WHEN EXTRACT(year FROM mk.tanggal_pelaksanaan)=" + ts1yr + " THEN 1 END) AS mhs_ts1,"
                                    + " COUNT(CASE WHEN EXTRACT(year FROM mk.tanggal_pelaksanaan)=" + tsYear + " THEN 1 END) AS mhs_ts,"
                                    + " 0 AS ptm_ts2, 0 AS ptm_ts1, 0 AS ptm_ts,"
                                    + " ROUND(AVG(COALESCE(mk.durasi_bulan, 6))::numeric, 0) AS lama_bulan"
                                    + " FROM magang_kependidikan mk"
                                    + " INNER JOIN dosen d ON mk.dosen_pembimbing=d.id"
                                    + " INNER JOIN mahasiswa m ON mk.mahasiswa=m.id"
                                    + " INNER JOIN jurusan j ON m.jurusan=j.id"
                                    + jurusanFilter
                                    + " GROUP BY d.id, d.nama ORDER BY d.nama LIMIT 50";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(noUrut++);
                                    for (Object o : obj) sub.add(o == null ? "0" : o.toString());
                                    datas.add(sub);
                                }
                                loaded = rows != null && !rows.isEmpty();
                            }
                        } catch (Exception exQ1) {
                            exQ1.printStackTrace(); ais.common.ErrorAuditUtil.record(exQ1, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_3_MagangKependidikan.java:176");
                        }

                        if (!loaded) {
                            for (int i = 1; i <= 5; i++) {
                                List sub = new ArrayList();
                                sub.add(i); sub.add("Nama Dosen Pembimbing");
                                sub.add(0); sub.add(0); sub.add(0); // Jml Mhs TS-2/TS-1/TS
                                sub.add(0); sub.add(0); sub.add(0); // Jml Pertemuan TS-2/TS-1/TS
                                sub.add(6);                          // Lama Magang (bulan)
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_3_MagangKependidikan.java:193");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_3_MagangKependidikan.java:197");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_3_MagangKependidikan.java:198");}
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

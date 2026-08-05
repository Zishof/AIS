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
 * DKPS Tabel 6.5 — Pembimbingan Tugas Akhir/Skripsi oleh DTPS.
 *
 * <p>Tabel ini menyajikan rekapitulasi jumlah mahasiswa bimbingan Tugas Akhir
 * (TA) atau Skripsi per Dosen Tetap Program Studi (DTPS) dalam tiga tahun
 * akademik terakhir (TS-2, TS-1, dan TS). Beban bimbingan TA yang proporsional
 * merupakan indikator penting dalam akreditasi LAMDIK 2.0 untuk menilai
 * efektivitas penyelenggaraan TA sebagai puncak pembelajaran mahasiswa.</p>
 *
 * <p>Standar rasio bimbingan yang wajar adalah tidak lebih dari 6 mahasiswa
 * bimbingan TA per dosen per tahun akademik. Rasio yang terlalu tinggi
 * mengindikasikan potensi menurunnya kualitas bimbingan, sedangkan rasio
 * yang terlalu rendah dapat menunjukkan distribusi pembimbing yang tidak merata.</p>
 *
 * <p>Data dikelompokkan per dosen dan per tahun akademik, disertai rata-rata
 * jumlah bimbingan selama tiga tahun terakhir sebagai gambaran beban kerja
 * bimbingan secara keseluruhan. NIDN dosen dicantumkan untuk memastikan
 * identifikasi yang tepat antara dosen internal dan dosen tetap program studi.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code skripsi} dengan JOIN ke {@code dosen} dan
 * {@code jurusan}, difilter berdasarkan tanggal pengajuan dalam tiga tahun
 * terakhir. Bimbingan dihitung untuk pembimbing 1 maupun pembimbing 2.
 * Jika query gagal, ditampilkan 5 baris template kosong.</p>
 *
 * <h3>Format baris (maxCols=6)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Dosen Pembimbing</li>
 *   <li>NIDN</li>
 *   <li>Jumlah Mhs Bimbingan TS-2</li>
 *   <li>Jumlah Mhs Bimbingan TS-1</li>
 *   <li>Jumlah Mhs Bimbingan TS</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_5_PembimbinganTA extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.5";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_5_PembimbinganTA() {
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
    public LaporanDkps_6_5_PembimbinganTA(String title, String border, boolean closable) throws Exception {
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
     * Memuat data pembimbingan TA dari database di background thread.
     *
     * <p>Query dijalankan pada tabel {@code skripsi} dengan join {@code dosen}
     * dan {@code jurusan}, menghitung jumlah bimbingan per dosen per tahun
     * dalam 3 tahun terakhir. Rata-rata dihitung sebagai total dibagi 3.
     * Jika query gagal, ditampilkan 5 baris template kosong.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data pembimbingan Tugas Akhir ..."));

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

                        int tahunSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                        int ts  = tahunSekarang;
                        int ts1 = tahunSekarang - 1;
                        int ts2 = tahunSekarang - 2;

                        boolean loaded = false;

                        // DKPS-6.5: 12 cols per-DTPS
                        // No, Nama DTPS, Bimbing PS Akreditasi TS-2/TS-1/TS,
                        // Bimbing PS Lain TS-2/TS-1/TS, Pertemuan TS-2/TS-1/TS, Rata-rata
                        try {
                            String jurusanFilter = "";
                            if (selectedJurusan != null) {
                                jurusanFilter = " AND j.id = " + selectedJurusan.getId();
                            }
                            // Bimbingan di PS yang diakreditasi (filter jurusan dosen)
                            String sql = "SELECT d.nama,"
                                + " COUNT(CASE WHEN EXTRACT(year FROM s.tanggal_pengajuan)=" + ts2 + " THEN 1 END) AS bimb_ps_ts2,"
                                + " COUNT(CASE WHEN EXTRACT(year FROM s.tanggal_pengajuan)=" + ts1 + " THEN 1 END) AS bimb_ps_ts1,"
                                + " COUNT(CASE WHEN EXTRACT(year FROM s.tanggal_pengajuan)=" + ts  + " THEN 1 END) AS bimb_ps_ts,"
                                + " 0 AS bimb_lain_ts2, 0 AS bimb_lain_ts1, 0 AS bimb_lain_ts,"
                                + " 0 AS ptm_ts2, 0 AS ptm_ts1, 0 AS ptm_ts,"
                                + " ROUND(COUNT(*)::numeric/3.0, 1) AS rata_rata"
                                + " FROM skripsi s"
                                + " INNER JOIN dosen d ON (s.pembimbing1 = d.id OR s.pembimbing2 = d.id)"
                                + " INNER JOIN jurusan j ON d.jurusan = j.id"
                                + " WHERE s.tanggal_pengajuan >= (current_date - interval '3 years')"
                                + jurusanFilter
                                + " GROUP BY d.id, d.nama"
                                + " ORDER BY d.nama LIMIT 50";

                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int noUrut = 1;
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(noUrut++);
                                for (Object o : obj) sub.add(o == null ? "0" : o.toString());
                                datas.add(sub);
                            }
                            loaded = rows != null && rows.size() > 0;
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_5_PembimbinganTA.java:173");
                        }

                        if (!loaded) {
                            for (int i = 1; i <= 5; i++) {
                                List sub = new ArrayList();
                                sub.add(i); sub.add(""); // No, Nama DTPS
                                sub.add(0); sub.add(0); sub.add(0); // Bimbing PS Akreditasi TS-2/TS-1/TS
                                sub.add(0); sub.add(0); sub.add(0); // Bimbing PS Lain TS-2/TS-1/TS
                                sub.add(0); sub.add(0); sub.add(0); // Pertemuan TS-2/TS-1/TS
                                sub.add(0);                          // Rata-rata
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_5_PembimbinganTA.java:191");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_5_PembimbinganTA.java:195");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_5_PembimbinganTA.java:196");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 12);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

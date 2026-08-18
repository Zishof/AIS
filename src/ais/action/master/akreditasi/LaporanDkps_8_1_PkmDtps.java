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
 * DKPS Tabel 8.1 — Pengabdian kepada Masyarakat (PkM) Dosen Tetap Program Studi (DTPS).
 *
 * <p>Tabel ini menyajikan rekapitulasi kegiatan Pengabdian kepada Masyarakat (PkM)
 * yang dilakukan oleh Dosen Tetap Program Studi (DTPS) dalam tiga tahun akademik
 * terakhir (TS-2, TS-1, dan TS), dikelompokkan berdasarkan sumber pendanaan.
 * PkM adalah salah satu dari Tri Dharma Perguruan Tinggi yang wajib dilaksanakan
 * oleh setiap dosen sebagai bentuk kontribusi nyata ilmu pengetahuan kepada
 * masyarakat dan pemangku kepentingan.</p>
 *
 * <p>Jenis kegiatan PkM yang dapat dilaporkan dalam borang LAMDIK 2.0 meliputi:
 * (1) Penyuluhan atau pelatihan masyarakat sesuai bidang keilmuan program studi;
 * (2) Pendampingan UMKM atau kelompok masyarakat dalam mengembangkan usaha;
 * (3) Pengembangan produk atau teknologi tepat guna untuk masyarakat;
 * (4) Pembinaan sekolah atau institusi pendidikan di sekitar perguruan tinggi;
 * (5) Pelayanan kesehatan atau konsultasi profesional kepada masyarakat;
 * serta (6) KKN (Kuliah Kerja Nyata) tematik yang melibatkan mahasiswa.</p>
 *
 * <p>Sumber pendanaan PkM yang dapat dilaporkan meliputi dana mandiri dosen,
 * dana internal institusi, dana hibah kompetitif nasional (DRTPM Kemendikbud),
 * dana dari pemerintah daerah, dana dari mitra industri atau lembaga swasta,
 * serta dana dari lembaga internasional. Program studi yang memiliki PkM
 * dengan sumber dana beragam dan melibatkan mahasiswa mendapatkan nilai lebih
 * tinggi dalam penilaian akreditasi.</p>
 *
 * <p>Perbedaan utama dengan tabel 7.1 (Penelitian) adalah bahwa PkM bersifat
 * aplikatif dan berorientasi pada penyelesaian masalah nyata di masyarakat,
 * sementara penelitian lebih bersifat ilmiah dan menghasilkan pengetahuan baru.
 * Keduanya harus seimbang dalam portfolio akademik program studi.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code pengajuan_penelitian_dan_pengabdian} dengan
 * filter {@code jenis='PENGABDIAN'}, dikelompokkan berdasarkan {@code sumber_dana}.
 * Join ke tabel {@code dosen} untuk filter DTPS berdasarkan jurusan yang dipilih.
 * Data mencakup tiga tahun terakhir sebelum tahun saat ini.</p>
 *
 * <h3>Format baris (maxCols=6)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Sumber Pendanaan</li>
 *   <li>Jumlah TS-2</li>
 *   <li>Jumlah TS-1</li>
 *   <li>Jumlah TS</li>
 *   <li>Total</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_8_1_PkmDtps extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-8.1";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_8_1_PkmDtps() {
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
    public LaporanDkps_8_1_PkmDtps(String title, String border, boolean closable) throws Exception {
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
     * Memuat data PkM DTPS per sumber dana di background thread.
     *
     * <p>Query mengagregasi jumlah PkM per sumber pendanaan untuk tiga tahun
     * terakhir, menggunakan filter {@code jenis='PENGABDIAN'}.
     * Jika tabel tidak ada, tampilkan template sumber dana standar.</p>
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

                        int tahun = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                        int ts2 = tahun - 2;
                        int ts1 = tahun - 1;

                        boolean loaded = false;
                        try {
                            String jurusanJoin = "";
                            String jurusanFilter = "";
                            if (selectedJurusan != null) {
                                jurusanJoin = " INNER JOIN dosen d2 ON p.dosen_ketua=d2.id";
                                jurusanFilter = " AND d2.jurusan = " + selectedJurusan.getId();
                            }

                            String sql = "SELECT p.sumber_dana, "
                                + "COUNT(CASE WHEN EXTRACT(year FROM p.tanggal_pengajuan)=" + ts2 + " THEN 1 END) as cnt_ts2, "
                                + "COUNT(CASE WHEN EXTRACT(year FROM p.tanggal_pengajuan)=" + ts1 + " THEN 1 END) as cnt_ts1, "
                                + "COUNT(CASE WHEN EXTRACT(year FROM p.tanggal_pengajuan)=" + tahun + " THEN 1 END) as cnt_ts, "
                                + "COUNT(*) as total "
                                + "FROM pengajuan_penelitian_dan_pengabdian p"
                                + jurusanJoin
                                + " WHERE p.jenis='PENGABDIAN'"
                                + " AND EXTRACT(year FROM p.tanggal_pengajuan) BETWEEN " + ts2 + " AND " + tahun
                                + jurusanFilter
                                + " GROUP BY p.sumber_dana"
                                + " ORDER BY total DESC";

                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int noUrut = 1;
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(noUrut);
                                sub.add(obj[0] != null ? obj[0].toString() : "Mandiri"); // Sumber Dana
                                sub.add(obj[1] != null ? obj[1].toString() : "0");  // TS-2
                                sub.add(obj[2] != null ? obj[2].toString() : "0");  // TS-1
                                sub.add(obj[3] != null ? obj[3].toString() : "0");  // TS
                                sub.add(obj[4] != null ? obj[4].toString() : "0");  // Total
                                datas.add(sub);
                                noUrut++;
                            }
                            loaded = rows != null && rows.size() > 0;
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_8_1_PkmDtps.java:183");
                        }

                        if (!loaded) {
                            String[] sumberDanas = {
                                "Mandiri", "Institusi/Perguruan Tinggi",
                                "Kemenristekdikti/Kemendikbud", "Pemerintah Daerah", "Lembaga Swasta/Mitra"
                            };
                            for (int i = 0; i < sumberDanas.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add(sumberDanas[i]);
                                sub.add(0); sub.add(0); sub.add(0); sub.add(0);
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_8_1_PkmDtps.java:203");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_8_1_PkmDtps.java:207");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_8_1_PkmDtps.java:208");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 6);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

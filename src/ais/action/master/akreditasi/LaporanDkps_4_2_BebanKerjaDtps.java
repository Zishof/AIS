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
 * DKPS Tabel 4.2 — Beban Kerja Dosen Tetap Program Studi (DTPS).
 *
 * <p>Menampilkan rekap beban kerja dosen tetap program studi dalam satu semester
 * aktif, meliputi SKS mengajar, jumlah kegiatan penelitian sebagai ketua, dan
 * kolom-kolom isian manual untuk SKS penelitian, pengabdian, dan manajemen.
 * Dalam instrumen akreditasi LAMDIK 2.0, tabel beban kerja DTPS digunakan untuk
 * mengevaluasi pemenuhan ekuivalensi waktu mengajar penuh (EWMP) setiap dosen
 * dan kesesuaiannya dengan ketentuan Permendikbud tentang beban kerja dosen.</p>
 *
 * <p>Komponen beban kerja dosen yang dievaluasi:</p>
 * <ul>
 *   <li><strong>Pendidikan/Pengajaran</strong> — dihitung berdasarkan SKS mata
 *       kuliah yang diampu pada semester aktif, diambil dari tabel
 *       {@code detail_perkuliahan} yang di-JOIN dengan {@code perkuliahan}
 *       dan {@code matakuliah}.</li>
 *   <li><strong>Penelitian</strong> — jumlah kegiatan penelitian aktif sebagai
 *       ketua di tahun berjalan, diambil dari tabel
 *       {@code pengajuan_penelitian_dan_pengabdian} dengan jenis PENELITIAN.
 *       Nilai SKS penelitian harus diisi manual sesuai SK Rektor.</li>
 *   <li><strong>Pengabdian Masyarakat</strong> — diisi manual oleh admin prodi
 *       karena tidak selalu tercatat di sistem.</li>
 *   <li><strong>Manajemen/Tugas Tambahan</strong> — diisi manual sesuai jabatan
 *       struktural yang dipegang (Dekan, Kaprodi, dll).</li>
 * </ul>
 *
 * <h3>Sumber data otomatis</h3>
 * <p>SKS mengajar diambil dari {@code detail_perkuliahan} yang di-JOIN dengan
 * {@code perkuliahan} dan {@code matakuliah} pada tahun akademik aktif.
 * Jumlah penelitian diambil dari {@code pengajuan_penelitian_dan_pengabdian}
 * dengan filter tahun berjalan dan jenis PENELITIAN. Kedua query dibungkus
 * try-catch tersendiri agar tidak memblokir tampilan jika tabel tidak tersedia.</p>
 *
 * <h3>Format data baris (DKPS-4.2, dataStartRow=5)</h3>
 * <p>Lima baris kosong sebagai padding, diikuti baris data dengan delapan kolom:
 * No, Nama Dosen, SKS Mengajar, SKS Penelitian (isian), SKS Pengabdian (isian),
 * SKS Manajemen (isian), Total SKS, dan Jumlah Penelitian.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 * @see LaporanDkps_4_1_ProfilDtps
 */
public class LaporanDkps_4_2_BebanKerjaDtps extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-4.2";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_4_2_BebanKerjaDtps() {
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
    public LaporanDkps_4_2_BebanKerjaDtps(String title, String border, boolean closable) throws Exception {
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
     * Perubahan nilai filter otomatis memicu {@link #onCetak}.
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Memuat data beban kerja DTPS dari database di background thread.
     *
     * <p>Untuk setiap dosen tetap aktif, dihitung SKS mengajar pada tahun
     * akademik aktif dan jumlah penelitian tahun berjalan. Kolom SKS penelitian,
     * pengabdian, manajemen, dan total dikosongkan (nilai 0) karena memerlukan
     * verifikasi manual oleh admin prodi sesuai SK pembagian tugas.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

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

                        // Cari id tahun akademik aktif
                        long tahunAkademikAktifId = -1L;
                        try {
                            Object taRes = session.createSQLQuery(
                                "SELECT ta.id FROM tahunakademik ta WHERE ta.aktif=true LIMIT 1"
                            ).uniqueResult();
                            if (taRes != null) tahunAkademikAktifId = Long.parseLong(taRes.toString());
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_2_BebanKerjaDtps.java:148");
                        }

                        final long taId = tahunAkademikAktifId;

                        String jurusanWhere = "";
                        if (selectedJurusan != null) {
                            jurusanWhere = " AND d.jurusan=" + selectedJurusan.getId();
                        }

                        String sqlDosen = "SELECT d.id, d.nama FROM dosen d "
                            + "WHERE d.aktif=true AND d.tetap=1"
                            + jurusanWhere
                            + " ORDER BY d.nama";

                        List<Object[]> dosenRows = session.createSQLQuery(sqlDosen).list();
                        int no = 1;
                        for (Object[] dosen : dosenRows) {
                            long dosenId = dosen[0] == null ? 0L : Long.parseLong(dosen[0].toString());
                            String nama = dosen[1] != null ? dosen[1].toString() : "";

                            // SKS Mengajar pada TA Aktif
                            long sksMengajar = 0;
                            if (taId > 0) {
                                try {
                                    String sqlSks = "SELECT COALESCE(SUM(mk.sks),0) "
                                        + "FROM detail_perkuliahan dp "
                                        + "INNER JOIN perkuliahan p ON dp.perkuliahan=p.id "
                                        + "INNER JOIN matakuliah mk ON p.matakuliah=mk.id "
                                        + "WHERE dp.dosen=" + dosenId
                                        + " AND p.tahunakademik=" + taId;
                                    Object sksRes = session.createSQLQuery(sqlSks).uniqueResult();
                                    if (sksRes != null) sksMengajar = Long.parseLong(sksRes.toString());
                                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_2_BebanKerjaDtps.java:181");
                                }
                            }

                            // Jumlah Penelitian tahun berjalan
                            long jmlPenelitian = 0;
                            try {
                                String sqlPen = "SELECT COUNT(*) "
                                    + "FROM pengajuan_penelitian_dan_pengabdian pp "
                                    + "WHERE pp.dosen_ketua=" + dosenId
                                    + " AND pp.jenis='PENELITIAN'"
                                    + " AND EXTRACT(year FROM pp.tanggal_pengajuan)=EXTRACT(year FROM CURRENT_DATE)";
                                Object penRes = session.createSQLQuery(sqlPen).uniqueResult();
                                if (penRes != null) jmlPenelitian = Long.parseLong(penRes.toString());
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_2_BebanKerjaDtps.java:195");
                            }

                            List sub = new ArrayList();
                            sub.add(no++);
                            sub.add(nama);
                            sub.add(sksMengajar);  // SKS PS Akreditasi
                            sub.add(0);            // SKS PS Lain dalam PT
                            sub.add(0);            // SKS PS Lain luar PT
                            sub.add(0);            // Penelitian
                            sub.add(0);            // PkM
                            sub.add(jmlPenelitian); // Tugas Tambahan
                            sub.add(0);            // Jumlah
                            sub.add(0);            // Rata-rata
                            datas.add(sub);
                        }

                        if (dosenRows.isEmpty()) {
                            for (int i = 1; i <= 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add("");
                                sub.add(0); sub.add(0); sub.add(0);
                                sub.add(0); sub.add(0); sub.add(0);
                                sub.add(0); sub.add(0);
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_2_BebanKerjaDtps.java:227");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_2_BebanKerjaDtps.java:231");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_2_BebanKerjaDtps.java:232");}
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

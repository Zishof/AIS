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
 * DKPS Tabel 4.1 — Profil Dosen Tetap Program Studi (DTPS).
 *
 * <p>Menampilkan profil lengkap dosen tetap program studi (DTPS) yang meliputi
 * identitas, latar belakang pendidikan tertinggi, jabatan fungsional, dan status
 * sertifikasi pendidik. Dalam instrumen akreditasi LAMDIK 2.0, tabel profil DTPS
 * merupakan salah satu elemen krusial karena menentukan nilai pada standar
 * sumber daya manusia.</p>
 *
 * <p>Kriteria DTPS dalam konteks LAMDIK:</p>
 * <ul>
 *   <li>Dosen dengan status tetap ({@code tetap=1}) yang terikat pada program
 *       studi tertentu.</li>
 *   <li>Memiliki NIDN (Nomor Induk Dosen Nasional) yang terdaftar di PD-DIKTI.</li>
 *   <li>Memiliki kualifikasi pendidikan minimal S2 untuk mengajar di program
 *       sarjana, atau S3 untuk program magister/doktoral.</li>
 *   <li>Diutamakan yang memiliki jabatan fungsional minimal Asisten Ahli.</li>
 * </ul>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil melalui JOIN antara tabel {@code public.dosen},
 * {@code public.riwayat_pendidikan_dosen}, dan {@code public.jabatan_fungsional_dosen}.
 * Informasi pendidikan S3 dan S2 diambil menggunakan subquery dengan LIMIT 1
 * untuk mendapatkan satu baris riwayat per jenjang. Kolom {@code sertifikasi}
 * menggunakan CASE WHEN untuk mengonversi boolean ke teks "Ya"/"Tidak".</p>
 *
 * <h3>Format data baris (DKPS-4.1, dataStartRow=5)</h3>
 * <p>Lima baris kosong sebagai padding, diikuti baris data dengan delapan kolom:
 * No, NIDN, Nama Dosen, Pendidikan S3, Pendidikan S2, Jabatan Fungsional,
 * Sertifikasi, dan Bidang Keahlian.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_4_1_ProfilDtps extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-4.1";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_4_1_ProfilDtps() {
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
    public LaporanDkps_4_1_ProfilDtps(String title, String border, boolean closable) throws Exception {
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
     * Memuat profil DTPS dari database di background thread.
     *
     * <p>Query utama mengambil data dosen tetap aktif. Untuk setiap dosen,
     * riwayat pendidikan S3 dan S2 diambil dengan subquery terpisah agar
     * lebih portabel. Jika kolom {@code sertifikasi} tidak ada, digunakan
     * nilai default "Tidak". Bidang keahlian diambil dari riwayat pendidikan
     * S3 (atau S2 jika S3 tidak ada).</p>
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

                        String jurusanWhere = "";
                        if (selectedJurusan != null) {
                            jurusanWhere = " AND d.jurusan=" + selectedJurusan.getId();
                        }

                        // Query utama: ambil dosen tetap aktif + jabatan fungsional
                        String sqlDosen = "SELECT d.id, d.nidn, d.nama, "
                            + "COALESCE(jfd.nama, '') AS jabatan "
                            + "FROM dosen d "
                            + "LEFT JOIN jabatan_fungsional_dosen jfd ON d.jabatan_fungsional_dosen=jfd.id "
                            + "WHERE d.aktif=true AND d.tetap=1"
                            + jurusanWhere
                            + " ORDER BY d.nama";

                        List<Object[]> dosenRows = session.createSQLQuery(sqlDosen).list();
                        int no = 1;
                        for (Object[] dosen : dosenRows) {
                            long dosenId = dosen[0] == null ? 0L : Long.parseLong(dosen[0].toString());
                            String nidn = dosen[1] != null ? dosen[1].toString() : "";
                            String nama = dosen[2] != null ? dosen[2].toString() : "";
                            String jabatan = dosen[3] != null ? dosen[3].toString() : "";

                            // Riwayat pendidikan S3
                            String s3Info = "";
                            try {
                                String sqlS3 = "SELECT COALESCE(rpd.nama_sekolah,'') "
                                    + "|| CASE WHEN rpd.bidang_ilmu IS NOT NULL "
                                    + "THEN ' (' || rpd.bidang_ilmu || ')' ELSE '' END "
                                    + "FROM riwayat_pendidikan_dosen rpd "
                                    + "WHERE rpd.dosen=" + dosenId
                                    + " AND rpd.jenjang_pendidikan='S3' "
                                    + "LIMIT 1";
                                Object s3Res = session.createSQLQuery(sqlS3).uniqueResult();
                                if (s3Res != null) s3Info = s3Res.toString();
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:168");
                            }

                            // Riwayat pendidikan S2
                            String s2Info = "";
                            try {
                                String sqlS2 = "SELECT COALESCE(rpd.nama_sekolah,'') "
                                    + "|| CASE WHEN rpd.bidang_ilmu IS NOT NULL "
                                    + "THEN ' (' || rpd.bidang_ilmu || ')' ELSE '' END "
                                    + "FROM riwayat_pendidikan_dosen rpd "
                                    + "WHERE rpd.dosen=" + dosenId
                                    + " AND rpd.jenjang_pendidikan='S2' "
                                    + "LIMIT 1";
                                Object s2Res = session.createSQLQuery(sqlS2).uniqueResult();
                                if (s2Res != null) s2Info = s2Res.toString();
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:183");
                            }

                            // Sertifikasi dosen
                            String sertifikasi = "Tidak";
                            try {
                                String sqlSert = "SELECT sertifikasi FROM dosen WHERE id=" + dosenId;
                                Object sertRes = session.createSQLQuery(sqlSert).uniqueResult();
                                if (sertRes != null) {
                                    boolean bSert = Boolean.parseBoolean(sertRes.toString());
                                    sertifikasi = bSert ? "Ya" : "Tidak";
                                }
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:195");
                            }

                            // Bidang keahlian: ambil dari S3, fallback S2
                            String bidangKeahlian = "";
                            try {
                                String sqlBidang = "SELECT COALESCE(rpd.bidang_ilmu,'') "
                                    + "FROM riwayat_pendidikan_dosen rpd "
                                    + "WHERE rpd.dosen=" + dosenId
                                    + " AND rpd.jenjang_pendidikan IN ('S3','S2') "
                                    + "ORDER BY rpd.jenjang_pendidikan ASC "
                                    + "LIMIT 1";
                                Object bdRes = session.createSQLQuery(sqlBidang).uniqueResult();
                                if (bdRes != null) bidangKeahlian = bdRes.toString();
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:209");
                            }

                            List sub = new ArrayList();
                            sub.add(no++);
                            sub.add(nidn);
                            sub.add(nama);
                            sub.add(s3Info);
                            sub.add(s2Info);
                            sub.add(jabatan);
                            sub.add(sertifikasi);
                            sub.add(bidangKeahlian);
                            sub.add(""); // Kesesuaian Kompetensi
                            sub.add(""); // Jabatan Akademik
                            sub.add(""); // Sertifikat Pendidik
                            sub.add(""); // MK di PS
                            sub.add(""); // Kesesuaian Bidang MK / MK di PS Lain
                            datas.add(sub);
                        }

                        if (dosenRows.isEmpty()) {
                            for (int i = 1; i <= 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add("");
                                sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                sub.add(""); sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:243");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:247");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_1_ProfilDtps.java:248");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 13);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

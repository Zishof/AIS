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
 * DKPS Tabel 3.4 — Kepuasan Mahasiswa terhadap Pelayanan Program Studi.
 *
 * <p>Menampilkan rekapitulasi hasil pengukuran kepuasan mahasiswa terhadap berbagai
 * aspek layanan yang diberikan oleh Program Studi (PS) dan Unit Pengelola Program
 * Studi (UPPS) dalam periode evaluasi akreditasi LAMDIK 2.0. Data dikelompokkan
 * berdasarkan aspek layanan dengan distribusi persentase responden pada skala penilaian:
 * Sangat Baik, Baik, Cukup, dan Kurang Baik, disertai rencana tindak lanjut.</p>
 *
 * <p>Instrumen ini merupakan bagian dari Kriteria 3 (Mahasiswa) akreditasi LAMDIK yang
 * menilai kualitas layanan dan kepuasan mahasiswa sebagai penerima layanan utama program
 * studi. Hasil survei kepuasan mahasiswa yang dianalisis secara berkelanjutan dan
 * ditindaklanjuti dengan perbaikan nyata menjadi bukti siklus penjaminan mutu yang
 * baik.</p>
 *
 * <p>Tujuh aspek pengukuran standar LAMDIK 2.0 meliputi: keandalan (reliability),
 * daya tanggap (responsiveness), kepastian (assurance), empati (empathy), keberwujudan
 * (tangible), ketersediaan dan kemudahan akses layanan akademik, serta kualitas layanan
 * administrasi dan keuangan.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code kepuasan_mahasiswa} yang dikelompokkan berdasarkan
 * kolom {@code aspek}. Jika tabel belum tersedia, ditampilkan tujuh baris template
 * dengan aspek-aspek standar sebagai panduan pengisian manual.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_3_4_KepuasanMahasiswa extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-3.4";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_3_4_KepuasanMahasiswa() {
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
    public LaporanDkps_3_4_KepuasanMahasiswa(String title, String border, boolean closable) throws Exception {
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
     * Memuat data kepuasan mahasiswa dari database di background thread.
     *
     * <p>Query mengambil distribusi persentase per aspek dari tabel
     * {@code kepuasan_mahasiswa}. Jika query gagal karena tabel belum ada,
     * ditampilkan tujuh baris template berisi aspek-aspek standar LAMDIK 2.0
     * dengan nilai persentase kosong sebagai panduan pengisian manual.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
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

                        String jurusanWhere = "";
                        if (selectedJurusan != null) {
                            jurusanWhere = " AND jurusan=" + selectedJurusan.getId();
                        }

                        boolean queryBerhasil = false;
                        try {
                            String sql = "SELECT aspek, "
                                + "ROUND(100.0 * SUM(CASE WHEN nilai=4 THEN 1 ELSE 0 END) / COUNT(*), 2) AS pct_sangat_baik, "
                                + "ROUND(100.0 * SUM(CASE WHEN nilai=3 THEN 1 ELSE 0 END) / COUNT(*), 2) AS pct_baik, "
                                + "ROUND(100.0 * SUM(CASE WHEN nilai=2 THEN 1 ELSE 0 END) / COUNT(*), 2) AS pct_cukup, "
                                + "ROUND(100.0 * SUM(CASE WHEN nilai=1 THEN 1 ELSE 0 END) / COUNT(*), 2) AS pct_kurang "
                                + "FROM kepuasan_mahasiswa "
                                + "WHERE 1=1"
                                + jurusanWhere
                                + " GROUP BY aspek ORDER BY aspek";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int no = 1;
                            for (Object[] r : rows) {
                                String aspek = r[0] != null ? r[0].toString() : "";
                                String pctSangatBaik = r[1] != null ? r[1].toString() : "0";
                                String pctBaik = r[2] != null ? r[2].toString() : "0";
                                String pctCukup = r[3] != null ? r[3].toString() : "0";
                                String pctKurang = r[4] != null ? r[4].toString() : "0";

                                List sub = new ArrayList();
                                sub.add(no++);
                                sub.add(aspek);
                                sub.add(pctSangatBaik);
                                sub.add(pctBaik);
                                sub.add(pctCukup);
                                sub.add(pctKurang);
                                sub.add("");
                                datas.add(sub);
                            }
                            queryBerhasil = true;
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_4_KepuasanMahasiswa.java:163");
                        }

                        if (!queryBerhasil) {
                            String[][] aspekTemplate = {
                                {"Keandalan (reliability): kemampuan dosen dan tenaga kependidikan dalam memberikan pelayanan"},
                                {"Daya tanggap (responsiveness): kecepatan dan ketepatan dalam merespons kebutuhan mahasiswa"},
                                {"Kepastian (assurance): jaminan layanan sesuai prosedur dan standar yang ditetapkan"},
                                {"Empati (empathy): perhatian dan kepedulian personal kepada mahasiswa"},
                                {"Keberwujudan (tangible): ketersediaan fasilitas fisik, peralatan, dan sarana pembelajaran"},
                                {"Ketersediaan dan kemudahan akses layanan akademik dan sistem informasi"},
                                {"Kualitas layanan administrasi akademik dan keuangan"}
                            };
                            for (int i = 0; i < aspekTemplate.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add(aspekTemplate[i][0]);
                                sub.add(""); sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_3_4_KepuasanMahasiswa.java:188");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_4_KepuasanMahasiswa.java:192");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_4_KepuasanMahasiswa.java:193");}
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

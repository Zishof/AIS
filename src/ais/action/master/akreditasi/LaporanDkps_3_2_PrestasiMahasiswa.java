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
 * DKPS Tabel 3.2 — Prestasi Akademik dan Non-Akademik Mahasiswa.
 *
 * <p>Menampilkan daftar prestasi yang diraih mahasiswa program studi dalam berbagai
 * kompetisi dan ajang ilmiah/non-ilmiah selama periode evaluasi akreditasi LAMDIK 2.0.
 * Data mencakup nama kegiatan, waktu perolehan prestasi, tingkat kompetisi
 * (lokal/nasional/internasional), dan prestasi yang dicapai (misalnya: Juara I, Medali
 * Emas, Best Paper, dan lain-lain).</p>
 *
 * <p>Instrumen ini termasuk dalam Kriteria 3 (Mahasiswa) akreditasi LAMDIK yang menilai
 * kualitas input dan proses pembinaan mahasiswa. Prestasi mahasiswa pada level internasional
 * dan nasional menjadi indikator unggul yang memberikan skor signifikan dalam penilaian.</p>
 *
 * <p>Kolom tingkat kompetisi direpresentasikan dalam tiga sub-kolom sesuai format LAMDIK
 * 2.0: Lokal/Wilayah/PT, Nasional, dan Internasional. Setiap baris diisi tanda centang
 * pada sub-kolom tingkat yang sesuai.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code prestasi_mahasiswa} yang di-JOIN dengan tabel
 * {@code mahasiswa} dan {@code jurusan}. Jika tabel belum tersedia, ditampilkan lima
 * baris template kosong sebagai panduan pengisian manual.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_3_2_PrestasiMahasiswa extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-3.2";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_3_2_PrestasiMahasiswa() {
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
    public LaporanDkps_3_2_PrestasiMahasiswa(String title, String border, boolean closable) throws Exception {
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
     * Memuat data prestasi mahasiswa dari database di background thread.
     *
     * <p>Query utama mengambil data dari tabel {@code prestasi_mahasiswa} dengan
     * JOIN ke tabel {@code mahasiswa} dan {@code jurusan}. Kolom tingkat dipetakan
     * ke tiga sub-kolom (Lokal/Nasional/Internasional). Jika query gagal karena
     * tabel belum ada, ditampilkan lima baris template kosong.</p>
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
                            jurusanWhere = " AND j.id=" + selectedJurusan.getId();
                        }

                        boolean queryBerhasil = false;
                        try {
                            String sql = "SELECT pm.nama_kegiatan, pm.waktu_perolehan, "
                                + "pm.tingkat, pm.prestasi "
                                + "FROM prestasi_mahasiswa pm "
                                + "INNER JOIN mahasiswa m ON pm.mahasiswa=m.id "
                                + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                + "WHERE 1=1"
                                + jurusanWhere
                                + " ORDER BY pm.waktu_perolehan DESC";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int no = 1;
                            for (Object[] r : rows) {
                                String namaKegiatan = r[0] != null ? r[0].toString() : "";
                                String waktu = r[1] != null ? r[1].toString() : "";
                                String tingkatStr = r[2] != null ? r[2].toString().toUpperCase() : "";
                                String lokal = (tingkatStr.contains("LOKAL") || tingkatStr.contains("WILAYAH")
                                    || tingkatStr.contains("PT")) ? "✓" : "";
                                String nasional = tingkatStr.contains("NASIONAL") ? "✓" : "";
                                String internasional = tingkatStr.contains("INTERNASIONAL") ? "✓" : "";
                                String prestasi = r[3] != null ? r[3].toString() : "";

                                List sub = new ArrayList();
                                sub.add(no++);
                                sub.add(namaKegiatan);
                                sub.add(waktu);
                                sub.add(lokal);
                                sub.add(nasional);
                                sub.add(internasional);
                                sub.add(prestasi);
                                datas.add(sub);
                            }
                            queryBerhasil = true;
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_2_PrestasiMahasiswa.java:162");
                        }

                        if (!queryBerhasil) {
                            for (int i = 1; i <= 5; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_3_2_PrestasiMahasiswa.java:178");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_2_PrestasiMahasiswa.java:182");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_2_PrestasiMahasiswa.java:183");}
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

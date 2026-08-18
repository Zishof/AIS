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
 * DKPS Tabel 6.1 — Struktur Kurikulum Program Studi.
 *
 * <p>Tabel ini menyajikan struktur kurikulum lengkap program studi, mencakup
 * seluruh mata kuliah yang termasuk dalam kurikulum aktif beserta informasi
 * kode mata kuliah, nama, jumlah SKS, semester penawaran, dan status
 * kewajiban (wajib atau pilihan). Data ini merupakan komponen inti dalam
 * penilaian akreditasi LAMDIK 2.0 untuk menilai ketepatan dan kelengkapan
 * struktur kurikulum yang ditawarkan program studi.</p>
 *
 * <p>Kurikulum program studi harus memenuhi standar jumlah total SKS yang
 * ditetapkan: minimal 144 SKS untuk program S1, minimal 36 SKS untuk S2,
 * dan minimal 42 SKS untuk S3. Proporsi mata kuliah wajib dan pilihan harus
 * seimbang sesuai dengan capaian pembelajaran lulusan (CPL) yang ditetapkan.
 * Distribusi mata kuliah per semester juga harus proporsional, idealnya 18-24
 * SKS per semester untuk program S1 reguler.</p>
 *
 * <p>Keselarasan antara mata kuliah dalam kurikulum dengan profil lulusan dan
 * Capaian Pembelajaran Lulusan (CPL) yang telah ditetapkan menjadi fokus
 * utama penilaian pada kriteria ini. Kurikulum yang dirancang berbasis Outcome
 * Based Education (OBE) dan telah melalui proses review oleh pemangku
 * kepentingan (stakeholder) eksternal akan mendapatkan nilai lebih tinggi.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code matakuliah} dengan join ke {@code jurusan}.
 * Filter program studi diterapkan berdasarkan pilihan Jurusan. Jika tidak ada
 * jurusan yang dipilih, data semua program studi ditampilkan. Hasil dibatasi
 * 100 baris untuk performa optimal.</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Kode Mata Kuliah</li>
 *   <li>Nama Mata Kuliah</li>
 *   <li>SKS</li>
 *   <li>Semester</li>
 *   <li>Status (Wajib / Pilihan)</li>
 *   <li>Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_1_StrukturKurikulum extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.1";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_1_StrukturKurikulum() {
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
    public LaporanDkps_6_1_StrukturKurikulum(String title, String border, boolean closable) throws Exception {
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
     * Memuat data struktur kurikulum dari tabel matakuliah di background thread.
     *
     * <p>Query mengambil semua mata kuliah pada jurusan terpilih beserta kode,
     * nama, SKS, semester, dan status wajib/pilihan. Diurutkan per semester
     * kemudian nama mata kuliah.</p>
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

                        String sql = "SELECT mk.kode, mk.nama, mk.sks, "
                            + "COALESCE(mk.semester::text, '-') as semester, "
                            + "CASE WHEN mk.wajib=true THEN 'Wajib' ELSE 'Pilihan' END as jenis "
                            + "FROM matakuliah mk INNER JOIN jurusan j ON mk.jurusan=j.id "
                            + "WHERE 1=1" + jurusanFilter
                            + " ORDER BY mk.semester, mk.nama"
                            + " LIMIT 100";

                        try {
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int noUrut = 1;
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(noUrut);                                        // No
                                sub.add(obj[0] != null ? obj[0].toString() : "");      // Kode MK
                                sub.add(obj[1] != null ? obj[1].toString() : "");      // Nama MK
                                sub.add(obj[2] != null ? obj[2].toString() : "");      // SKS
                                sub.add(obj[3] != null ? obj[3].toString() : "-");     // Semester
                                sub.add(obj[4] != null ? obj[4].toString() : "Wajib"); // Status
                                sub.add("");                                            // Keterangan
                                sub.add("");                                            // Asesmen CPL
                                sub.add("");                                            // Unit Penyelenggara
                                datas.add(sub);
                                noUrut++;
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_1_StrukturKurikulum.java:167");
                        }

                        if (datas.size() <= 5) {
                            for (int i = 1; i <= 5; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add("");
                                sub.add("-"); sub.add("Wajib"); sub.add("");
                                sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_1_StrukturKurikulum.java:184");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_1_StrukturKurikulum.java:188");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_1_StrukturKurikulum.java:189");}
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

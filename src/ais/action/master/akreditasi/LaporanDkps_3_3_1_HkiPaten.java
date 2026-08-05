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
 * DKPS Tabel 3.3-1 — Karya Inovatif Mahasiswa: HKI (Paten dan Paten Sederhana).
 *
 * <p>Menampilkan daftar karya inovatif mahasiswa program studi yang telah mendapatkan
 * pelindungan Hak Kekayaan Intelektual (HKI) berupa Paten atau Paten Sederhana selama
 * periode evaluasi akreditasi LAMDIK 2.0. Data mencakup identitas mahasiswa (NIM dan
 * nama), judul karya inovatif/luaran penelitian, tahun perolehan paten, dan nomor
 * paten yang diterbitkan oleh DJKI Kemenkumham.</p>
 *
 * <p>Instrumen ini merupakan bagian dari Kriteria 3 (Mahasiswa) akreditasi LAMDIK yang
 * menilai kualitas luaran karya inovatif mahasiswa. Paten yang dihasilkan mahasiswa
 * menjadi indikator unggul yang menunjukkan kemampuan inovasi dan kreativitas dalam
 * konteks pengembangan ilmu dan teknologi.</p>
 *
 * <p>Paten Sederhana (utility model) juga termasuk dalam cakupan instrumen ini karena
 * keduanya merupakan bentuk pelindungan kekayaan intelektual atas invensi yang memiliki
 * kebaruan dan dapat diterapkan secara industri.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code hki_mahasiswa} atau {@code karya_mahasiswa} dengan
 * filter {@code jenis LIKE '%PATEN%'}. Jika tabel belum tersedia, ditampilkan tiga
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
public class LaporanDkps_3_3_1_HkiPaten extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-3.3-1";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_3_3_1_HkiPaten() {
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
    public LaporanDkps_3_3_1_HkiPaten(String title, String border, boolean closable) throws Exception {
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
     * Memuat data HKI paten mahasiswa dari database di background thread.
     *
     * <p>Query pertama mencoba tabel {@code hki_mahasiswa}; jika gagal, dicoba tabel
     * {@code karya_mahasiswa}. Filter {@code jenis LIKE '%PATEN%'} memastikan hanya
     * paten dan paten sederhana yang ditampilkan. Jika semua query gagal, ditampilkan
     * tiga baris template kosong.</p>
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

                        // Coba tabel hki_mahasiswa
                        try {
                            String sql = "SELECT m.nim, m.nama, h.judul, h.tahun, h.nomor "
                                + "FROM hki_mahasiswa h "
                                + "INNER JOIN mahasiswa m ON h.mahasiswa=m.id "
                                + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                + "WHERE UPPER(h.jenis) LIKE '%PATEN%'"
                                + jurusanWhere
                                + " ORDER BY h.tahun DESC, m.nama";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int no = 1;
                            for (Object[] r : rows) {
                                String nimNama = (r[0] != null ? r[0].toString() : "")
                                    + " - " + (r[1] != null ? r[1].toString() : "");
                                String judul = r[2] != null ? r[2].toString() : "";
                                String tahun = r[3] != null ? r[3].toString() : "";
                                String noPaten = r[4] != null ? r[4].toString() : "";

                                List sub = new ArrayList();
                                sub.add(no++);
                                sub.add(nimNama);
                                sub.add(judul);
                                sub.add(tahun);
                                sub.add(noPaten);
                                datas.add(sub);
                            }
                            queryBerhasil = true;
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_1_HkiPaten.java:159");
                        }

                        // Fallback ke tabel karya_mahasiswa
                        if (!queryBerhasil) {
                            try {
                                String sql = "SELECT m.nim, m.nama, k.judul, k.tahun, k.nomor "
                                    + "FROM karya_mahasiswa k "
                                    + "INNER JOIN mahasiswa m ON k.mahasiswa=m.id "
                                    + "INNER JOIN jurusan j ON m.jurusan=j.id "
                                    + "WHERE UPPER(k.jenis) LIKE '%PATEN%'"
                                    + jurusanWhere
                                    + " ORDER BY k.tahun DESC, m.nama";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int no = 1;
                                for (Object[] r : rows) {
                                    String nimNama = (r[0] != null ? r[0].toString() : "")
                                        + " - " + (r[1] != null ? r[1].toString() : "");
                                    String judul = r[2] != null ? r[2].toString() : "";
                                    String tahun = r[3] != null ? r[3].toString() : "";
                                    String noPaten = r[4] != null ? r[4].toString() : "";

                                    List sub = new ArrayList();
                                    sub.add(no++);
                                    sub.add(nimNama);
                                    sub.add(judul);
                                    sub.add(tahun);
                                    sub.add(noPaten);
                                    datas.add(sub);
                                }
                                queryBerhasil = true;
                            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_1_HkiPaten.java:190");
                            }
                        }

                        if (!queryBerhasil) {
                            for (int i = 1; i <= 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_3_3_1_HkiPaten.java:206");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_1_HkiPaten.java:210");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_3_3_1_HkiPaten.java:211");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 5);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

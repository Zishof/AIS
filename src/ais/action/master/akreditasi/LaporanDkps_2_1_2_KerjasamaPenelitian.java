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
 * DKPS Tabel 2.1-2 — Kerjasama Tridharma Bagian 2: Kerjasama Penelitian.
 *
 * <p>Menampilkan daftar kerjasama program studi dengan lembaga mitra dalam bidang
 * penelitian selama periode evaluasi akreditasi LAMDIK 2.0. Data mencakup identitas
 * lembaga mitra, tingkat kerjasama (lokal/nasional/internasional), judul kegiatan,
 * manfaat yang diperoleh program studi, serta periode berlakunya kerjasama.</p>
 *
 * <p>Instrumen ini merupakan bagian dari Kriteria 2 (Kerjasama) akreditasi LAMDIK
 * yang menilai kualitas dan kuantitas kerjasama program studi dalam kegiatan Tridharma
 * Perguruan Tinggi, khususnya pada ranah penelitian dan pengembangan ilmu pengetahuan.
 * Kerjasama penelitian yang melibatkan lembaga internasional dan nasional bereputasi
 * memberikan skor lebih tinggi pada penilaian.</p>
 *
 * <p>Kolom tingkat kerjasama direpresentasikan dalam tiga sub-kolom terpisah sesuai
 * format baku LAMDIK 2.0: Lokal (dalam provinsi), Nasional (antar provinsi/dalam negeri),
 * dan Internasional (melibatkan lembaga luar negeri).</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data diambil dari tabel {@code kerjasama} dengan filter {@code jenis='PENELITIAN'}.
 * Jika tabel belum tersedia, ditampilkan tiga baris template kosong sebagai panduan
 * pengisian manual.</p>
 *
 * <h3>Manajemen sesi</h3>
 * <p>Background thread menggunakan {@code currentNativeSession()} dengan
 * penutupan wajib di {@code finally}. Tidak menggunakan multi-catch (Java 1.7).</p>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_2_1_2_KerjasamaPenelitian extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-2.1-2";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_2_1_2_KerjasamaPenelitian() {
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
    public LaporanDkps_2_1_2_KerjasamaPenelitian(String title, String border, boolean closable) throws Exception {
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
     * Memuat data kerjasama penelitian dari database di background thread.
     *
     * <p>Query utama mengambil data dari tabel {@code kerjasama} dengan filter
     * {@code jenis='PENELITIAN'}. Kolom tingkat kerjasama dipetakan ke tiga
     * sub-kolom (Lokal/Nasional/Internasional). Jika query gagal karena tabel
     * belum ada, ditampilkan tiga baris template kosong.</p>
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
                            jurusanWhere = " AND k.jurusan=" + selectedJurusan.getId();
                        }

                        boolean queryBerhasil = false;
                        try {
                            String sql = "SELECT k.nama_mitra, k.tingkat, k.judul, k.manfaat, "
                                + "k.tanggal_awal, k.tanggal_akhir "
                                + "FROM kerjasama k "
                                + "WHERE k.jenis='PENELITIAN'"
                                + jurusanWhere
                                + " ORDER BY k.tanggal_awal DESC";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int no = 1;
                            for (Object[] r : rows) {
                                String namaMitra = r[0] != null ? r[0].toString() : "";
                                String tingkatStr = r[1] != null ? r[1].toString().toUpperCase() : "";
                                String lokal = "LOKAL".equals(tingkatStr) ? "✓" : "";
                                String nasional = "NASIONAL".equals(tingkatStr) ? "✓" : "";
                                String internasional = "INTERNASIONAL".equals(tingkatStr) ? "✓" : "";
                                String judul = r[2] != null ? r[2].toString() : "";
                                String manfaat = r[3] != null ? r[3].toString() : "";
                                String tglAwal = r[4] != null ? r[4].toString() : "";
                                String tglAkhir = r[5] != null ? r[5].toString() : "";

                                List sub = new ArrayList();
                                sub.add(no++);
                                sub.add(namaMitra);
                                sub.add(lokal);
                                sub.add(nasional);
                                sub.add(internasional);
                                sub.add(judul);
                                sub.add(manfaat);
                                sub.add(tglAwal);
                                sub.add(tglAkhir);
                                datas.add(sub);
                            }
                            queryBerhasil = true;
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_2_1_2_KerjasamaPenelitian.java:164");
                        }

                        if (!queryBerhasil) {
                            for (int i = 1; i <= 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                sub.add(""); sub.add(""); sub.add(""); sub.add("");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_2_1_2_KerjasamaPenelitian.java:180");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_2_1_2_KerjasamaPenelitian.java:184");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_2_1_2_KerjasamaPenelitian.java:185");}
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

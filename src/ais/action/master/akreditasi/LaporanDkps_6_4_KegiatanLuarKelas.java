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
 * DKPS Tabel 6.4 — Kegiatan Akademik di Luar Kelas Program Studi.
 *
 * <p>Tabel ini mendokumentasikan kegiatan akademik yang diselenggarakan oleh
 * program studi di luar perkuliahan formal dalam kelas, mencakup kuliah tamu,
 * kunjungan industri atau studi lapangan, seminar dan lokakarya akademik,
 * serta praktikum lapangan. Kegiatan-kegiatan ini memperkaya pengalaman
 * belajar mahasiswa melalui interaksi dengan praktisi, industri, dan
 * lingkungan nyata yang relevan dengan bidang keilmuan program studi.</p>
 *
 * <p>Kegiatan kuliah tamu menghadirkan narasumber eksternal (akademisi atau
 * praktisi) yang menyampaikan materi aktual di luar kurikulum formal.
 * Kunjungan industri memperkenalkan mahasiswa pada implementasi nyata ilmu
 * di dunia kerja. Seminar akademik melatih mahasiswa untuk mempresentasikan
 * dan mendiskusikan karya ilmiah. Praktikum lapangan mengintegrasikan
 * teori dengan pengalaman langsung di lingkungan nyata.</p>
 *
 * <p>Frekuensi dan kualitas kegiatan ini mencerminkan keaktifan program studi
 * dalam menciptakan lingkungan belajar yang dinamis dan kontekstual, yang
 * menjadi salah satu aspek penilaian dalam akreditasi LAMDIK 2.0.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data kegiatan luar kelas umumnya tidak tersimpan secara terstruktur di
 * database AIS, sehingga ditampilkan 5 baris template kosong sebagai panduan
 * pengisian manual oleh operator program studi berdasarkan dokumentasi
 * kegiatan yang ada (berita acara, foto, laporan kegiatan, dll.).</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Kegiatan</li>
 *   <li>Jenis (Kuliah Tamu/Kunjungan/Seminar/Praktikum Lapangan)</li>
 *   <li>Tahun</li>
 *   <li>Narasumber / Tujuan</li>
 *   <li>Jumlah Peserta</li>
 *   <li>Hasil / Luaran</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_4_KegiatanLuarKelas extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.4";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_4_KegiatanLuarKelas() {
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
    public LaporanDkps_6_4_KegiatanLuarKelas(String title, String border, boolean closable) throws Exception {
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
     * Menampilkan template baris kosong untuk kegiatan akademik di luar kelas.
     *
     * <p>Karena data kegiatan luar kelas umumnya tidak tersimpan secara terstruktur
     * di database, ditampilkan 5 baris template kosong sebagai panduan
     * pengisian manual oleh operator program studi.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data kegiatan luar kelas ..."));

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

                        // Template 5 baris kosong — data sangat jarang ada di DB
                        for (int i = 1; i <= 5; i++) {
                            List sub = new ArrayList();
                            sub.add(i);
                            sub.add(""); // Nama dan Tema Kegiatan
                            sub.add(""); // Dosen Pembimbing
                            sub.add(""); // Tanggal Kegiatan
                            sub.add(""); // Bukti Kegiatan
                            datas.add(sub);
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_4_KegiatanLuarKelas.java:147");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_4_KegiatanLuarKelas.java:151");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_4_KegiatanLuarKelas.java:152");}
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

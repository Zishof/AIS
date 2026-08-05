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
 * DKPS Tabel 4.4 — Pengembangan Dosen Tetap Program Studi (DTPS).
 *
 * <p>Tabel ini mendokumentasikan kegiatan pengembangan kompetensi dan profesionalisme
 * yang diikuti oleh Dosen Tetap Program Studi (DTPS) dalam tiga tahun terakhir.
 * Pengembangan dosen merupakan salah satu indikator penting dalam penilaian
 * akreditasi LAMDIK 2.0 karena mencerminkan komitmen institusi dan individu dosen
 * untuk terus meningkatkan kualitas pembelajaran dan riset.</p>
 *
 * <p>Jenis kegiatan pengembangan yang dimaksud meliputi: pelatihan pedagogik dan
 * andragogik (seperti Pekerti, AA, atau pelatihan metode pembelajaran inovatif),
 * workshop penelitian dan penulisan artikel ilmiah, pelatihan penggunaan teknologi
 * pembelajaran (e-learning, LMS, platform digital), seminar dan lokakarya
 * peningkatan keahlian bidang keilmuan, magang industri atau academic exchange,
 * serta program beasiswa studi lanjut (S3 atau post-doctoral).</p>
 *
 * <p>Durasi kegiatan dicatat dalam satuan jam efektif atau hari pelaksanaan.
 * Lembaga penyelenggara mencakup: perguruan tinggi lain, kementerian, lembaga
 * sertifikasi profesi, asosiasi ilmiah, maupun organisasi internasional.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data pengembangan dosen umumnya belum disimpan secara terstruktur di database
 * AIS sehingga tabel ini menampilkan 5 baris template kosong sebagai panduan
 * pengisian manual oleh operator program studi berdasarkan dokumen laporan
 * kegiatan, sertifikat, atau SK keikutsertaan yang dimiliki masing-masing dosen.</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Dosen</li>
 *   <li>Jenis Kegiatan</li>
 *   <li>Tahun</li>
 *   <li>Durasi (jam/hari)</li>
 *   <li>Lembaga Penyelenggara</li>
 *   <li>Bukti / Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_4_4_PengembanganDtps extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-4.4";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_4_4_PengembanganDtps() {
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
    public LaporanDkps_4_4_PengembanganDtps(String title, String border, boolean closable) throws Exception {
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
     * Menampilkan template baris kosong untuk pengembangan DTPS.
     *
     * <p>Karena data pengembangan dosen belum terstruktur di database, ditampilkan
     * 5 baris template kosong sebagai panduan pengisian manual.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
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

                        // Template: 5 baris kosong panduan pengisian
                        for (int i = 1; i <= 5; i++) {
                            List sub = new ArrayList();
                            sub.add(i);    // No
                            sub.add("");   // Nama Dosen
                            sub.add("");   // Jenis Kegiatan
                            sub.add("");   // Tahun
                            sub.add("");   // Durasi
                            sub.add("");   // Lembaga Penyelenggara
                            sub.add("");   // Bukti/Keterangan
                            datas.add(sub);
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_4_PengembanganDtps.java:147");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_4_PengembanganDtps.java:151");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_4_PengembanganDtps.java:152");}
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

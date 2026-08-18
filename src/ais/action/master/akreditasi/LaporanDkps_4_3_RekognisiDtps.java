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
 * DKPS Tabel 4.3 — Rekognisi Dosen Tetap Program Studi (DTPS).
 *
 * <p>Tabel ini menampilkan rekognisi atau penghargaan yang diterima oleh Dosen
 * Tetap Program Studi (DTPS) dalam kurun waktu tiga tahun terakhir sebelum
 * pengisian borang akreditasi LAMDIK 2.0. Rekognisi mencakup semua bentuk
 * pengakuan eksternal atas kontribusi dan prestasi akademik dosen, baik dalam
 * bidang pendidikan, penelitian, maupun pengabdian kepada masyarakat.</p>
 *
 * <p>Jenis rekognisi yang dimaksud antara lain: penghargaan dari asosiasi profesi,
 * pengakuan sebagai narasumber/pembicara kunci (keynote speaker) pada seminar
 * nasional maupun internasional, pengakuan sebagai editor atau reviewer jurnal
 * bereputasi, penghargaan inovasi dari lembaga pemerintah atau swasta,
 * penghargaan penelitian terbaik, serta bentuk rekognisi lain yang relevan
 * dengan bidang keilmuan program studi.</p>
 *
 * <p>Tingkat rekognisi dibedakan menjadi tiga: Lokal/Wilayah (lingkup daerah
 * atau perguruan tinggi), Nasional (lingkup seluruh Indonesia), dan
 * Internasional (lingkup lintas negara). Semakin tinggi tingkat rekognisi yang
 * diperoleh, semakin besar kontribusinya terhadap penilaian akreditasi pada
 * kriteria ini.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data rekognisi umumnya belum tersimpan secara terstruktur di database AIS
 * dan perlu diisi manual berdasarkan dokumen pendukung (SK penghargaan, sertifikat,
 * surat undangan sebagai narasumber, dll). Tabel ini menampilkan 5 baris template
 * kosong sebagai panduan pengisian oleh operator program studi.</p>
 *
 * <h3>Format baris (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Dosen</li>
 *   <li>Jenis Rekognisi</li>
 *   <li>Tahun</li>
 *   <li>Tingkat (Lokal / Nasional / Internasional)</li>
 *   <li>Penyelenggara / Pemberi</li>
 *   <li>Bukti / Keterangan</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_4_3_RekognisiDtps extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-4.3";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_4_3_RekognisiDtps() {
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
    public LaporanDkps_4_3_RekognisiDtps(String title, String border, boolean closable) throws Exception {
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
     * Menampilkan template baris kosong untuk rekognisi DTPS.
     *
     * <p>Karena data rekognisi belum terstruktur di database, ditampilkan
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
                            sub.add(i);           // No
                            sub.add("");          // Nama DTPS
                            sub.add("");          // Bidang Keahlian
                            sub.add("");          // Rekognisi/Prestasi
                            sub.add("");          // Jenis Rekognisi
                            sub.add("");          // Tahun
                            datas.add(sub);
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_4_3_RekognisiDtps.java:148");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_3_RekognisiDtps.java:152");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_4_3_RekognisiDtps.java:153");}
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

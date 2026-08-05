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
 * DKPS 2.0 Instrumen 7.5 — Karya Ilmiah DTPS yang Disitasi oleh Peneliti Lain.
 *
 * <p>Instrumen ini mendokumentasikan karya ilmiah yang ditulis oleh Dosen Tetap
 * Program Studi (DTPS) dan telah disitasi (dikutip) oleh peneliti lain. Sitasi
 * merupakan ukuran dampak akademik dari sebuah karya ilmiah — semakin banyak
 * karya dosen disitasi, semakin besar pengakuan komunitas ilmiah internasional
 * terhadap hasil riset program studi tersebut.</p>
 *
 * <p>DKPS 2.0 mensyaratkan data sitasi karya ilmiah DTPS dalam tiga tahun
 * terakhir. Data sitasi dapat bersumber dari Google Scholar, Scopus, atau
 * Web of Science (WoS). Program studi dengan dosen yang memiliki h-index tinggi
 * dan total sitasi besar menunjukkan produktivitas riset berkualitas tinggi dan
 * diakui secara internasional.</p>
 *
 * <p>Sumber data: tabel {@code karya_dosen} dengan kolom {@code jumlah_sitasi}
 * yang berisi jumlah kutipan dari basis data akademik. Filter diterapkan pada
 * {@code jumlah_sitasi > 0} agar hanya karya yang benar-benar disitasi yang
 * ditampilkan. Jika tabel belum tersedia, ditampilkan template panduan pengisian.</p>
 *
 * <h3>Format kolom (maxCols=6)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Dosen (DTPS)</li>
 *   <li>Judul Karya Ilmiah</li>
 *   <li>Jumlah Sitasi (Google Scholar/Scopus/WoS)</li>
 *   <li>Tahun Terbit</li>
 *   <li>Keterangan (sumber sitasi)</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_7_5_SitasiKaryaIlmiah extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-7.5";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_7_5_SitasiKaryaIlmiah() {
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
    public LaporanDkps_7_5_SitasiKaryaIlmiah(String title, String border, boolean closable) throws Exception {
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
     * Memuat data karya ilmiah DTPS yang disitasi di background thread.
     *
     * <p>Query mengambil daftar karya ilmiah yang memiliki jumlah sitasi lebih dari
     * nol, diurutkan dari yang paling banyak disitasi. Filter jurusan diterapkan
     * melalui join ke tabel dosen dan jurusan. Jika tabel karya_dosen belum
     * tersedia di database, ditampilkan template kosong sebagai panduan pengisian.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data sitasi karya ilmiah DTPS ..."));

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

                        boolean loaded = false;
                        try {
                            String jurusanFilter = selectedJurusan != null
                                ? " AND j.id = " + selectedJurusan.getId() : "";

                            String sql = "SELECT ROW_NUMBER() OVER() AS no,"
                                + " d.nama AS nama_dosen, kd.judul, kd.jumlah_sitasi,"
                                + " kd.tahun_terbit, kd.keterangan"
                                + " FROM karya_dosen kd"
                                + " INNER JOIN dosen d ON kd.dosen = d.id"
                                + " INNER JOIN jurusan j ON d.jurusan = j.id"
                                + " WHERE kd.jumlah_sitasi > 0"
                                + jurusanFilter
                                + " ORDER BY kd.jumlah_sitasi DESC, kd.tahun_terbit DESC"
                                + " LIMIT 50";

                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(obj[0] != null ? obj[0].toString() : "");   // No
                                sub.add(obj[1] != null ? obj[1].toString() : "");   // Nama Dosen
                                sub.add(obj[2] != null ? obj[2].toString() : "");   // Judul Artikel yang disitasi
                                sub.add(obj[3] != null ? obj[3].toString() : "0");  // Jumlah Sitasi
                                datas.add(sub);
                            }
                            loaded = rows != null && rows.size() > 0;
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_7_5_SitasiKaryaIlmiah.java:154");
                        }

                        if (!loaded) {
                            for (int i = 0; i < 3; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add("(nama dosen)");
                                sub.add("(judul artikel)");
                                sub.add("5");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_7_5_SitasiKaryaIlmiah.java:171");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_5_SitasiKaryaIlmiah.java:175");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_5_SitasiKaryaIlmiah.java:176");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 4);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

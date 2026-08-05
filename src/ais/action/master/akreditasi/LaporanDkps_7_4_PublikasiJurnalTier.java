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
 * DKPS 2.0 Instrumen 7.4 — Publikasi DTPS pada Jurnal Nasional dan Internasional per Tier.
 *
 * <p>Instrumen ini mendokumentasikan kualitas publikasi ilmiah Dosen Tetap Program
 * Studi (DTPS) berdasarkan tier/indexasi jurnal tempat artikel diterbitkan. Tier
 * jurnal mencerminkan reputasi dan dampak ilmiah dari publikasi tersebut dalam
 * komunitas akademik internasional maupun nasional.</p>
 *
 * <p>Dalam penilaian akreditasi LAMDIK 2.0, publikasi pada jurnal dengan tier lebih
 * tinggi mendapat nilai yang lebih tinggi: Scopus Q1 dan SINTA 1 merupakan tingkatan
 * tertinggi, sedangkan SINTA 5-6 berada di tingkatan terendah. Program studi yang
 * dosennya aktif mempublikasikan di jurnal Scopus Q1/Q2 atau SINTA 1-2 menunjukkan
 * kualitas riset yang diakui secara internasional.</p>
 *
 * <p>Sumber data: tabel {@code karya_dosen} dengan filter {@code jenis_karya='JURNAL'}
 * dan tiga tahun terakhir. Kolom {@code level_jurnal} menyimpan tier seperti
 * "Scopus Q1", "Scopus Q2", "SINTA 1", dll. Jika tabel belum tersedia, ditampilkan
 * template panduan pengisian manual.</p>
 *
 * <h3>Format kolom (maxCols=7)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Dosen (DTPS)</li>
 *   <li>Judul Artikel</li>
 *   <li>Nama Jurnal</li>
 *   <li>Tier (Scopus Q1/SINTA 1/dll)</li>
 *   <li>Tahun Terbit</li>
 *   <li>URL/DOI</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_7_4_PublikasiJurnalTier extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-7.4";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_7_4_PublikasiJurnalTier() {
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
    public LaporanDkps_7_4_PublikasiJurnalTier(String title, String border, boolean closable) throws Exception {
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
     * Memuat data publikasi jurnal DTPS per tier di background thread.
     *
     * <p>Query mengambil daftar artikel jurnal DTPS tiga tahun terakhir beserta
     * tier/indexasi jurnal (Scopus Q1-Q4, SINTA 1-6). Filter jurusan diterapkan
     * melalui join ke tabel dosen dan jurusan. Jika tabel karya_dosen belum
     * tersedia di database, ditampilkan template kosong berisi contoh tier.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data publikasi jurnal per tier ..."));

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

                            String sql = "SELECT ROW_NUMBER() OVER(ORDER BY d.nama, kd.tahun_terbit DESC) AS no,"
                                + " d.nama AS nama_dosen, kd.judul, kd.nama_jurnal,"
                                + " kd.level_jurnal, kd.tahun_terbit, kd.url"
                                + " FROM karya_dosen kd"
                                + " INNER JOIN dosen d ON kd.dosen = d.id"
                                + " INNER JOIN jurusan j ON d.jurusan = j.id"
                                + " WHERE kd.jenis_karya = 'JURNAL'"
                                + " AND EXTRACT(year FROM kd.tanggal_terbit)"
                                + " >= (EXTRACT(year FROM CURRENT_DATE) - 3)"
                                + jurusanFilter
                                + " ORDER BY kd.tahun_terbit DESC, d.nama"
                                + " LIMIT 100";

                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(obj[0] != null ? obj[0].toString() : "");  // No
                                sub.add(obj[1] != null ? obj[1].toString() : "");  // Nama Dosen
                                sub.add(obj[2] != null ? obj[2].toString() : "");  // Judul
                                sub.add(obj[3] != null ? obj[3].toString() : "");  // Nama Jurnal
                                sub.add(obj[4] != null ? obj[4].toString() : "");  // Tier
                                sub.add(obj[5] != null ? obj[5].toString() : "");  // Tahun
                                sub.add(obj[6] != null ? obj[6].toString() : "");  // URL/DOI
                                datas.add(sub);
                            }
                            loaded = rows != null && rows.size() > 0;
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_7_4_PublikasiJurnalTier.java:159");
                        }

                        if (!loaded) {
                            String[] tiers = {
                                "Scopus Q1", "Scopus Q2", "SINTA 1", "SINTA 2", "SINTA 3"
                            };
                            for (int i = 0; i < tiers.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add("(nama dosen)");
                                sub.add("(judul artikel)");
                                sub.add("(nama jurnal)");
                                sub.add(tiers[i]);
                                sub.add("(tahun)");
                                sub.add("(url/doi)");
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_7_4_PublikasiJurnalTier.java:182");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_4_PublikasiJurnalTier.java:186");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_4_PublikasiJurnalTier.java:187");}
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

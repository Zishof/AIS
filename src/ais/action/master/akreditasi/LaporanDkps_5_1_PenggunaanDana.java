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
 * DKPS Tabel 5.1 — Data Penggunaan Dana Program Studi.
 *
 * <p>Tabel ini menyajikan rekapitulasi penggunaan dana program studi selama tiga
 * tahun akademik terakhir (TS-2, TS-1, dan TS), dikelompokkan berdasarkan jenis
 * penggunaan dana. Data ini merupakan indikator penting dalam akreditasi LAMDIK
 * 2.0 untuk menilai kecukupan dan efisiensi pengelolaan keuangan program studi
 * dalam mendukung kegiatan Tri Dharma Perguruan Tinggi.</p>
 *
 * <p>Kategori penggunaan dana yang umum digunakan dalam instrumen ini meliputi:
 * (1) Pendidikan — biaya operasional pembelajaran langsung; (2) Penelitian —
 * pendanaan kegiatan riset dosen dan mahasiswa; (3) Pengabdian kepada Masyarakat
 * (PkM) — biaya kegiatan pengabdian yang dilakukan program studi; (4) Investasi
 * Sarana dan Prasarana — pembelian peralatan, buku, software, dan fasilitas;
 * (5) Operasional Umum — biaya administrasi, listrik, internet, dan pendukung
 * lainnya. Nilai dana disajikan dalam satuan juta rupiah.</p>
 *
 * <p>Rasio ideal pengunaan dana untuk pendidikan, penelitian, dan PkM sesuai
 * standar BAN-PT/LAMDIK minimal 50% dari total anggaran operasional, dengan
 * proporsi yang seimbang antara ketiga dharma tersebut. Program studi yang
 * menunjukkan peningkatan alokasi penelitian dan PkM dari tahun ke tahun akan
 * mendapatkan penilaian lebih baik.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code jenis_penggunaan_dana} dan
 * {@code dana_penggunaan}. Jika tabel tidak tersedia, ditampilkan template
 * dengan jenis penggunaan dana standar sebagai panduan pengisian manual.</p>
 *
 * <h3>Format baris (maxCols=6)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Jenis Penggunaan Dana</li>
 *   <li>Dana TS-2 (juta rupiah)</li>
 *   <li>Dana TS-1 (juta rupiah)</li>
 *   <li>Dana TS (juta rupiah)</li>
 *   <li>Rata-rata (juta rupiah)</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_5_1_PenggunaanDana extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-5.1";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_5_1_PenggunaanDana() {
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
    public LaporanDkps_5_1_PenggunaanDana(String title, String border, boolean closable) throws Exception {
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
     * Memuat data penggunaan dana dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code dana_penggunaan} dan
     * {@code jenis_penggunaan_dana}. Jika gagal, tampilkan template
     * dengan kategori dana standar LAMDIK.</p>
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

                        int tahunSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

                        boolean loaded = false;
                        try {
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='dana_penggunaan'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = "";
                                if (selectedJurusan != null) {
                                    jurusanFilter = " AND dp.jurusan = " + selectedJurusan.getId();
                                }
                                String sql = "SELECT jp.nama as jenis, "
                                    + "COALESCE((SELECT SUM(d.jumlah)/1000000.0 FROM dana_penggunaan d "
                                    + "WHERE d.jenis_penggunaan=jp.id "
                                    + "AND EXTRACT(year FROM d.tanggal)=" + (tahunSekarang - 2)
                                    + jurusanFilter + "), 0) as ts2, "
                                    + "COALESCE((SELECT SUM(d.jumlah)/1000000.0 FROM dana_penggunaan d "
                                    + "WHERE d.jenis_penggunaan=jp.id "
                                    + "AND EXTRACT(year FROM d.tanggal)=" + (tahunSekarang - 1)
                                    + jurusanFilter + "), 0) as ts1, "
                                    + "COALESCE((SELECT SUM(d.jumlah)/1000000.0 FROM dana_penggunaan d "
                                    + "WHERE d.jenis_penggunaan=jp.id "
                                    + "AND EXTRACT(year FROM d.tanggal)=" + tahunSekarang
                                    + jurusanFilter + "), 0) as ts "
                                    + "FROM jenis_penggunaan_dana jp ORDER BY jp.nama";

                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    double ts2 = obj[1] != null ? Double.parseDouble(obj[1].toString()) : 0;
                                    double ts1 = obj[2] != null ? Double.parseDouble(obj[2].toString()) : 0;
                                    double ts  = obj[3] != null ? Double.parseDouble(obj[3].toString()) : 0;
                                    double rata = Math.round((ts2 + ts1 + ts) / 3.0 * 100) / 100.0;
                                    List sub = new ArrayList();
                                    sub.add(noUrut);
                                    sub.add(obj[0] != null ? obj[0].toString() : "");
                                    sub.add(Math.round(ts2 * 100) / 100.0); // UPPS TS-2
                                    sub.add(Math.round(ts1 * 100) / 100.0); // UPPS TS-1
                                    sub.add(Math.round(ts  * 100) / 100.0); // UPPS TS
                                    sub.add(rata);                           // UPPS Rata-rata
                                    sub.add(0);                              // PS TS-2
                                    sub.add(0);                              // PS TS-1
                                    sub.add(0);                              // PS TS
                                    sub.add(0);                              // PS Rata-rata
                                    datas.add(sub);
                                    noUrut++;
                                }
                                loaded = rows != null && rows.size() > 0;
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_1_PenggunaanDana.java:187");
                        }

                        if (!loaded) {
                            // Template jenis penggunaan dana standar
                            String[] jenisDefault = {
                                "Pendidikan", "Penelitian", "Pengabdian kepada Masyarakat",
                                "Investasi Sarana Prasarana", "Operasional Umum"
                            };
                            for (int i = 0; i < jenisDefault.length; i++) {
                                List sub = new ArrayList();
                                sub.add(i + 1);
                                sub.add(jenisDefault[i]);
                                sub.add(0); sub.add(0); sub.add(0); sub.add(0);
                                sub.add(0); sub.add(0); sub.add(0); sub.add(0);
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_5_1_PenggunaanDana.java:209");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_1_PenggunaanDana.java:213");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_5_1_PenggunaanDana.java:214");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 10);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

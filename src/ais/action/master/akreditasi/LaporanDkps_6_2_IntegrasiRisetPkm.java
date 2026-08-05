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
 * DKPS Tabel 6.2 — Integrasi Penelitian dan PkM DTPS dalam Pembelajaran.
 *
 * <p>Tabel ini mendokumentasikan mata kuliah yang mengintegrasikan hasil
 * penelitian dan/atau kegiatan Pengabdian kepada Masyarakat (PkM) yang
 * dilakukan oleh Dosen Tetap Program Studi (DTPS) sebagai materi pembelajaran.
 * Integrasi riset-PkM ke dalam pembelajaran merupakan komponen esensial dalam
 * penilaian akreditasi LAMDIK 2.0 pada kriteria pendidikan.</p>
 *
 * <p>Bentuk integrasi yang dimaksud mencakup: penambahan materi baru berbasis
 * temuan riset (Tambahan Materi), pemberian penugasan yang terinspirasi dari
 * riset/PkM (Penugasan), pengembangan modul atau bahan ajar dari hasil riset
 * (Modul), serta penggunaan kasus nyata dari kegiatan PkM sebagai objek
 * pembelajaran (Studi Kasus).</p>
 *
 * <p>Integrasi ini menunjukkan bahwa proses pembelajaran tidak hanya bersumber
 * dari buku teks, melainkan diperkaya oleh kontribusi ilmiah dosen yang aktif
 * melakukan penelitian dan pengabdian, sehingga mahasiswa memperoleh
 * pengetahuan yang relevan dan mutakhir.</p>
 *
 * <h3>Sumber data</h3>
 * <p>Data dicoba diambil dari tabel {@code mk_integrasi_riset} dengan join
 * ke {@code dosen}, {@code matakuliah}, dan {@code jurusan}. Jika tidak
 * tersedia, ditampilkan 5 baris template kosong sebagai panduan pengisian
 * manual oleh operator program studi.</p>
 *
 * <h3>Format baris (maxCols=6)</h3>
 * <ol>
 *   <li>No. Urut</li>
 *   <li>Nama Dosen (DTPS)</li>
 *   <li>Kode/Nama MK yang Diintegrasikan</li>
 *   <li>Bentuk Integrasi</li>
 *   <li>Judul Riset/PkM yang Diintegrasikan</li>
 *   <li>Tahun</li>
 * </ol>
 *
 * @author AIS Development Team
 * @version 2.0
 * @see AkreditasiBaseWindow
 */
public class LaporanDkps_6_2_IntegrasiRisetPkm extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "DKPS-6.2";
    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Program Studi
     * lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanDkps_6_2_IntegrasiRisetPkm() {
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
    public LaporanDkps_6_2_IntegrasiRisetPkm(String title, String border, boolean closable) throws Exception {
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
     * Memuat data integrasi riset-PkM dalam pembelajaran dari database di background thread.
     *
     * <p>Mencoba query dari tabel {@code mk_integrasi_riset} dengan join ke
     * dosen, matakuliah, dan jurusan. Jika tidak ada data, ditampilkan 5
     * baris template kosong sebagai panduan pengisian manual.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil secara programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data integrasi riset dan PkM ..."));

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
                            String checkSql = "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_name='mk_integrasi_riset'";
                            Object cnt = session.createSQLQuery(checkSql).uniqueResult();
                            boolean tableExists = cnt != null && Long.parseLong(cnt.toString()) > 0;

                            if (tableExists) {
                                String jurusanFilter = "";
                                if (selectedJurusan != null) {
                                    jurusanFilter = " AND j.id = " + selectedJurusan.getId();
                                }
                                String sql = "SELECT d.nama AS nama_dosen,"
                                    + " COALESCE(mk.kode,'') || ' - ' || COALESCE(mk.nama,'') AS mk_nama,"
                                    + " mir.bentuk_integrasi,"
                                    + " mir.judul_riset_pkm,"
                                    + " mir.tahun"
                                    + " FROM mk_integrasi_riset mir"
                                    + " INNER JOIN dosen d ON mir.dosen = d.id"
                                    + " INNER JOIN matakuliah mk ON mir.matakuliah = mk.id"
                                    + " INNER JOIN jurusan j ON d.jurusan = j.id"
                                    + " WHERE 1=1" + jurusanFilter
                                    + " ORDER BY mir.tahun DESC, d.nama"
                                    + " LIMIT 50";
                                List<Object[]> rows = session.createSQLQuery(sql).list();
                                int noUrut = 1;
                                for (Object[] obj : rows) {
                                    List sub = new ArrayList();
                                    sub.add(noUrut);
                                    sub.add(obj[0] != null ? obj[0].toString() : "");
                                    sub.add(obj[1] != null ? obj[1].toString() : "");
                                    sub.add(obj[2] != null ? obj[2].toString() : "");
                                    sub.add(obj[3] != null ? obj[3].toString() : "");
                                    sub.add(obj[4] != null ? obj[4].toString() : "");
                                    sub.add(""); // Tahun TS-2
                                    sub.add(""); // Tahun TS-1
                                    sub.add(""); // Tahun TS / Tahun YYYY
                                    datas.add(sub);
                                    noUrut++;
                                }
                                loaded = rows != null && rows.size() > 0;
                            }
                        } catch (Exception exQuery) {
                            exQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(exQuery, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_2_IntegrasiRisetPkm.java:176");
                        }

                        if (!loaded) {
                            // Template 5 baris kosong panduan pengisian manual
                            for (int i = 1; i <= 5; i++) {
                                List sub = new ArrayList();
                                sub.add(i);
                                sub.add(""); // Nama DTPS
                                sub.add(""); // Judul Penelitian/PkM
                                sub.add(""); // Mata Kuliah
                                sub.add(""); // Bentuk Integrasi
                                sub.add(""); // Tahun TS-2
                                sub.add(""); // Tahun TS-1
                                sub.add(""); // Tahun TS
                                sub.add(""); // Tahun YYYY
                                datas.add(sub);
                            }
                        }

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_6_2_IntegrasiRisetPkm.java:199");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_2_IntegrasiRisetPkm.java:203");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_6_2_IntegrasiRisetPkm.java:204");}
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

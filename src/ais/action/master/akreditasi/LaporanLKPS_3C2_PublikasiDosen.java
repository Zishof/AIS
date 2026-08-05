package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;

/**
 * LKPS Tabel 3.C.2 — Publikasi Ilmiah Dosen Tetap Program Studi.
 *
 * <p>Menampilkan jumlah publikasi ilmiah dosen tetap program studi, dikelompokkan
 * berdasarkan jenis media publikasi (jurnal nasional, jurnal internasional,
 * prosiding, buku, dll.) selama tiga tahun terakhir (TS-2, TS-1, TS).</p>
 *
 * <p>Semakin banyak publikasi di jurnal internasional terindeks — terutama
 * Scopus atau Web of Science — semakin tinggi nilai komponen ini dalam penilaian
 * akreditasi BAN-PT.</p>
 *
 * <p>Data diambil dari tabel {@code epsbed.epsbed_publikasi_dosen} yang digabung
 * dengan {@code epsbed.epsbed_media_publikasi} untuk mendapatkan nama jenis media.
 * Hanya dosen dengan status tetap ({@code dosen.tetap = 1}) yang dihitung.</p>
 *
 * <h3>Format data baris (LKPS-3.C.2, dataStartRow=6)</h3>
 * <p>Setiap baris berisi: {@code [Jenis Media Publikasi, TS-2, TS-1, TS]} pada
 * posisi indeks 0-3 agar sesuai dengan header konfigurasi grid. Enam baris
 * kosong pertama dipertahankan untuk Excel template.</p>
 *
 * <h3>Perbaikan dari versi sebelumnya</h3>
 * <ul>
 *   <li>Kolom data dipindah ke posisi 0-3 (sebelumnya 3/6-8 → data tidak tampil di grid)</li>
 *   <li>Filter Fakultas ditambahkan dan dihubungkan dengan Program Studi</li>
 *   <li>Session {@code currentNativeSession} ditutup di blok {@code finally}</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 2.0
 */
public class LaporanLKPS_3C2_PublikasiDosen extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "LKPS-3.C.2";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Prodi dan Tahun
     * Akademik, lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanLKPS_3C2_PublikasiDosen() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
                    Common.getCurrentTahunAkademik());
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
    public LaporanLKPS_3C2_PublikasiDosen(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
                Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    /** @return kode sheet {@value #sheetCode} */
    @Override
    protected String getSheetCode() {
        return sheetCode;
    }

    /**
     * Membangun baris filter: Fakultas, Program Studi, dan Tahun Akademik.
     * Perubahan nilai filter otomatis memicu {@link #onCetak}.
     */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });
    }

    /**
     * Memuat data publikasi dosen dari database di background thread.
     *
     * <p>Query dikelompokkan per media publikasi. Setiap baris data dibangun
     * dengan format {@code [Jenis Media, TS-2, TS-1, TS]}. Publikasi dari
     * media yang tidak diketahui ({@code null}) ditampilkan sebagai
     * "Tidak Diketahui".</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                        session = HibernateUtil.currentNativeSession();

                        // 6 baris kosong sesuai dataStartRow konfigurasi LKPS-3.C.2
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 6; i++) {
                            datas.add(new ArrayList());
                        }

                        // JOIN dengan tabel dosen jurusan hanya jika filter Program Studi aktif
                        String jurusanJoin = selectedJurusan == null ? ""
                                : " INNER JOIN dosen d2 ON ep.dosen = d2.id AND d2.jurusan = "
                                  + selectedJurusan.getId();

                        String sql =
                            "SELECT COALESCE(mp.nama, 'Tidak Diketahui') AS jenis_media,"
                            + " sum(CASE WHEN ep.tahun_publikasi = " + (tahun - 2) + " THEN 1 ELSE 0 END) AS t2,"
                            + " sum(CASE WHEN ep.tahun_publikasi = " + (tahun - 1) + " THEN 1 ELSE 0 END) AS t1,"
                            + " sum(CASE WHEN ep.tahun_publikasi = " + tahun + " THEN 1 ELSE 0 END) AS t0"
                            + " FROM epsbed.epsbed_publikasi_dosen ep"
                            + " LEFT JOIN epsbed.epsbed_media_publikasi mp ON ep.media_publikasi = mp.id"
                            + " INNER JOIN dosen d ON ep.dosen = d.id AND d.tetap = 1"
                            + jurusanJoin
                            + " WHERE ep.tahun_publikasi BETWEEN " + (tahun - 2) + " AND " + tahun
                            + " GROUP BY mp.id, mp.nama ORDER BY mp.nama";

                        List<Object[]> rows = session.createSQLQuery(sql).list();
                        for (Object[] obj : rows) {
                            List sub = new ArrayList();
                            sub.add(obj[0] != null ? obj[0].toString().trim() : "Tidak Diketahui"); // Jenis Media
                            sub.add(obj[1] == null ? 0 : ((Number) obj[1]).intValue());              // TS-2
                            sub.add(obj[2] == null ? 0 : ((Number) obj[2]).intValue());              // TS-1
                            sub.add(obj[3] == null ? 0 : ((Number) obj[3]).intValue());              // TS
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanLKPS_3C2_PublikasiDosen.java:180");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_3C2_PublikasiDosen.java:184");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_3C2_PublikasiDosen.java:185");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 12);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

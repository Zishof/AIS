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
 * LKPS Tabel 1.A.3 — Penggunaan Dana Program Studi.
 *
 * <p>Menampilkan distribusi belanja program studi berdasarkan jenis kegiatan
 * (pendidikan, penelitian, pengabdian masyarakat, investasi, dll.) selama
 * tiga tahun terakhir (TS-2, TS-1, TS) dalam satuan juta rupiah.</p>
 *
 * <p>Data diambil dari tabel {@code temporary.dana_penggunaan__sapto} yang
 * dikelompokkan per jenis penggunaan sesuai borang BAN-PT LKPS S1. Informasi
 * ini menunjukkan seberapa besar proporsi dana yang digunakan untuk masing-masing
 * kegiatan — idealnya pendidikan mendapat porsi terbesar.</p>
 *
 * <h3>Format data baris (LKPS-1.A.3, dataStartRow=6)</h3>
 * <p>Setiap baris berisi: {@code [No, Jenis Penggunaan, TS-2, TS-1, TS]} pada
 * posisi indeks 0-4 agar sesuai dengan header konfigurasi grid. Enam baris
 * kosong pertama dipertahankan untuk sinkronisasi posisi baris Excel template.</p>
 *
 * <h3>Perbaikan dari versi sebelumnya</h3>
 * <ul>
 *   <li>Kolom data dipindah ke posisi 0-4 (sebelumnya 3/6-8 → data tidak tampil)</li>
 *   <li>Filter Fakultas ditambahkan dan dihubungkan dengan filter Program Studi</li>
 *   <li>Session {@code currentNativeSession} ditutup di blok {@code finally}</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 2.0
 */
public class LaporanLKPS_1A3_PenggunaanDana extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "LKPS-1.A.3";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Prodi dan Tahun
     * Akademik, lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanLKPS_1A3_PenggunaanDana() {
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
    public LaporanLKPS_1A3_PenggunaanDana(String title, String border, boolean closable) throws Exception {
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
     * Memuat data penggunaan dana dari database di background thread.
     *
     * <p>Setiap baris data dibangun dengan format {@code [No, Jenis, TS-2, TS-1, TS]}
     * agar cocok dengan header konfigurasi LKPS-1.A.3.</p>
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

                        // 6 baris kosong sesuai dataStartRow konfigurasi LKPS-1.A.3
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 6; i++) {
                            datas.add(new ArrayList());
                        }

                        String jurusanFilter = selectedJurusan == null ? ""
                                : " AND a.jurusan = " + selectedJurusan.getId();

                        String sql =
                            "SELECT max(b.nama) AS nama,"
                            + " sum(CASE WHEN a.tahun = " + (tahun - 2) + " THEN a.nilai ELSE 0 END) AS t2,"
                            + " sum(CASE WHEN a.tahun = " + (tahun - 1) + " THEN a.nilai ELSE 0 END) AS t1,"
                            + " sum(CASE WHEN a.tahun = " + tahun + " THEN a.nilai ELSE 0 END) AS t0"
                            + " FROM temporary.dana_penggunaan__sapto a"
                            + " INNER JOIN temporary.jenis_dana_penggunaan__sapto b"
                            + "   ON a.jenis_dana_penggunaan_sapto = b.id"
                            + " WHERE 1=1"
                            + jurusanFilter
                            + " GROUP BY b.id ORDER BY max(b.nama)";

                        List<Object[]> rows = session.createSQLQuery(sql).list();
                        int rowNo = 1;
                        for (Object[] obj : rows) {
                            List sub = new ArrayList();
                            sub.add(rowNo++);                                                              // No
                            sub.add(obj[0] != null ? obj[0].toString().trim() : "");                      // Jenis Penggunaan
                            sub.add(obj[1] == null ? 0.0 : Double.parseDouble(obj[1].toString().trim())); // TS-2
                            sub.add(obj[2] == null ? 0.0 : Double.parseDouble(obj[2].toString().trim())); // TS-1
                            sub.add(obj[3] == null ? 0.0 : Double.parseDouble(obj[3].toString().trim())); // TS
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanLKPS_1A3_PenggunaanDana.java:175");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_1A3_PenggunaanDana.java:179");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanLKPS_1A3_PenggunaanDana.java:180");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 16);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

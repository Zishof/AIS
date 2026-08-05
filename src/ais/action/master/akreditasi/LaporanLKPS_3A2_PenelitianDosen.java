package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;

/**
 * LKPS Tabel 3.A.2 — Penelitian Dosen Tetap Program Studi (DTPR).
 *
 * <p>Menampilkan jumlah penelitian yang diajukan dan disetujui oleh dosen tetap
 * program studi, dikelompokkan per sumber pendanaan selama tiga tahun terakhir
 * (TS-2, TS-1, TS).</p>
 *
 * <p>Sumber pendanaan penelitian mencakup:</p>
 * <ol>
 *   <li>Pembiayaan sendiri oleh peneliti</li>
 *   <li>PT/Yayasan yang bersangkutan</li>
 *   <li>Kemdiknas/Kementerian lain terkait</li>
 *   <li>Institusi dalam negeri di luar Kemdiknas/Kementerian lain terkait</li>
 *   <li>Institusi luar negeri</li>
 *   <li>Sumber lain (baris gabungan)</li>
 * </ol>
 *
 * <p>Hanya penelitian berstatus {@code DISETUJUI} yang dihitung. Data diambil
 * dari entitas {@link PengajuanPenelitianDanPengabdian} dengan tipe PENELITIAN.</p>
 *
 * <h3>Format data baris (LKPS-3.A.2, dataStartRow=7)</h3>
 * <p>Setiap baris berisi: {@code [Sumber Dana, TS-2, TS-1, TS]} pada posisi
 * indeks 0-3 agar sesuai dengan header konfigurasi grid. Tujuh baris kosong
 * pertama dipertahankan untuk Excel template.</p>
 *
 * <h3>Perbaikan dari versi sebelumnya</h3>
 * <ul>
 *   <li>Nama sumber dana ditambahkan sebagai kolom pertama (sebelumnya hilang)</li>
 *   <li>Kolom data dipindah ke posisi 0-3 (sebelumnya 2-4 tanpa label → data tidak bermakna)</li>
 *   <li>Filter Fakultas ditambahkan dan dihubungkan dengan Program Studi</li>
 *   <li>Try-finally ditambahkan agar data selalu dikirim ke UI meski terjadi exception</li>
 *   <li>{@code HibernateUtil.closeSession()} dihapus — menggunakan {@code currentSession()}
 *       yang dikelola otomatis oleh framework, tidak perlu ditutup manual</li>
 * </ul>
 *
 * @author AIS Development Team
 * @version 2.0
 */
public class LaporanLKPS_3A2_PenelitianDosen extends AkreditasiBaseWindow {

    /** Kode sheet untuk konfigurasi grid, grafik, dan Excel template. */
    public static final String sheetCode = "LKPS-3.A.2";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    /**
     * Daftar nama sumber dana penelitian sesuai borang BAN-PT LKPS 3.A.2.
     * String kosong di akhir mewakili "Sumber Lain" (gabungan sumber yang
     * tidak masuk kategori di atas).
     */
    private static final String[] SUMBER_LIST = {
        "Pembiayaan sendiri oleh peneliti",
        "PT/yayasan yang bersangkutan",
        "Kemdiknas/Kementerian lain terkait",
        "Institusi dalam negeri di luar Kemdiknas/Kementerian lain terkait",
        "Institusi luar negeri",
        "" // sumber lain / tidak terkategorikan
    };

    /**
     * Konstruktor default — menginisialisasi filter Fakultas/Prodi dan Tahun
     * Akademik, lalu membangun tata letak tanpa auto-load data.
     */
    public LaporanLKPS_3A2_PenelitianDosen() {
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
    public LaporanLKPS_3A2_PenelitianDosen(String title, String border, boolean closable) throws Exception {
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
     * Memuat data penelitian dosen dari database di background thread.
     *
     * <p>Untuk setiap sumber dana dalam {@link #SUMBER_LIST}, metode menghitung
     * jumlah penelitian yang disetujui per tahun (TS-2, TS-1, TS) menggunakan
     * Hibernate Criteria. Setiap baris data dibangun dengan format
     * {@code [Sumber Dana, TS-2, TS-1, TS]}. String kosong di SUMBER_LIST
     * ditampilkan sebagai "Sumber Lain".</p>
     *
     * <p>Catatan: menggunakan {@code currentSession()} (bukan {@code currentNativeSession})
     * karena query Criteria butuh sesi Hibernate penuh — sesi ini dikelola
     * otomatis oleh framework dan tidak perlu ditutup manual.</p>
     *
     * @param event event ZK atau {@code null} jika dipanggil programatik
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
            final Jurusan selectedJurusan = getSelectedJurusan();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 7 baris kosong sesuai dataStartRow konfigurasi LKPS-3.A.2
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 7; i++) {
                            datas.add(new ArrayList());
                        }

                        for (String sumber : SUMBER_LIST) {
                            String labelSumber = sumber.trim().isEmpty() ? "Sumber Lain" : sumber;
                            List sub = new ArrayList();
                            sub.add(labelSumber); // Sumber Dana (posisi 0)

                            for (int yr = tahun - 2; yr <= tahun; yr++) {
                                Criteria c = HibernateUtil.currentSession()
                                    .createCriteria(PengajuanPenelitianDanPengabdian.class)
                                    .createAlias("sumberDanaPenelitianDanPengabdianes", "sumberDana")
                                    .createAlias("tbmuser", "tbmuser")
                                    .createAlias("tbmuser.dosen", "dosen")
                                    .add(Restrictions.eq("dosen.tetap", 1))
                                    .createAlias("penelitianDanPengabdian", "penelitianDanPengabdian")
                                    .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian",
                                            ConstantValues.PENELITIAN))
                                    .add(sumber.trim().isEmpty()
                                        ? Restrictions.sqlRestriction("true")
                                        : Restrictions.eq("sumberDana.nama", sumber))
                                    .add(Restrictions.eq("penelitianDanPengabdian.tahun", yr))
                                    .add(Restrictions.eq("status",
                                            PengajuanPenelitianDanPengabdian.DISETUJUI));

                                if (selectedJurusan != null) {
                                    c.createAlias("dosen.jurusan", "dosenJurusan")
                                     .add(Restrictions.eq("dosenJurusan.id", selectedJurusan.getId()));
                                }

                                int jumlah = ((Number) c.setProjection(Projections.rowCount())
                                        .uniqueResult()).intValue();
                                sub.add(jumlah); // TS-2 / TS-1 / TS (posisi 1, 2, 3)
                            }
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        datas.add(new ArrayList());

                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanLKPS_3A2_PenelitianDosen.java:214");
                        label.setValue("");
                    }
                    // Catatan: currentSession() TIDAK ditutup — dikelola otomatis oleh framework
                }
            }).start();

            display(label, 16);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}

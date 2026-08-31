package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.TugasBelajarDosen;
import ais.ui.util.DataCriteriaWithColumn;

/**
 * Laporan SAPTO/borang akreditasi BAN-PT butir A.4.4 (kode sheet {@code A-4.4_PT}): rekap jumlah
 * dosen tetap yang mengikuti tugas belajar ({@link TugasBelajarDosen}) dalam 3 tahun akademik
 * terakhir (tahun ajaran terpilih dan dua tahun sebelumnya), dipecah per jenjang studi lanjut ke
 * dalam tiga baris — jenjang selain S2/S3/Sp-1/Sp-2, jenjang S2/Sp-1, dan jenjang S3/Sp-2 — dengan
 * kolom untuk masing-masing dari tiga tahun tersebut. Pengambilan data dijalankan asinkron di
 * thread terpisah agar antarmuka tidak terkunci selama kueri berjalan, hasilnya disimpan sebagai
 * atribut pada {@link Label} placeholder lalu dirender oleh {@link SaptoUtil#displayWorksheet}.
 * Setiap sel angka dapat diklik untuk mengunduh rincian data dosen yang menyusun angka tersebut
 * (nama, bidang, jenjang, negara tujuan, identitas kepegawaian lengkap) lewat
 * {@link Common#cetakDataCustomButton}.
 */
public class LaporanKegiatanDosen_A_4_4 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.4_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membangun jendela laporan dengan tahun ajaran berjalan terpilih otomatis pada combobox filter. */
    public LaporanKegiatanDosen_A_4_4() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun jendela laporan dengan judul/border/closable kustom; tahun ajaran berjalan terpilih otomatis. */
    public LaporanKegiatanDosen_A_4_4(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** @return kode sheet borang {@code "A-4.4_PT"}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter berisi combobox pilihan tahun ajaran; perubahan pilihan memicu cetak ulang otomatis. */
    @Override
    protected void buildFilters(Row row) {
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    /**
     * Menghitung dan menampilkan rekap tugas belajar dosen 3 tahun terakhir untuk tahun ajaran
     * yang dipilih. Kueri SQL native menghitung jumlah baris {@code tugas_belajar_dosen} per tahun
     * (tahun terpilih dan dua tahun sebelumnya) dalam tiga kelompok jenjang, dijalankan di thread
     * terpisah agar UI tetap responsif; hasil disimpan sebagai atribut {@code datas} pada
     * {@link Label} placeholder dan dirender lewat {@link SaptoUtil#displayWorksheet}. Sel data
     * yang diklik memicu unduhan rincian dosen penyusun angka tersebut.
     *
     * @param event event pemicu (perubahan combobox tahun ajaran), boleh {@code null}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (tahunAkademik == null) { label.setValue(""); return; }

            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 10; i++) datas.add(new ArrayList());

                    String sql =
                        "select " +
                        "sum(case when a.tahun=" + tahun + "-2 then 1 else 0 end) as t2," +
                        "sum(case when a.tahun=" + tahun + "-1 then 1 else 0 end) as t1," +
                        "sum(case when a.tahun=" + tahun + "-0 then 1 else 0 end) as t " +
                        "from tugas_belajar_dosen a inner join jenjang b on (a.jenjang=b.id) " +
                        "where b.nama not in ('S3','S2','Sp-1','Sp-2') " +
                        "union all " +
                        "select sum(case when a.tahun=" + tahun + "-2 then 1 else 0 end),sum(case when a.tahun=" + tahun + "-1 then 1 else 0 end),sum(case when a.tahun=" + tahun + "-0 then 1 else 0 end) " +
                        "from tugas_belajar_dosen a inner join jenjang b on (a.jenjang=b.id) where b.nama in ('S2','Sp-1') " +
                        "union all " +
                        "select sum(case when a.tahun=" + tahun + "-2 then 1 else 0 end),sum(case when a.tahun=" + tahun + "-1 then 1 else 0 end),sum(case when a.tahun=" + tahun + "-0 then 1 else 0 end) " +
                        "from tugas_belajar_dosen a inner join jenjang b on (a.jenjang=b.id) where b.nama in ('S3','Sp-2')";

                    List<Object[]> objs = session.createSQLQuery(sql).list();
                    for (Object[] obj : objs) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add("");
                        for (int c = 0; c < 3; c++) {
                            sub.add(obj[c] == null ? 0.0 : Double.parseDouble(obj[c].toString().trim()));
                        }
                        datas.add(sub);
                    }

                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent ev = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            TugasBelajarDosen.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    try {
                                        int x = ev.getColumn() - 3;
                                        int y = ev.getRow() - 10;
                                        Integer[] xx = {tahun - 2, tahun - 1, tahun, 0};
                                        Integer colX = xx[x];
                                        String[] jenjang = y == 1 ? new String[]{"S2","Strata 2","Sp-1"}
                                            : y == 2 ? new String[]{"S3","Strata 3","Sp-2"}
                                            : y == 3 ? null : new String[]{};

                                        Criteria criteria = HibernateUtil.currentSession()
                                            .createCriteria(TugasBelajarDosen.class)
                                            .add(colX == 0 ? Restrictions.sqlRestriction("true")
                                                : Restrictions.eq("tahun", colX))
                                            .createAlias("jenjang","jenjang")
                                            .add(jenjang == null ? Restrictions.sqlRestriction("true")
                                                : (jenjang.length == 0
                                                    ? Restrictions.not(Restrictions.in("jenjang.nama",
                                                        new String[]{"S3","Strata 3","Sp-2","S2","Strata 2","Sp-1"}))
                                                    : Restrictions.in("jenjang.nama", jenjang)));

                                        return new Object[]{criteria, new String[]{"nama","bidang","jenjang.nama",
                                            "negara.namaNegara","tahun","dosen.nidn","dosen.mycode","dosen.nama",
                                            "dosen.kelamin","dosen.tempatlahir","dosen.tanggallahir","dosen.ktp",
                                            "dosen.npwp","dosen.gelarDepan","dosen.gelarBelakang",
                                            "dosen.jabatanFungsionalDosen.nama","dosen.pendidikan.nama",
                                            "dosen.fakultas.nama","dosen.jurusan.nama","dosen.sertifikasi",
                                            "dosen.ikatanKerjaDosen.nama","dosen.statusKepegawaian.nama",
                                            "dosen.jenisPendidikDanTenagaKependidikan.nama","dosen.lembagaPengangkat.nama",
                                            "dosen.sumberGaji.nama","dosen.golonganPegawai.nama","agama.nama","statusPerkawinan"}};
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanKegiatanDosen_A_4_4.java:153"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 9, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

/**
 * Laporan SAPTO/borang akreditasi BAN-PT butir A.6.1.4 (kode sheet {@code A-6.1.4_PT}): rekap
 * perolehan dana institusi selama 3 tahun terakhir (tahun ajaran terpilih dan dua tahun
 * sebelumnya), dikelompokkan per kategori sumber dana — Mahasiswa, Perguruan Tinggi sendiri,
 * Yayasan, Kemristekdikti/Kementerian Lain, dan gabungan Lembaga luar Kemdiknas/luar negeri/Sumber
 * Lain. Data diambil dari tabel staging {@code temporary.dana_penerimaan__sapto} yang sudah
 * diklasifikasikan menurut {@code jenis_dana_penerimaan__sapto.sumberdana}, dijumlahkan per jenis
 * dana dalam tiap kategori sumber untuk masing-masing dari tiga tahun. Data dimuat asinkron di
 * thread terpisah lalu dirender lewat {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanDanaInstitusi_A_6_1_4 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.1.4_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membangun jendela laporan dengan tahun ajaran berjalan terpilih otomatis. */
    public LaporanDanaInstitusi_A_6_1_4() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun jendela laporan dengan judul/border/closable kustom; tahun ajaran berjalan terpilih otomatis. */
    public LaporanDanaInstitusi_A_6_1_4(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** @return kode sheet borang {@code "A-6.1.4_PT"}. */
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
     * Menghitung dan menampilkan rekap perolehan dana institusi 3 tahun terakhir per kategori
     * sumber dana untuk tahun ajaran yang dipilih. Data dimuat asinkron di thread terpisah dan
     * dirender lewat {@link SaptoUtil#displayWorksheet}.
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

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 10; i++) datas.add(new ArrayList());

                    String[] datasa = {
                        "'Mahasiswa'",
                        "'Perguruan Tinggi'",
                        "'Yayasan'",
                        "'Kemristekdikti / Kementerian Lain'",
                        "'Lembaga/institusi di luar Kemdiknas/Kementerian lain terkait','Lembaga/institusi luar negeri','Sumber Lain'"
                    };

                    for (String d : datasa) {
                        String sql =
                            "select max(b.nama) as nama," +
                            "sum(case when a.tahun=" + tahun + "-2 then a.nilai else 0 end) as t2," +
                            "sum(case when a.tahun=" + tahun + "-1 then a.nilai else 0 end) as t1," +
                            "sum(case when a.tahun=" + tahun + "-0 then a.nilai else 0 end) as t0 " +
                            "from temporary.dana_penerimaan__sapto a " +
                            "inner join temporary.jenis_dana_penerimaan__sapto b on (a.jenis_dana_penerimaan_sapto=b.id) " +
                            "where b.sumberdana in (" + d + ") group by b.id";

                        List<Object[]> objs = session.createSQLQuery(sql).list();
                        for (Object[] obj : objs) {
                            List sub = new ArrayList();
                            sub.add(""); sub.add(""); sub.add("");
                            sub.add(obj[0]);
                            sub.add(""); sub.add("");
                            sub.add(obj[1] == null ? 0.0 : Double.parseDouble(obj[1].toString().trim()));
                            sub.add(obj[2] == null ? 0.0 : Double.parseDouble(obj[2].toString().trim()));
                            sub.add(obj[3] == null ? 0.0 : Double.parseDouble(obj[3].toString().trim()));
                            datas.add(sub);
                        }
                        for (int i = objs.size(); i <= 4; i++) datas.add(new ArrayList());
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

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

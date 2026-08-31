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
import ais.database.model.sapto.JenisDanaPenerimaanSapto;
import ais.database.model.sapto.JenisDanaPenggunaanSapto;

/**
 * Laporan borang akreditasi BAN-PT SAPTO butir A-6.1.7 (penggunaan dana untuk kegiatan pengabdian
 * kepada masyarakat, dirinci per sumber dana: PT sendiri/yayasan, pemerintah, dalam negeri, luar
 * negeri) tingkat institusi. Menyusun rekap nilai penggunaan dana untuk 3 tahun akademik terakhir
 * (TS-2, TS-1, TS) per kategori sumber dana lewat query SQL native ke tabel sementara
 * {@code temporary.dana_penggunaan__sapto} yang telah disiapkan modul SAPTO, dijalankan pada
 * thread terpisah agar antarmuka tidak terkunci selama query berjalan, lalu hasilnya ditampilkan
 * lewat {@link SaptoUtil#displayWorksheet}. Kode sheet borang: {@link #sheetCode}.
 */
public class LaporanDanaInstitusi_A_6_1_7 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.1.7_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membangun jendela laporan dengan tahun akademik berjalan terpilih pada combobox filter, lalu langsung membangun kerangka laporan (header/filter). */
    public LaporanDanaInstitusi_A_6_1_7() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor varian dengan judul/border/closable eksplisit, dipakai saat jendela dibuat sebagai komponen tersemat (bukan dari konstruktor default). */
    public LaporanDanaInstitusi_A_6_1_7(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** Mengembalikan kode sheet borang BAN-PT yang ditangani jendela ini: {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter berisi combobox pilihan tahun akademik (readonly, dari daftar tahun ajaran); memilih ulang tahun akademik memicu {@link #onCetak} otomatis. */
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
     * Menyusun dan menampilkan worksheet A-6.1.7: untuk tahun akademik terpilih, menjalankan 4
     * query SQL native (satu per kategori sumber dana: PT sendiri/yayasan, pemerintah, dalam
     * negeri, luar negeri) yang menjumlahkan nilai penggunaan dana pengabdian pada TS-2/TS-1/TS
     * dari {@code temporary.dana_penggunaan__sapto}, dijalankan di thread terpisah agar UI tetap
     * responsif, lalu hasilnya dirender lewat {@link SaptoUtil#displayWorksheet}.
     *
     * @param event event pemicu (perubahan combobox tahun akademik), boleh {@code null}
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
                    for (int i = 0; i < 7; i++) datas.add(new ArrayList());

                    String[] sumberList = {
                        "'" + JenisDanaPenerimaanSapto.SUMBER_DANA_PT_SENDIRI + "','" + JenisDanaPenerimaanSapto.SUMBER_DANA_YAYASAN + "'",
                        "'" + JenisDanaPenerimaanSapto.SUMBER_DANA_PEMERINTAH + "'",
                        "'" + JenisDanaPenerimaanSapto.SUMBER_DANA_DALAM_NEGERI + "'",
                        "'" + JenisDanaPenerimaanSapto.SUMBER_DANA_LUAR_NEGERI + "'"
                    };

                    for (String sumber : sumberList) {
                        String sql =
                            "select " +
                            "sum(case when a.tahun=" + tahun + "-2 then a.nilai else 0 end) as t2," +
                            "sum(case when a.tahun=" + tahun + "-1 then a.nilai else 0 end) as t1," +
                            "sum(case when a.tahun=" + tahun + "-0 then a.nilai else 0 end) as t0 " +
                            "from temporary.dana_penggunaan__sapto a " +
                            "inner join temporary.jenis_dana_penggunaan__sapto b on (a.jenis_dana_penggunaan_sapto=b.id) " +
                            "where b.jenispenggunaan='" + JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN +
                            "' and a.sumberdana in (" + sumber + ")";

                        List<Object[]> objs = session.createSQLQuery(sql).list();
                        for (Object[] obj : objs) {
                            List sub = new ArrayList();
                            sub.add(""); sub.add(""); sub.add("");
                            sub.add(obj[0] == null ? 0.0 : Double.parseDouble(obj[0].toString().trim()));
                            sub.add(obj[1] == null ? 0.0 : Double.parseDouble(obj[1].toString().trim()));
                            sub.add(obj[2] == null ? 0.0 : Double.parseDouble(obj[2].toString().trim()));
                            datas.add(sub);
                        }
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

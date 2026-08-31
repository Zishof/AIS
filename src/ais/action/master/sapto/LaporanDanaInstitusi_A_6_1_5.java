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
import ais.database.model.sapto.JenisDanaPenggunaanSapto;

/**
 * Laporan borang akreditasi BAN-PT butir A-6.1.5 pada paket sapto: rekap perolehan dan
 * penggunaan dana institusi selama 3 tahun terakhir (tahun ajaran terpilih dan dua tahun
 * sebelumnya), dipecah per jenis penggunaan dana ({@link JenisDanaPenggunaanSapto} — pendidikan,
 * penelitian, pengabdian, investasi prasarana/sarana/SDM, dan lain-lain). Kelas ini adalah
 * window ZK yang dibangun di atas {@link SaptoBaseWindow}, mengikuti konvensi kode sheet
 * {@link #sheetCode}.
 *
 * <p>
 * Menampilkan filter combobox tahun ajaran ({@link #buildFilters(Row)}); memilih tahun ajaran
 * lain otomatis memicu {@link #onCetak(Event)} ulang. Data dihitung dari tabel sementara
 * {@code temporary.dana_penggunaan__sapto} (hasil ekstraksi/agregasi data akunting untuk
 * keperluan borang) lewat query SQL native per jenis penggunaan, dijalankan di thread terpisah
 * agar UI tidak terblokir.
 * </p>
 */
public class LaporanDanaInstitusi_A_6_1_5 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.1.5_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membangun window laporan dengan pilihan tahun ajaran berjalan sebagai default (dipanggil dari kode yang membuat instance tanpa parameter tambahan). */
    public LaporanDanaInstitusi_A_6_1_5() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun window laporan dengan judul, tipe border, dan status closable yang dapat diatur eksplisit, sekaligus memilih tahun ajaran berjalan sebagai default. */
    public LaporanDanaInstitusi_A_6_1_5(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter combobox tahun akademik (readonly, dipilih dari daftar) yang memicu cetak ulang otomatis saat berubah. */
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
     * Menjalankan query rekap dana per jenis penggunaan untuk 3 tahun (tahun ajaran terpilih
     * dan dua tahun sebelumnya) dan menampilkan hasilnya sebagai worksheet {@link #sheetCode}.
     * Query dijalankan di thread terpisah; hasil disisipkan ke {@code label} lewat atribut
     * {@code "datas"} yang dibaca {@link SaptoUtil#displayWorksheet}.
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

                    String[] jenisList = {
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENDIDIKAN,
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN,
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN,
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_PRASARANA,
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SARANA,
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SDM,
                        JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_LAIN_LAIN
                    };

                    for (String jenis : jenisList) {
                        String sql =
                            "select " +
                            "sum(case when a.tahun=" + tahun + "-2 then a.nilai else 0 end) as t2," +
                            "sum(case when a.tahun=" + tahun + "-1 then a.nilai else 0 end) as t1," +
                            "sum(case when a.tahun=" + tahun + "-0 then a.nilai else 0 end) as t0 " +
                            "from temporary.dana_penggunaan__sapto a " +
                            "inner join temporary.jenis_dana_penggunaan__sapto b on (a.jenis_dana_penggunaan_sapto=b.id) " +
                            "where b.jenispenggunaan='" + jenis + "'";

                        List<Object[]> objs = session.createSQLQuery(sql).list();
                        for (Object[] obj : objs) {
                            List sub = new ArrayList();
                            sub.add(""); sub.add(""); sub.add(""); sub.add(""); sub.add(""); sub.add("");
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

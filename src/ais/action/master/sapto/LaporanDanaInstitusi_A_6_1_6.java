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
 * Jendela laporan borang akreditasi BAN-PT (SAPTO) butir A-6.1.6_PT — Dana Penelitian Institusi:
 * menyajikan rekap dana penelitian per sumber dana (PT sendiri/yayasan, pemerintah, dalam negeri,
 * luar negeri) untuk tiga tahun berjalan (TS-2, TS-1, TS) berdasarkan tahun akademik yang dipilih.
 * Data diambil lewat SQL native langsung terhadap tabel {@code temporary.dana_penggunaan__sapto}
 * yang difilter pada jenis penggunaan "Penelitian" dan dikelompokkan per sumber dana; nilai tahun
 * disisipkan langsung ke string SQL (bukan parameter bind) namun berasal dari combobox tahun
 * akademik yang dibangun sendiri oleh {@link Common#generateTahunAjaran}, bukan input bebas
 * pengguna. Memperluas {@link SaptoBaseWindow} untuk kerangka layar cetak/ekspor borang SAPTO baku.
 */
public class LaporanDanaInstitusi_A_6_1_6 extends SaptoBaseWindow {

    /** Kode sheet/butir borang SAPTO yang diwakili laporan ini. */
    public static final String sheetCode = "A-6.1.6_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Konstruktor default; menyiapkan pilihan tahun akademik berjalan lalu membangun kerangka dasar layar. */
    public LaporanDanaInstitusi_A_6_1_6() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit, diteruskan ke {@link SaptoBaseWindow}. */
    public LaporanDanaInstitusi_A_6_1_6(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter berisi combobox tahun akademik; perubahan pilihan langsung memicu {@link #onCetak}. */
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
     * Menangani aksi cetak/tampilkan lembar kerja: mengosongkan konten, lalu di thread terpisah
     * menjalankan empat query agregat SQL native (satu per kelompok sumber dana) yang menjumlahkan
     * nilai dana penggunaan bertipe "Penelitian" pada tahun TS-2/TS-1/TS, menyusun hasilnya menjadi
     * baris tabel worksheet, lalu menampilkannya lewat {@link SaptoUtil#displayWorksheet}.
     *
     * @param event event ZK pemicu aksi cetak (juga dipanggil manual dengan {@code null} saat filter berubah)
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
                            "where b.jenispenggunaan='" + JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN +
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

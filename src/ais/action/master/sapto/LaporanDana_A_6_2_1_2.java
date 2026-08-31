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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.sapto.JenisDanaPenggunaanSapto;

/**
 * Jendela laporan borang akreditasi BAN-PT SAPTO butir <b>A-6.2.1.2</b>: rekapitulasi penggunaan
 * dana program studi selama 3 tahun akademik terakhir (TS-2, TS-1, TS), dipecah per jenis
 * penggunaan (pendidikan, penelitian, pengabdian masyarakat, investasi prasarana/sarana/SDM, dan
 * lain-lain — lihat {@link JenisDanaPenggunaanSapto}). Data diambil dari tabel staging
 * {@code temporary.dana_penggunaan__sapto} yang sudah disinkronkan sebelumnya, difilter opsional
 * per jurusan/program studi dan tahun akademik terpilih, dijalankan asinkron di thread terpisah
 * agar UI tidak terblokir, lalu ditampilkan lewat {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanDana_A_6_2_1_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.1.2";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Konstruktor default: menyiapkan filter fakultas/jurusan dan tahun akademik (default tahun akademik berjalan), lalu membangun kerangka jendela laporan. */
    public LaporanDana_A_6_2_1_2() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Seperti konstruktor default, dengan judul/border/closable jendela yang dapat disesuaikan. */
    public LaporanDana_A_6_2_1_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    /** Kode butir borang yang dipetakan ke jendela ini: {@value #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menyusun kontrol filter pada baris toolbar: filter fakultas/jurusan bawaan {@link SaptoBaseWindow}, ditambah pemilih tahun akademik yang memicu {@link #onCetak} saat diubah. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    /**
     * Menyusun data laporan A-6.2.1.2: untuk tiap jenis penggunaan dana, menjalankan SQL native
     * agregat atas {@code temporary.dana_penggunaan__sapto} yang menjumlahkan nilai per tahun
     * (TS-2/TS-1/TS) dan opsional difilter per jurusan terpilih, dijalankan di thread terpisah
     * agar UI tidak terblokir, lalu hasilnya ditampilkan lewat {@link SaptoUtil#displayWorksheet}.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
                ? null : (Jurusan) jurusan.getSelectedItem().getValue();
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
                        String jurusanFilter = selectedJurusan == null ? "" : " and a.jurusan=" + selectedJurusan.getId();
                        String sql =
                            "select " +
                            "sum(case when a.tahun=" + tahun + "-2 then a.nilai else 0 end) as t2," +
                            "sum(case when a.tahun=" + tahun + "-1 then a.nilai else 0 end) as t1," +
                            "sum(case when a.tahun=" + tahun + "-0 then a.nilai else 0 end) as t0 " +
                            "from temporary.dana_penggunaan__sapto a " +
                            "inner join temporary.jenis_dana_penggunaan__sapto b on (a.jenis_dana_penggunaan_sapto=b.id) " +
                            "where b.jenispenggunaan='" + jenis + "'" + jurusanFilter;

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

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

/**
 * Laporan borang akreditasi BAN-PT butir A-6.2.1.1 (perolehan dana per sumber): merekap dana
 * penerimaan dari tabel sementara {@code temporary.dana_penerimaan__sapto} untuk 3 tahun terakhir
 * (tahun-2 s.d. tahun berjalan), dikelompokkan menjadi 4 kategori sumber dana tetap: Perguruan
 * Tinggi, Yayasan, Kemristekdikti/Kementerian lain, dan Lembaga/institusi lain (dalam &amp; luar
 * negeri/sumber lain). Setiap kategori dijumlahkan per tahun lewat SQL native ber-{@code CASE WHEN},
 * opsional difilter jurusan. Mengikuti kerangka kerja laporan sapto ({@link SaptoBaseWindow});
 * subkelas ini menentukan {@code sheetCode} ({@code "A-6.2.1.1"}), filter fakultas/jurusan +
 * tahun akademik, dan pengisian data di {@link #onCetak}.
 */
public class LaporanDana_A_6_2_1_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.1.1";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membuat jendela laporan dengan filter fakultas/jurusan dan tahun akademik berjalan sebagai default, lalu membangun tata letak dasar. */
    public LaporanDana_A_6_2_1_1() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membuat jendela laporan dengan judul/border/closable kustom; setup sama seperti konstruktor default. */
    public LaporanDana_A_6_2_1_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter fakultas/jurusan serta dropdown tahun akademik ke baris filter; memicu {@link #onCetak} otomatis saat tahun diganti. */
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

    /** Menjalankan rekap SQL native dana penerimaan per kategori sumber dana untuk 3 tahun terakhir (di thread terpisah, difilter jurusan bila dipilih) dan menampilkannya sebagai worksheet A-6.2.1.1. */
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

                    String[] datasa = {
                        "'Perguruan Tinggi'", "'Yayasan'",
                        "'Kemristekdikti / Kementerian Lain'",
                        "'Lembaga/institusi di luar Kemdiknas/Kementerian lain terkait','Lembaga/institusi luar negeri','Sumber Lain'"
                    };

                    for (String d : datasa) {
                        String jurusanFilter = selectedJurusan == null ? "" : " and a.jurusan=" + selectedJurusan.getId();
                        String sql =
                            "select max(b.nama) as nama," +
                            "sum(case when a.tahun=" + tahun + "-2 then a.nilai else 0 end) as t2," +
                            "sum(case when a.tahun=" + tahun + "-1 then a.nilai else 0 end) as t1," +
                            "sum(case when a.tahun=" + tahun + "-0 then a.nilai else 0 end) as t0 " +
                            "from temporary.dana_penerimaan__sapto a " +
                            "inner join temporary.jenis_dana_penerimaan__sapto b on (a.jenis_dana_penerimaan_sapto=b.id) " +
                            "where b.sumberdana in (" + d + ")" + jurusanFilter + " group by b.id";

                        List<Object[]> objs = session.createSQLQuery(sql).list();
                        for (Object[] obj : objs) {
                            List sub = new ArrayList();
                            sub.add(""); sub.add(""); sub.add("");
                            sub.add(obj[0]); sub.add(""); sub.add("");
                            sub.add(obj[1] == null ? 0.0 : Double.parseDouble(obj[1].toString().trim()));
                            sub.add(obj[2] == null ? 0.0 : Double.parseDouble(obj[2].toString().trim()));
                            sub.add(obj[3] == null ? 0.0 : Double.parseDouble(obj[3].toString().trim()));
                            datas.add(sub);
                        }
                        for (int i = objs.size(); i <= 8; i++) datas.add(new ArrayList());
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

package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.SumberDanaPenelitianDanPengabdian;

/**
 * Laporan borang akreditasi BAN-PT butir A-6.2.2 (penelitian dosen tetap yang didanai): mendaftar
 * {@link PengajuanPenelitianDanPengabdian} berjenis penelitian ({@code TipePenelitianDanPengabdian
 * .PENELITIAN}) yang sudah disetujui, milik dosen tetap ({@code dosen.tetap == 1}), untuk penelitian
 * bertahun &gt;= tahun akademik terpilih dikurangi 3, opsional difilter jurusan. Sumber dana tiap
 * penelitian digabung dari relasi {@code sumberDanaPenelitianDanPengabdianes}, dan jumlah dana
 * dikonversi ke satuan juta rupiah. Mengikuti kerangka kerja laporan sapto
 * ({@link SaptoBaseWindow}); subkelas ini menentukan {@code sheetCode} ({@code "A-6.2.2"}), filter
 * fakultas/jurusan + tahun akademik, dan pengisian data di {@link #onCetak}.
 */
public class LaporanDana_A_6_2_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.2";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membuat jendela laporan dengan filter fakultas/jurusan dan tahun akademik berjalan sebagai default, lalu membangun tata letak dasar. */
    public LaporanDana_A_6_2_2() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membuat jendela laporan dengan judul/border/closable kustom; setup sama seperti konstruktor default. */
    public LaporanDana_A_6_2_2(String title, String border, boolean closable) throws Exception {
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

    /** Mengambil data penelitian dosen tetap yang disetujui dan didanai untuk 3 tahun terakhir dari tahun akademik terpilih (di thread terpisah, difilter jurusan bila dipilih) dan menampilkannya sebagai worksheet A-6.2.2. */
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
                    for (int i = 0; i < 10; i++) datas.add(new ArrayList());

                    List<PengajuanPenelitianDanPengabdian> list = session
                        .createCriteria(PengajuanPenelitianDanPengabdian.class)
                        .createAlias("sumberDanaPenelitianDanPengabdianes", "sumberDanaPenelitianDanPengabdianes")
                        .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                        .add(Restrictions.eq("dosen.tetap", 1))
                        .add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
                            : Restrictions.eq("dosen.jurusan", selectedJurusan))
                        .createAlias("penelitianDanPengabdian", "penelitianDanPengabdian")
                        .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENELITIAN))
                        .add(Restrictions.ge("penelitianDanPengabdian.tahun", tahun - 3))
                        .add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
                        .addOrder(Order.asc("penelitianDanPengabdian.tahun")).addOrder(Order.asc("id")).list();

                    for (PengajuanPenelitianDanPengabdian p : list) {
                        List sub = new ArrayList();
                        sub.add("");
                        sub.add(p.getPenelitianDanPengabdian().getTahun().toString());
                        sub.add(p.getJudul());

                        StringBuilder sumberDana = new StringBuilder();
                        for (SumberDanaPenelitianDanPengabdian s : p.getSumberDanaPenelitianDanPengabdianes()) {
                            if (sumberDana.length() > 0) sumberDana.append(", ");
                            sumberDana.append(s.getNama());
                        }
                        sub.add(sumberDana.toString());
                        sub.add(p.getJumlahDana() / 1000000.0);
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

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

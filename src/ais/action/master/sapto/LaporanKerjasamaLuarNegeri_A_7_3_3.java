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
import ais.database.model.KerjasamaAntarInstansi;

/**
 * Laporan borang akreditasi BAN-PT butir A-7.3.3 (kerjasama luar negeri): mendaftar seluruh
 * {@link KerjasamaAntarInstansi} dengan mitra di luar Indonesia ({@code negara != INDONESIA})
 * untuk 3 tahun terakhir dari tahun akademik yang dipilih (tahun, tahun-1, tahun-2), diurutkan
 * berdasarkan tanggal mulai kerjasama. Mengikuti kerangka kerja laporan sapto:
 * {@link SaptoBaseWindow} menyediakan tata letak dasar, subkelas ini hanya menentukan
 * {@code sheetCode} ({@code "A-7.3.3_PT"}), filter (dropdown tahun akademik), dan logika pengisian
 * data di {@link #onCetak}. Data diambil di thread terpisah agar UI tidak terblokir, lalu
 * ditampilkan lewat {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanKerjasamaLuarNegeri_A_7_3_3 extends SaptoBaseWindow {

    public static final String sheetCode = "A-7.3.3_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membuat jendela laporan dengan tahun akademik berjalan sebagai default dan langsung membangun tata letak dasar. */
    public LaporanKerjasamaLuarNegeri_A_7_3_3() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membuat jendela laporan dengan judul/border/closable kustom; setup sama seperti konstruktor default. */
    public LaporanKerjasamaLuarNegeri_A_7_3_3(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter dropdown tahun akademik ke baris filter; memicu {@link #onCetak} otomatis saat tahun diganti. */
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

    /** Mengambil data {@link KerjasamaAntarInstansi} luar negeri untuk 3 tahun terakhir dari tahun akademik terpilih (di thread terpisah) dan menampilkannya sebagai worksheet A-7.3.3. */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (tahunAkademik == null) { label.setValue(""); return; }

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    List<KerjasamaAntarInstansi> list = session
                        .createCriteria(KerjasamaAntarInstansi.class)
                        .add(Restrictions.ne("negara", ConstantValues.INDONESIA))
                        .add(Restrictions.in("tahun", new Integer[]{tahun, tahun - 1, tahun - 2}))
                        .addOrder(Order.asc("mulai")).list();

                    int i = 1;
                    for (KerjasamaAntarInstansi k : list) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(i);
                        sub.add(k.getNama());
                        sub.add(k.getJenisKerjasama() == null ? "" : k.getJenisKerjasama().getNama());
                        sub.add(k.getMulai() == null ? "" : Common.dateFormat1.get().format(k.getMulai()));
                        sub.add(k.getSampai() == null ? "" : Common.dateFormat1.get().format(k.getSampai()));
                        sub.add(k.getManfaat());
                        datas.add(sub);
                        i++;
                    }

                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 8);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

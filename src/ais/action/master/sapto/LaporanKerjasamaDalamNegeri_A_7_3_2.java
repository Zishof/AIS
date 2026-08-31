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
 * Laporan borang akreditasi BAN-PT sapto butir <b>A-7.3.2</b> (kerja sama dalam negeri):
 * menampilkan daftar {@link KerjasamaAntarInstansi} dengan {@code negara} = Indonesia untuk tahun
 * akademik terpilih beserta dua tahun sebelumnya, diurutkan berdasarkan tanggal mulai. Mengikuti
 * kerangka umum kelas {@code Laporan*_A_X_Y} pada paket ini: filter tahun akademik lewat
 * {@link Combobox}, muat data di thread terpisah, lalu tampilkan sebagai lembar kerja (worksheet)
 * lewat {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanKerjasamaDalamNegeri_A_7_3_2 extends SaptoBaseWindow {

    /** Kode lembar borang akreditasi yang dilaporkan kelas ini. */
    public static final String sheetCode = "A-7.3.2_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membuat jendela laporan dengan tahun akademik berjalan sebagai filter awal dan langsung membangun tampilan dasar. */
    public LaporanKerjasamaDalamNegeri_A_7_3_2() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /**
     * Membuat jendela laporan dengan judul, gaya border, dan status dapat-ditutup kustom.
     *
     * @param title    judul jendela
     * @param border   gaya border jendela
     * @param closable apakah jendela dapat ditutup pengguna
     */
    public LaporanKerjasamaDalamNegeri_A_7_3_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** @return {@link #sheetCode}, kode lembar borang akreditasi yang dilaporkan kelas ini. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menyusun baris filter tahun akademik; memilih ulang tahun akademik langsung mencetak ulang laporan. */
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
     * Memuat ulang laporan untuk tahun akademik terpilih pada {@link #tahunAjaran}: mengambil
     * data {@link KerjasamaAntarInstansi} kerja sama dalam negeri (tahun terpilih dan dua tahun
     * sebelumnya) secara asinkron di thread terpisah, lalu menampilkannya sebagai worksheet
     * lewat {@link SaptoUtil#displayWorksheet}. Tidak melakukan apa pun bila belum ada tahun
     * akademik terpilih.
     *
     * @param event event pemicu (boleh {@code null}, tidak dipakai)
     */
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
                        .add(Restrictions.eq("negara", ConstantValues.INDONESIA))
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

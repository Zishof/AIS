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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PrestasiMahasiswa;

/**
 * Laporan borang akreditasi BAN-PT butir A-3.1.11 (prestasi mahasiswa tingkat lokal/nasional/
 * internasional): untuk rentang tahun akademik terpilih (mulai s.d. selesai), menampilkan seluruh
 * {@link PrestasiMahasiswa} berstatus {@link PrestasiMahasiswa#DISETUJUI} pada rentang tersebut,
 * dengan kolom ceklis tingkat ditentukan dari nama kategori prestasi (mengandung "internasional" ->
 * kolom Internasional, mengandung "nasional" -> kolom Nasional, selain itu -> kolom lokal/wilayah).
 *
 * <p>
 * Data dimuat di thread terpisah dan dirender ke worksheet {@link #sheetCode} ("A-3.1.11") lewat
 * {@link SaptoUtil#displayWorksheet} (tanpa handler klik sel — laporan ini murni tampilan).
 * </p>
 */
public class LaporanPrestasiMahasiswa_A_3_1_11 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.1.11";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;
    private Combobox tahunAjaran1;

    /** Konstruktor default: menyiapkan filter rentang tahun akademik (default keduanya periode berjalan) lalu membangun kerangka jendela dan langsung memuat data. */
    public LaporanPrestasiMahasiswa_A_3_1_11() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            Common.selectComboItem(tahunAjaran1 = Common.generateTahunAjaran(tahunAjaran1), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit; kegagalan inisialisasi dilempar ke pemanggil. */
    public LaporanPrestasiMahasiswa_A_3_1_11(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        Common.selectComboItem(tahunAjaran1 = Common.generateTahunAjaran(tahunAjaran1), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** @return kode sheet template worksheet borang, {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter rentang tahun akademik (mulai s.d. selesai, keduanya wajib/readonly; memicu {@link #onCetak} otomatis saat berubah) ke baris toolbar filter. */
    @Override
    protected void buildFilters(Row row) {
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });

        row.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
        tahunAjaran1.setWidth("90%");
        tahunAjaran1.setReadonly(true);
        row.appendChild(tahunAjaran1);
        tahunAjaran1.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    /**
     * Handler cetak: memuat {@link PrestasiMahasiswa} disetujui dalam rentang tahun terpilih di
     * thread terpisah, menandai kolom tingkat (Internasional/Nasional/lokal) berdasarkan nama
     * kategori prestasinya, lalu menampilkannya lewat {@link SaptoUtil#displayWorksheet}.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final String tahunAkademik1 = (String) tahunAjaran1.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    int tahun1 = Integer.parseInt(tahunAkademik1.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    List<PrestasiMahasiswa> items = session.createCriteria(PrestasiMahasiswa.class)
                        .add(Restrictions.between("tahun", tahun, tahun1))
                        .add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI))
                        .addOrder(Order.asc("tanggal")).list();

                    int i = 1;
                    for (PrestasiMahasiswa p : items) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(i); sub.add(p.getNama()); sub.add(p.getTahun());
                        if (p.getKategoriPrestasiMahasiswa() != null && p.getKategoriPrestasiMahasiswa().getNama().trim().toLowerCase().contains("internasional")) {
                            sub.add(""); sub.add(""); sub.add("V");
                        } else if (p.getKategoriPrestasiMahasiswa() != null && p.getKategoriPrestasiMahasiswa().getNama().trim().toLowerCase().contains("nasional")) {
                            sub.add(""); sub.add("V"); sub.add("");
                        } else {
                            sub.add("V"); sub.add(""); sub.add("");
                        }
                        sub.add(p.getCapaian());
                        datas.add(sub);
                        i++;
                    }
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    HibernateUtil.closeSession();
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

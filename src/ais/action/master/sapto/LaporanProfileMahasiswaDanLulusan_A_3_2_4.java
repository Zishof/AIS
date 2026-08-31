package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoGenerator;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteriaWithColumn;

/**
 * Laporan borang akreditasi BAN-PT butir A-3.2.4 (profil mahasiswa dan lulusan lima tahun
 * terakhir): perhitungan datanya didelegasikan sepenuhnya ke
 * {@link SaptoGenerator#generateProfileMahasiswaDanLulusan_A_3_2_4} untuk tahun akademik terpilih
 * (rentang 5 tahun mundur dari tahun angkatan pada tahun akademik tersebut), dijalankan di thread
 * terpisah dan dirender ke worksheet {@link #sheetCode} ("A-3.2.4") lewat
 * {@link SaptoUtil#displayWorksheet}.
 *
 * <p>
 * Klik sel data memicu unduhan Excel: kolom kedua (indeks {@code x==1}) mengunduh mahasiswa yang
 * mengisi checklist penilaian alumni ({@link GrupChecklistPenilaianUmum#UNTUK_ALUMNI}) pada tahun
 * lulus kolom tersebut, kolom lain mengunduh seluruh {@link Mahasiswa} aktif dengan tahun lulus
 * yang sama — dipetakan lewat offset tetap (baris -6, kolom -3, lima kolom tahun mundur dari tahun
 * terpilih) mengikuti tata letak template worksheet.
 * </p>
 */
public class LaporanProfileMahasiswaDanLulusan_A_3_2_4 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.2.4";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Konstruktor default: menyiapkan filter tahun akademik (default periode berjalan) lalu membangun kerangka jendela dan langsung memuat data. */
    public LaporanProfileMahasiswaDanLulusan_A_3_2_4() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit; kegagalan inisialisasi dilempar ke pemanggil. */
    public LaporanProfileMahasiswaDanLulusan_A_3_2_4(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** @return kode sheet template worksheet borang, {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter tahun akademik (wajib, readonly; memicu {@link #onCetak} otomatis saat berubah) ke baris toolbar filter. */
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
     * Handler cetak: mendelegasikan perhitungan data ke
     * {@link SaptoGenerator#generateProfileMahasiswaDanLulusan_A_3_2_4} di thread terpisah untuk
     * tahun akademik terpilih, lalu menampilkannya lewat {@link SaptoUtil#displayWorksheet}. Klik
     * sel data memicu unduhan Excel mahasiswa/lulusan sesuai kolom tahun yang diklik.
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
                    for (int i = 0; i < 5; i++) datas.add(new ArrayList());
                    datas.add(new ArrayList());

                    SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_4(session, label, tahun, datas);

                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent ev = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            null, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    int x = ev.getColumn() - 3;
                                    int y = ev.getRow() - 6;
                                    Integer[] yy = {tahun-4, tahun-3, tahun-2, tahun-1, tahun};
                                    Integer colY = yy[y];
                                    if (x == 1) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(ChecklistHasilPenilaianUmum.class)
                                            .setProjection(Projections.groupProperty("mahasiswa"))
                                            .createAlias("mahasiswa","mahasiswa")
                                            .createAlias("checklistPenilaianUmum","checklistPenilaianUmum")
                                            .createAlias("checklistPenilaianUmum.grupChecklistPenilaianUmum","grupChecklistPenilaianUmum")
                                            .add(Restrictions.eq("grupChecklistPenilaianUmum.diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_ALUMNI))
                                            .add(Restrictions.eq("mahasiswa.tahunLulus", colY));
                                        return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                    } else {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("tahunLulus", colY));
                                        return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                    }
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[]{"","","","","",""}).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswaDanLulusan_A_3_2_4.java:123"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 9, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

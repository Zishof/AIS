package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyComboitemConfig;

/**
 * Laporan SAPTO/borang akreditasi BAN-PT butir A.3.2.2 (kode sheet {@code A-3.2.2}): rekap profil
 * kelulusan mahasiswa (jumlah lulusan, rata-rata masa studi/IPK — dihitung oleh
 * {@link SaptoGenerator#generateProfileMahasiswaDanLulusan_A_3_2_2}) selama 3 tahun terakhir
 * (tahun akademik terpilih dan dua tahun sebelumnya), disusun terpisah per jenjang pendidikan
 * (S3, S2, S1, D4, D3, D2, D1) dan difilter jenis semester (Ganjil/Genap, default sesuai semester
 * berjalan). Data dimuat asinkron di thread terpisah lalu dirender lewat
 * {@link SaptoUtil#displayWorksheet}; setiap sel dapat diklik untuk mengunduh rincian mahasiswa
 * lulus (NIM, nama, tahun angkatan, tahun lulus, jurusan, IPK) yang menyusun angka tersebut lewat
 * {@link Common#cetakDataCustomButton}.
 */
public class LaporanProfileMahasiswaDanLulusan_A_3_2_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.2.2";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;
    private Combobox searchJenisSemester;

    /** Membangun jendela laporan dengan tahun ajaran berjalan dan semester berjalan terpilih otomatis. */
    public LaporanProfileMahasiswaDanLulusan_A_3_2_2() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildSemesterCombobox();
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun jendela laporan dengan judul/border/closable kustom; tahun ajaran dan semester berjalan terpilih otomatis. */
    public LaporanProfileMahasiswaDanLulusan_A_3_2_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildSemesterCombobox();
        buildBase(true);
    }

    /** Menyiapkan combobox pilihan jenis semester (Ganjil/Genap), default sesuai semester yang sedang berjalan. */
    private void buildSemesterCombobox() {
        searchJenisSemester = new Combobox();
        org.zkoss.zul.Comboitem item = new org.zkoss.zul.Comboitem();
        item.setLabel(Perkuliahan.GANJIL); item.setValue(Perkuliahan.GANJIL);
        searchJenisSemester.appendChild(item);
        item = new MyComboitemConfig();
        item.setLabel(Perkuliahan.GENAP); item.setValue(Perkuliahan.GENAP);
        searchJenisSemester.appendChild(item);
        searchJenisSemester.setReadonly(true);
        Common.selectComboItem(searchJenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
    }

    /** @return kode sheet borang {@code "A-3.2.2"}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter tahun akademik dan semester; perubahan tiap filter memicu cetak ulang otomatis. */
    @Override
    protected void buildFilters(Row row) {
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });

        row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
        searchJenisSemester.setWidth("90%");
        row.appendChild(searchJenisSemester);
        searchJenisSemester.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    /**
     * Menghitung dan menampilkan rekap profil kelulusan 3 tahun terakhir per jenjang untuk tahun
     * akademik dan semester yang dipilih. Data dimuat asinkron di thread terpisah dan dirender
     * lewat {@link SaptoUtil#displayWorksheet}; sel dapat diklik untuk mengunduh rincian mahasiswa
     * lulus penyusun angka.
     *
     * @param event event pemicu (perubahan filter), boleh {@code null}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final String semester = (String) searchJenisSemester.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'S3','s3'", tahun, semester));
                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'S2','s2'", tahun, semester));
                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'S1','s1'", tahun, semester));
                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'D4','d4'", tahun, semester));
                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'D3','d3'", tahun, semester));
                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'D2','d2'", tahun, semester));
                    datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_2(session, label, "'D1','d1'", tahun, semester));

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
                                    int y = ev.getRow() - 9;
                                    Integer[] xx = {tahun-2, tahun-1, tahun, tahun-2, tahun-1, tahun};
                                    String[] jenjang;
                                    if (y == 0) jenjang = new String[]{"S3","Strata 3"};
                                    else if (y == 1) jenjang = new String[]{"S2","Strata 2"};
                                    else if (y == 2) jenjang = new String[]{"S1","Strata 1"};
                                    else if (y == 3) jenjang = new String[]{"D4"};
                                    else if (y == 4) jenjang = new String[]{"D3"};
                                    else if (y == 5) jenjang = new String[]{"D2"};
                                    else jenjang = new String[]{"D1"};
                                    Integer colX = xx[x];
                                    final String[] finalJenjang = jenjang;
                                    Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                        .add(Restrictions.eq("tahunLulus", colX))
                                        .createAlias("jurusan","jurusan")
                                        .createAlias("jurusan.jenjang","jenjang")
                                        .add(Restrictions.in("jenjang.nama", finalJenjang));
                                    return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[]{"","","","","",""}).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswaDanLulusan_A_3_2_2.java:151"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 9, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

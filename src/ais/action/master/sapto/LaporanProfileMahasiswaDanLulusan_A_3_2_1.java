package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Arrays;
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

import ais.action.master.MahasiswaAction;
import ais.action.master.sapto.util.SaptoGenerator;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.HistoryStatusMahasiswa;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanProfileMahasiswaDanLulusan_A_3_2_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.2.1";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    public LaporanProfileMahasiswaDanLulusan_A_3_2_1() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileMahasiswaDanLulusan_A_3_2_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

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
                    List sub = new ArrayList();
                    sub.add(""); sub.add(""); sub.add(""); sub.add(tahun);
                    datas.add(sub);
                    for (int i = 0; i < 5; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 6; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'S1','s1'", 6, tahun, null));
                    }
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'S2','s2'", 4, tahun, null));
                    }
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 5; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'S3','s3'", 5, tahun, null));
                    }
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 6; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'D4','d4'", 6, tahun, null));
                    }
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'D3','d3'", 4, tahun, null));
                    }
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 2; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'D2','d2'", 2, tahun, null));
                    }
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 1; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan_A_3_2_1(session, label, rowIndex, "'D1','d1'", 1, tahun, null));
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
                                    int x = ev.getColumn() - 2;
                                    int y = ev.getRow() - 11;
                                    Integer[] yy = {tahun-6, tahun-5, tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0};
                                    String[] jenjang = {"S1","Strata 1"};

                                    if (y > 12 && y < 18) {
                                        jenjang = new String[]{"S2","Strata 2"};
                                        y = y - 13;
                                        yy = new Integer[]{tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0};
                                    } else if (y > 23 && y < 30) {
                                        jenjang = new String[]{"S3","Strata 3"};
                                        y = y - 24;
                                        yy = new Integer[]{tahun-5, tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0};
                                    } else if (y > 35 && y < 43) {
                                        jenjang = new String[]{"D4"};
                                        y = y - 36;
                                        yy = new Integer[]{tahun-6, tahun-5, tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0};
                                    } else if (y > 48 && y < 54) {
                                        jenjang = new String[]{"D3"};
                                        y = y - 49;
                                        yy = new Integer[]{tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0};
                                    } else if (y > 59 && y < 63) {
                                        jenjang = new String[]{"D2"};
                                        y = y - 60;
                                        yy = new Integer[]{tahun-2, tahun-1, tahun, 0};
                                    } else if (y > 68 && y < 71) {
                                        jenjang = new String[]{"D1"};
                                        y = y - 69;
                                        yy = new Integer[]{tahun-1, tahun, 0};
                                    }

                                    Integer colY = yy[y];
                                    Integer colX = yy[x];
                                    final String[] finalJenjang = jenjang;
                                    Criteria c = HibernateUtil.currentSession().createCriteria(HistoryStatusMahasiswa.class)
                                        .add(Restrictions.eq("statusMahasiswa", colX == 0 ? ConstantValues.LULUS : ConstantValues.AKTIF))
                                        .setProjection(Projections.groupProperty("mahasiswa"))
                                        .createAlias("mahasiswa","mahasiswa")
                                        .add(Restrictions.sqlRestriction("to_number(this_.tahunakademik,'9999') = " + (colX == 0 ? tahun : colX)))
                                        .add(Restrictions.or(Restrictions.isNull("mahasiswa.merupakanPindahan"), Restrictions.eq("mahasiswa.merupakanPindahan", false)))
                                        .add(Restrictions.eq("mahasiswa.tahunangkatan", colY))
                                        .createAlias("mahasiswa.jurusan","jurusan")
                                        .createAlias("jurusan.jenjang","jenjang")
                                        .add(Restrictions.in("jenjang.nama", finalJenjang));
                                    return new Object[]{c, MahasiswaAction.contents};
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswaDanLulusan_A_3_2_1.java:189"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 9, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

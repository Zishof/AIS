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
import ais.database.model.Jurusan;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanProfileMahasiswaDanLulusan_A_3_1_4 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.1.4";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    public LaporanProfileMahasiswaDanLulusan_A_3_1_4() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileMahasiswaDanLulusan_A_3_1_4(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

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

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (selectedJurusan != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                    	try {
                        Session session = HibernateUtil.currentNativeSession();
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 8; i++) datas.add(new ArrayList());

                        int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                        for (int rowIndex = tahun - 6; rowIndex <= tahun; rowIndex++) {
                            datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusanA_3_1_4(
                                session, label, rowIndex, 6, tahun, selectedJurusan, "Reguler"));
                        }
                        HibernateUtil.closeSession();
                        datas.add(new ArrayList()); datas.add(new ArrayList());
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add(""); sub.add(tahun);
                        datas.add(sub);
                        label.setAttribute("datas", datas);
                        label.setValue("");
                                        	} finally {
                    		ais.database.hibernate.HibernateUtil.closeSession();
                    	}
                    }
                }).start();
            } else {
                label.setValue("");
            }

            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);

            EventListener onCellClick = selectedJurusan == null ? null : new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent ev = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            null, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    int x = ev.getColumn() - 2;
                                    int y = ev.getRow() - 8;
                                    Integer[] yy = {tahun-6, tahun-5, tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0};
                                    Integer colY = yy[y];
                                    Integer colX = yy[x];
                                    Criteria c = HibernateUtil.currentSession().createCriteria(HistoryStatusMahasiswa.class)
                                        .add(Restrictions.eq("statusMahasiswa", colX == 0 ? ConstantValues.LULUS : ConstantValues.AKTIF))
                                        .setProjection(Projections.groupProperty("mahasiswa"))
                                        .createAlias("mahasiswa","mahasiswa")
                                        .add(Restrictions.eq("mahasiswa.program", "Reguler"))
                                        .add(Restrictions.sqlRestriction("to_number(this_.tahunakademik,'9999') = " + (colX == 0 ? tahun : colX)))
                                        .add(Restrictions.or(Restrictions.isNull("mahasiswa.merupakanPindahan"), Restrictions.eq("mahasiswa.merupakanPindahan", false)))
                                        .add(Restrictions.eq("mahasiswa.tahunangkatan", colY))
                                        .add(Restrictions.eq("mahasiswa.jurusan", selectedJurusan));
                                    return new Object[]{c, MahasiswaAction.contents};
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswaDanLulusan_A_3_1_4.java:139"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 11, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

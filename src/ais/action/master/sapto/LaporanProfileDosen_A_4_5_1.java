package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.Jurusan;

public class LaporanProfileDosen_A_4_5_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.5.1";
    private static final long serialVersionUID = 3331244819198611604L;
    public LaporanProfileDosen_A_4_5_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileDosen_A_4_5_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<FormulirKegiatan> formulirKegiatans = session.createCriteria(FormulirKegiatan.class)
                        .addOrder(Order.asc("mulai"))
                        .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                        .list();

                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 8; i++) datas.add(new ArrayList());

                    int rowIndexTotal = 1;
                    for (int rowIndex = 1; rowIndex <= formulirKegiatans.size(); rowIndex++) {
                        FormulirKegiatan fk = formulirKegiatans.get(rowIndex - 1);

                        if (fk.getJabatanPembicara1() != null && !fk.getJabatanPembicara1().isEmpty()) {
                            Integer tahun = null;
                            if (fk.getTanggal() != null) {
                                Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
                                cal.setTime(fk.getTanggal()); tahun = cal.get(Calendar.YEAR);
                            }
                            List sub = new ArrayList();
                            sub.add(""); sub.add(rowIndexTotal);
                            sub.add(fk.getNamaPembicara1()); sub.add(fk.getJabatanPembicara1());
                            sub.add(fk.getNama()); sub.add(tahun);
                            datas.add(sub); rowIndexTotal++;
                        }
                        if (fk.getJabatanPembicara2() != null && !fk.getJabatanPembicara2().isEmpty()) {
                            Integer tahun = null;
                            if (fk.getTanggal() != null) {
                                Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
                                cal.setTime(fk.getTanggal()); tahun = cal.get(Calendar.YEAR);
                            }
                            List sub = new ArrayList();
                            sub.add(""); sub.add(rowIndexTotal);
                            sub.add(fk.getNamaPembicara2()); sub.add(fk.getJabatanPembicara2());
                            sub.add(fk.getNama()); sub.add(tahun);
                            datas.add(sub); rowIndexTotal++;
                        }
                        if (fk.getJabatanPembicara3() != null && !fk.getJabatanPembicara3().isEmpty()) {
                            Integer tahun = null;
                            if (fk.getTanggal() != null) {
                                Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
                                cal.setTime(fk.getTanggal()); tahun = cal.get(Calendar.YEAR);
                            }
                            List sub = new ArrayList();
                            sub.add(""); sub.add(rowIndexTotal);
                            sub.add(fk.getNamaPembicara3()); sub.add(fk.getJabatanPembicara3());
                            sub.add(fk.getNama()); sub.add(tahun);
                            datas.add(sub); rowIndexTotal++;
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

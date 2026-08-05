package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.DosenAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;

public class LaporanProfileDosen_A_4_3_5 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.3.5";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;
    private Combobox semester;

    public LaporanProfileDosen_A_4_3_5() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildSemesterCombobox();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileDosen_A_4_3_5(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildSemesterCombobox();
        buildBase(false);
    }

    private void buildSemesterCombobox() {
        semester = new Combobox();
        org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem();
        ci.setLabel(Perkuliahan.GENAP); ci.setValue(Perkuliahan.GENAP);
        semester.appendChild(ci);
        ci = new ais.ui.util.MyComboitemConfig();
        ci.setLabel(Perkuliahan.GANJIL); ci.setValue(Perkuliahan.GANJIL);
        semester.appendChild(ci);
        ci = new ais.ui.util.MyComboitemConfig();
        ci.setLabel("Semua"); ci.setValue(null);
        semester.appendChild(ci);
        semester.setSelectedItem(ci);
        semester.setWidth("90%");
        semester.setReadonly(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });

        row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
        row.appendChild(semester);
        semester.addEventListener("onChange", new EventListener() {
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

            org.hibernate.Session sess = HibernateUtil.currentNativeSession();
            final List<Dosen> dosens = sess.createCriteria(Dosen.class)
                .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                .add(Restrictions.eq("tetap", 1)).addOrder(Order.asc("nama"))
                .add(Restrictions.eq("sesuaiBidangKeilmuan", false))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("nidn")).add(Restrictions.ne("nidn", "")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    org.hibernate.Session session = HibernateUtil.currentNativeSession();
                    int rowIndexTotal = 1;
                    for (int rowIndex = 1; rowIndex <= dosens.size(); rowIndex++) {
                        Dosen dosen = dosens.get(rowIndex - 1);

                        Criterion criterion = dosen == null ? Restrictions.sqlRestriction("false")
                            : Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

                        List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(criterion)
                            .add(selectedJurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", selectedJurusan))
                            .add(tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
                            .add(semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.sqlRestriction("this_.semester % 2 = "
                                    + (semester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))
                            .add(Restrictions.isNull("perkuliahan_paralel")).list();

                        for (Perkuliahan perkuliahan : perkuliahans) {
                            List sub = new ArrayList();
                            sub.add(""); sub.add(rowIndexTotal);
                            sub.add(dosen.getNama());
                            sub.add(perkuliahan.getMatakuliah().getKode());
                            sub.add(perkuliahan.getMatakuliah().getNama());
                            sub.add(perkuliahan.getMatakuliah().getSks());
                            sub.add(perkuliahan.getJumlahMaksimalPertemuan());
                            int jumlah = ((Number) session.createCriteria(Pertemuan.class)
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("perkuliahan", perkuliahan))
                                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                            sub.add(jumlah);
                            datas.add(sub);
                            rowIndexTotal++;
                        }
                    }
                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
                    for (int i = 0; i <= 150; i++) datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 9;
                        DosenAction.cetakDRHDosen(dosens.get(y));
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen_A_4_3_5.java:178"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

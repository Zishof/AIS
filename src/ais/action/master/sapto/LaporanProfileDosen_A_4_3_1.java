package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.DosenAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.RiwayatPendidikanDosen;

public class LaporanProfileDosen_A_4_3_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.3.1";
    private static final long serialVersionUID = 3331244819198611604L;
    public LaporanProfileDosen_A_4_3_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileDosen_A_4_3_1(String title, String border, boolean closable) throws Exception {
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

            Session session = HibernateUtil.currentNativeSession();
            final List<Dosen> dosens = session.createCriteria(Dosen.class)
                .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                .addOrder(Order.asc("nama")).add(Restrictions.eq("tetap", 1))
                .add(Restrictions.or(Restrictions.isNull("sesuaiBidangKeilmuan"), Restrictions.eq("sesuaiBidangKeilmuan", true)))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("nidn")).add(Restrictions.ne("nidn", "")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 13; i++) datas.add(new ArrayList());

                    Session session = HibernateUtil.currentNativeSession();
                    for (int rowIndex = 1; rowIndex <= dosens.size(); rowIndex++) {
                        Dosen dosen = dosens.get(rowIndex - 1);
                        RiwayatPendidikanDosen s1 = (RiwayatPendidikanDosen) session
                            .createCriteria(RiwayatPendidikanDosen.class).add(Restrictions.eq("dosen", dosen))
                            .add(Restrictions.eq("jenjangPendidikan", ConstantValues.s1)).setMaxResults(1).uniqueResult();
                        RiwayatPendidikanDosen s2 = (RiwayatPendidikanDosen) session
                            .createCriteria(RiwayatPendidikanDosen.class).add(Restrictions.eq("dosen", dosen))
                            .add(Restrictions.eq("jenjangPendidikan", ConstantValues.s2)).setMaxResults(1).uniqueResult();
                        RiwayatPendidikanDosen s3 = (RiwayatPendidikanDosen) session
                            .createCriteria(RiwayatPendidikanDosen.class).add(Restrictions.eq("dosen", dosen))
                            .add(Restrictions.eq("jenjangPendidikan", ConstantValues.s3)).setMaxResults(1).uniqueResult();

                        List sub = new ArrayList();
                        sub.add(""); sub.add(rowIndex);
                        sub.add(dosen.getNama()); sub.add(dosen.getNidn());
                        sub.add(dosen.getTanggallahir() == null ? "" : Common.dateFormat112.get().format(dosen.getTanggallahir()));
                        sub.add(dosen.getJabatanFungsionalDosen() == null ? "" : dosen.getJabatanFungsionalDosen().getNama());
                        sub.add(dosen.getSertifikasi() ? "Ya" : "Tidak");
                        sub.add(s1 == null ? "" : s1.getGelarAkademik());
                        sub.add(s1 == null ? "" : s1.getNamaSekolah());
                        sub.add(s1 == null ? "" : s1.getBidangIlmu());
                        sub.add(s2 == null ? "" : s2.getGelarAkademik());
                        sub.add(s2 == null ? "" : s2.getNamaSekolah());
                        sub.add(s2 == null ? "" : s2.getBidangIlmu());
                        sub.add(s3 == null ? "" : s3.getGelarAkademik());
                        sub.add(s3 == null ? "" : s3.getNamaSekolah());
                        sub.add(s3 == null ? "" : s3.getBidangIlmu());
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

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 13;
                        DosenAction.cetakDRHDosen(dosens.get(y));
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen_A_4_3_1.java:123"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

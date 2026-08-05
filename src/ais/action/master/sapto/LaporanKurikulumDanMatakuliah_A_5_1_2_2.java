package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.MatakuliahKurikulumDetailHelper;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyToolbarbuttonConfig;

public class LaporanKurikulumDanMatakuliah_A_5_1_2_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-5.1.2.2";
    private static final long serialVersionUID = 3331244819198611604L;
    private AmbilDataKurikulumBanbox kurikulum;

    public LaporanKurikulumDanMatakuliah_A_5_1_2_2() {
        super();
        try { buildBase(false); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanKurikulumDanMatakuliah_A_5_1_2_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum (harus dipilih)"));
        kurikulum = new AmbilDataKurikulumBanbox();
        kurikulum.setWidth("90%");
        kurikulum.setEventListener(new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
        row.appendChild(kurikulum);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Kurikulum selectedKurikulum = (Kurikulum) kurikulum.getAttribute("kurikulum");

            Session session = HibernateUtil.currentNativeSession();
            final List<KurikulumPunyaMatakuliah> items = session.createCriteria(KurikulumPunyaMatakuliah.class)
                .add(Restrictions.eq("kurikulum", selectedKurikulum))
                .createAlias("matakuliah","matakuliah").addOrder(Order.asc("semester"))
                .add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"), Restrictions.eq("matakuliah.aktif", true)))
                .addOrder(Order.asc("matakuliah.kode")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (selectedKurikulum != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                    	try {
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 12; i++) datas.add(new ArrayList());
                        Session s = HibernateUtil.currentNativeSession();

                        for (KurikulumPunyaMatakuliah kpm : items) {
                            ArrayList sub = new ArrayList();
                            sub.add("");
                            sub.add(kpm.getSemester() == null ? "" : kpm.getSemester().toString());
                            sub.add(kpm.getMatakuliah().getKode());
                            sub.add(kpm.getMatakuliah().getNama());
                            sub.add(kpm.getMatakuliah().getSks());
                            sub.add(kpm.getInti() ? "V" : "");
                            sub.add(kpm.getInstitusional() ? "V" : "");
                            sub.add(kpm.getTerdapatTugas() ? "V" : "");
                            sub.add(!kpm.getDeskripsiPembelajaran().isEmpty() ? "V" : "");
                            LampiranLain lam = LampiranLain.ambil(kpm.getId(), KurikulumPunyaMatakuliah.class.getName());
                            sub.add(lam != null ? "V" : "");
                            int count = ((Number) s.createCriteria(KurikulumPunyaMatakuliahDetail.class)
                                .add(Restrictions.eq("kurikulumPunyaMatakuliah", kpm))
                                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                            sub.add(count >= kpm.getJumlahPertemuanPerkuliahanDefault().intValue() ? "V" : "");
                            sub.add(kpm.getKurikulum().getJurusan().getNama());
                            datas.add(sub);
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
            } else {
                label.setValue("");
            }

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 12;
                        final KurikulumPunyaMatakuliah kpm = items.get(y);
                        final Window window = new Window("Data Rencana Pembelajaran Semester", "none", true);
                        window.setHeight("99%"); window.setWidth("90%");
                        window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
                        Borderlayout bl = new ais.ui.util.MyBorderlayout();
                        bl.setParent(window);
                        Center c = new Center(); c.setParent(bl);
                        ais.ui.util.ZkCompat.setFlex(c, true);
                        new MatakuliahKurikulumDetailHelper().display(kpm, null, c);
                        South south = new South();
                        ais.ui.util.ZkCompat.setFlex(south, true);
                        south.setParent(bl);
                        Toolbar toolbar = new Toolbar(); toolbar.setParent(south);
                        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
                        cancel.setTooltiptext("Tutup");
                        cancel.addEventListener("onClick", new EventListener() {
                            @Override public void onEvent(Event e) throws Exception { window.detach(); }
                        });
                        cancel.setParent(toolbar);
                        window.onModal();
                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

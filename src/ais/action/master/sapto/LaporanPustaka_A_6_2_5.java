package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.library.ItemAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.ItemPunyaBarcode;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanPustaka_A_6_2_5 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.5_PT";
    private static final long serialVersionUID = 3331244819198611604L;

    public LaporanPustaka_A_6_2_5() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanPustaka_A_6_2_5(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }
    @Override protected void buildFilters(Row row) { /* no filters */ }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 7; i++) datas.add(new ArrayList());

                    String[] tipeList = {"Textbook", "Jurnal Nasional yang terakreditasi", "Jurnal International", "Prosiding"};

                    for (String tipe : tipeList) {
                        int tidakDownload = ((Number) session.createCriteria(ItemPunyaBarcode.class)
                            .createAlias("item", "item").createAlias("item.tipeItem", "tipeItem")
                            .add(Restrictions.ilike("tipeItem.nama", tipe, MatchMode.EXACT))
                            .add(Restrictions.eq("item.bolehDiDownload", false))
                            .setProjection(Projections.countDistinct("item")).uniqueResult()).intValue();

                        int bisaDownload = ((Number) session.createCriteria(ItemPunyaBarcode.class)
                            .createAlias("item", "item").createAlias("item.tipeItem", "tipeItem")
                            .add(Restrictions.ilike("tipeItem.nama", tipe, MatchMode.EXACT))
                            .add(Restrictions.or(Restrictions.isNull("item.bolehDiDownload"),
                                Restrictions.eq("item.bolehDiDownload", true)))
                            .setProjection(Projections.countDistinct("item")).uniqueResult()).intValue();

                        int barcode = ((Number) session.createCriteria(ItemPunyaBarcode.class)
                            .createAlias("item", "item").createAlias("item.tipeItem", "tipeItem")
                            .add(Restrictions.ilike("tipeItem.nama", tipe, MatchMode.EXACT))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add(""); sub.add("");
                        sub.add(tidakDownload);
                        sub.add(bisaDownload);
                        sub.add(barcode);
                        datas.add(sub);
                    }

                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList());
                    for (int i = 0; i < 15; i++) datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            final String[] tipeArr = {"Textbook", "Jurnal Nasional yang terakreditasi", "Jurnal International", "Prosiding", ""};

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        final CellMouseEvent ev = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            null, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    try {
                                        int x = ev.getColumn() - 4;
                                        int y = ev.getRow() - 7;
                                        String colY = tipeArr[y];

                                        if (x == 2) {
                                            Criteria criteria = HibernateUtil.currentSession()
                                                .createCriteria(ItemPunyaBarcode.class)
                                                .addOrder(Order.asc("item"))
                                                .createAlias("item", "item").createAlias("item.tipeItem", "tipeItem")
                                                .add(colY.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                    : Restrictions.ilike("tipeItem.nama", colY, MatchMode.EXACT));
                                            return new Object[]{criteria, new String[]{"barcode","item.isbn","item.isbn10",
                                                "item.issn","item.nama","item.tipeItem.nama","item.pengarangs",
                                                "item.jenisItem.nama","item.penerbit.nama","item.penerbit.nama",
                                                "item.deweyDecimalClass","item.tahun","item.penaklikan","item.edisi",
                                                "item.penaklikan","item.jurusan.nama","item.fakultas.nama"}};
                                        } else {
                                            Criteria criteria = HibernateUtil.currentSession()
                                                .createCriteria(ItemPunyaBarcode.class)
                                                .setProjection(Projections.groupProperty("item"))
                                                .createAlias("item", "item").createAlias("item.tipeItem", "tipeItem")
                                                .add(colY.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                    : Restrictions.ilike("tipeItem.nama", colY, MatchMode.EXACT))
                                                .add(x == 1
                                                    ? Restrictions.or(Restrictions.isNull("item.bolehDiDownload"),
                                                        Restrictions.eq("item.bolehDiDownload", true))
                                                    : x == 0 ? Restrictions.eq("item.bolehDiDownload", false)
                                                    : Restrictions.sqlRestriction("true"));
                                            return new Object[]{criteria, ItemAction.contents};
                                        }
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanPustaka_A_6_2_5.java:146"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

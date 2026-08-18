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
import ais.database.model.Jurusan;
import ais.database.model.library.ItemPunyaBarcode;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanPustaka_A_6_4_1_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.4.1.1";
    private static final long serialVersionUID = 3331244819198611604L;
    private static final String[] TIPE_LIST = {
        "Textbook", "Jurnal Nasional yang terakreditasi", "Jurnal International",
        "Prosiding", "Skripsi", "Tesis", "Disertasi", ""
    };

    public LaporanPustaka_A_6_4_1_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanPustaka_A_6_4_1_1(String title, String border, boolean closable) throws Exception {
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
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
                ? null : (Jurusan) jurusan.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    for (String tipe : TIPE_LIST) {
                        int count = ((Number) session.createCriteria(ItemPunyaBarcode.class)
                            .createAlias("item", "item")
                            .add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
                                : Restrictions.eq("item.jurusan", selectedJurusan))
                            .createAlias("item.tipeItem", "tipeItem")
                            .add(Restrictions.ilike("tipeItem.nama", tipe, MatchMode.EXACT))
                            .setProjection(Projections.countDistinct("item")).uniqueResult()).intValue();

                        int barcode = ((Number) session.createCriteria(ItemPunyaBarcode.class)
                            .createAlias("item", "item")
                            .add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
                                : Restrictions.eq("item.jurusan", selectedJurusan))
                            .createAlias("item.tipeItem", "tipeItem")
                            .add(Restrictions.ilike("tipeItem.nama", tipe, MatchMode.EXACT))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add(""); sub.add("");
                        sub.add(count); sub.add(barcode);
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
                                        int y = ev.getRow() - 6;
                                        String colY = TIPE_LIST[y];
                                        if (x == 1) {
                                            Criteria criteria = HibernateUtil.currentSession()
                                                .createCriteria(ItemPunyaBarcode.class)
                                                .addOrder(Order.asc("item")).createAlias("item", "item")
                                                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
                                                    : Restrictions.eq("item.jurusan", selectedJurusan))
                                                .createAlias("item.tipeItem", "tipeItem")
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
                                                .createAlias("item", "item")
                                                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
                                                    : Restrictions.eq("item.jurusan", selectedJurusan))
                                                .createAlias("item.tipeItem", "tipeItem")
                                                .add(colY.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                    : Restrictions.ilike("tipeItem.nama", colY, MatchMode.EXACT));
                                            return new Object[]{criteria, ItemAction.contents};
                                        }
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanPustaka_A_6_4_1_1.java:152"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

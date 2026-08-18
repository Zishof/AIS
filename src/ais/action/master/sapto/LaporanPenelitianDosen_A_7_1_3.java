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

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanPenelitianDosen_A_7_1_3 extends SaptoBaseWindow {

    public static final String sheetCode = "A-7.1.3_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] JENIS_LIST = {
        "Jurnal ilmiah terakreditasi DIKTI", "Jurnal ilmiah internasional",
        "Buku tingkat nasional", "Buku tingkat internasional",
        "Karya seni tingkat nasional", "Karya seni tingkat internasional",
        "Karya sastra tingkat nasional", "Karya sastra tingkat internasional", ""
    };

    public LaporanPenelitianDosen_A_7_1_3() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanPenelitianDosen_A_7_1_3(String title, String border, boolean closable) throws Exception {
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
            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 7; i++) datas.add(new ArrayList());

                    for (String jenis : JENIS_LIST) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add("");
                        for (int yr = tahun - 2; yr <= tahun; yr++) {
                            int jumlah = ((Number) session.createCriteria(Artikel.class)
                                .createAlias("tingkatArtikeles", "tingkatArtikeles")
                                .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                                .add(Restrictions.eq("dosen.tetap", 1))
                                .add(Restrictions.eq("tingkatArtikeles.nama", jenis))
                                .add(Restrictions.eq("tahun", yr))
                                .add(Restrictions.eq("status", Artikel.DISETUJUI))
                                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                            sub.add(jumlah);
                        }
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
                            Artikel.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    try {
                                        int x = ev.getColumn() - 2;
                                        int y = ev.getRow() - 7;
                                        String colY = JENIS_LIST[y];
                                        Integer[] yy = {tahun - 2, tahun - 1, tahun, 0};
                                        Integer colX = yy[x];

                                        Criteria criteria = HibernateUtil.currentSession()
                                            .createCriteria(Artikel.class)
                                            .createAlias("tingkatArtikeles", "tingkatArtikeles")
                                            .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                                            .add(Restrictions.eq("dosen.tetap", 1))
                                            .add(colY.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                : Restrictions.eq("tingkatArtikeles.nama", colY))
                                            .add(colX == 0 ? Restrictions.sqlRestriction("true")
                                                : Restrictions.eq("tahun", colX))
                                            .add(Restrictions.eq("status", Artikel.DISETUJUI));

                                        return new Object[]{criteria, new String[]{"tbmuser.dosen.nidn","tbmuser.dosen.mycode",
                                            "tbmuser.dosen.nama","judul","keyword","tanggalPublikasi","tingkatArtikeles",
                                            "referensi","anggota","tahun","issn","eIssn","vol","nomor","licenseURL","pathUrl",
                                            "copyrightYear","copyrightHolder","sponsor","previewJurnal","plagiatChecker",
                                            "peerReview","jurnalPenelitian.judul","tahunAkademik","semester","bahasa","masaPenugasan"}};
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanPenelitianDosen_A_7_1_3.java:146"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

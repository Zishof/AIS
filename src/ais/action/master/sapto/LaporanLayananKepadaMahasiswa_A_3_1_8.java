package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import ais.database.model.GrupPertemuan;
import ais.database.model.JenisLayananKepadaMahasiswa;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanLayananKepadaMahasiswa_A_3_1_8 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.1.8";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;
    private Combobox tahunAjaran1;

    private static final String[] EMPTY_COLS = new String[54];
    static { java.util.Arrays.fill(EMPTY_COLS, ""); }

    public LaporanLayananKepadaMahasiswa_A_3_1_8() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            Common.selectComboItem(tahunAjaran1 = Common.generateTahunAjaran(tahunAjaran1), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanLayananKepadaMahasiswa_A_3_1_8(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        Common.selectComboItem(tahunAjaran1 = Common.generateTahunAjaran(tahunAjaran1), Common.getCurrentTahunAkademik());
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

        row.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
        tahunAjaran1.setWidth("90%");
        tahunAjaran1.setReadonly(true);
        row.appendChild(tahunAjaran1);
        tahunAjaran1.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final String tahunAkademik1 = (String) tahunAjaran1.getSelectedItem().getValue();

            final List<JenisLayananKepadaMahasiswa> jenisLayanans = HibernateUtil.currentSession()
                .createCriteria(JenisLayananKepadaMahasiswa.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nomorUrut")).list();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    int tahun1 = Integer.parseInt(tahunAkademik1.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 7; i++) datas.add(new ArrayList());

                    int i = 1;
                    for (JenisLayananKepadaMahasiswa jl : jenisLayanans) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(i); sub.add(jl.getNama()); sub.add(jl.getKeterangan());

                        int freq = ((Number) session.createCriteria(GrupPertemuan.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("jenisLayananKepadaMahasiswa", jl))
                            .add(Restrictions.between("tahun", tahun, tahun1))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                        sub.add(freq);

                        int jumlahMhs = ((Number) session.createCriteria(PertemuanPunyaGrupPertemuan.class)
                            .createAlias("grupPertemuan","grupPertemuan")
                            .add(Restrictions.or(Restrictions.isNull("grupPertemuan.aktif"), Restrictions.eq("grupPertemuan.aktif", true)))
                            .add(Restrictions.eq("grupPertemuan.jenisLayananKepadaMahasiswa", jl))
                            .add(Restrictions.between("grupPertemuan.tahun", tahun, tahun1))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                        sub.add(jumlahMhs);
                        datas.add(sub);
                        i++;
                    }
                    HibernateUtil.closeSession();
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
                            GrupPertemuan.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                                    int tahun1 = Integer.parseInt(tahunAkademik1.split("/")[0]);
                                    int y = ev.getRow() - 7;
                                    int x = ev.getColumn() - 4;
                                    JenisLayananKepadaMahasiswa jl = null;
                                    try { jl = jenisLayanans.get(y); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanLayananKepadaMahasiswa_A_3_1_8.java:142"); /* ignore */ }
                                    if (jl == null) return null;
                                    if (x == 0) {
                                        return new Object[]{
                                            HibernateUtil.currentSession().createCriteria(GrupPertemuan.class)
                                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                                .createAlias("jenisLayananKepadaMahasiswa","jenisLayananKepadaMahasiswa")
                                                .add(Restrictions.eq("jenisLayananKepadaMahasiswa", jl))
                                                .add(Restrictions.between("tahun", tahun, tahun1)),
                                            new String[]{"dosen.nama","nama","catatan","tanggal","waktuMulai","waktuSelesai","fakultas.nama","jurusan.nama","program","tahunAngkatan","ruang.nama","tahunAkademik","jenisSemester","kelas"}
                                        };
                                    } else if (x == 1) {
                                        return new Object[]{
                                            HibernateUtil.currentSession().createCriteria(PertemuanPunyaGrupPertemuan.class)
                                                .createAlias("grupPertemuan","grupPertemuan")
                                                .add(Restrictions.or(Restrictions.isNull("grupPertemuan.aktif"), Restrictions.eq("grupPertemuan.aktif", true)))
                                                .add(Restrictions.eq("grupPertemuan.jenisLayananKepadaMahasiswa", jl))
                                                .add(Restrictions.between("grupPertemuan.tahun", tahun, tahun1)),
                                            new String[]{"grupPertemuan.dosen.nama","grupPertemuan.nama","mahasiswa.nim","mahasiswa.nama","mahasiswa.jurusan.nama","mahasiswa.tahunangkatan"}
                                        };
                                    }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanLayananKepadaMahasiswa_A_3_1_8.java:168"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 6, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

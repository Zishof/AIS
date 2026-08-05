package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.MahasiswaAction;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.action.master.sapto.util.SaptoGenerator;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanProfileMahasiswa_A_3_1_5 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.1.5";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    public LaporanProfileMahasiswa_A_3_1_5() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileMahasiswa_A_3_1_5(String title, String border, boolean closable) throws Exception {
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
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());
                    List sub = new ArrayList();
                    sub.add(""); sub.add(""); sub.add(""); sub.add(tahun);
                    datas.add(sub);
                    for (int i = 0; i < 5; i++) datas.add(new ArrayList());

                    for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswa(session, label, rowIndex, "S1","S2","S3","s1","s2","s3"));
                    }
                    datas.add(new ArrayList()); datas.add(new ArrayList());

                    for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswa(session, label, rowIndex, "Profesi","Sp-1","Sp-2"));
                    }
                    datas.add(new ArrayList()); datas.add(new ArrayList());

                    for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
                        datas.add(SaptoGenerator.generateProfileMahasiswa(session, label, rowIndex, "D1","D2","D3","D4","d1","d2","d3","d4"));
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

            final String[] xx = {"S3-Strata 3","S2-Strata 2","S1-Strata 1","Sp-2","Sp-1","Profesi","D4","D3","D2","D1","Total","Total"};
            final int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
            final Integer[] yy = {tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0, -1,
                                   tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0, -1,
                                   tahun-4, tahun-3, tahun-2, tahun-1, tahun, 0, -1};

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
                                    int y = ev.getRow() - 12;
                                    Integer colY = yy[y];
                                    String colX = xx[x];
                                    String newTa = (colY) + "/" + (colY + 1);
                                    String[] jenjang = {"S1","S2","S3","s1","s2","s3","Strata 3","Strata 2","Strata 1"};
                                    Integer[] tahunAngkatan = {tahun-4, tahun-3, tahun-2, tahun-1, tahun};
                                    if (y > 6 && y < 13) {
                                        jenjang = new String[]{"Profesi","Sp-1","Sp-2"};
                                    } else if (y > 13) {
                                        jenjang = new String[]{"D1","D2","D3","D4","d1","d2","d3","d4"};
                                    }
                                    final String[] finalJenjang = jenjang;

                                    if (x == 0) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(KapasitasMahasiswaBaru.class)
                                            .createAlias("jurusan","jurusan").createAlias("jurusan.jenjang","jenjang")
                                            .add(Restrictions.in("jenjang.nama", finalJenjang))
                                            .add(colY == 0 ? Restrictions.in("tahun", tahunAngkatan) : Restrictions.eq("tahun", colY));
                                        return new Object[]{c, new String[]{"jurusan.nama","tahunAkademik","ganjilGenap","jumlahTargetMahasiswaBaru"}};
                                    } else if (x == 1) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .createAlias("prodi1","prodi1", Criteria.LEFT_JOIN).createAlias("prodi2","prodi2", Criteria.LEFT_JOIN)
                                            .createAlias("prodi3","prodi3", Criteria.LEFT_JOIN).createAlias("prodi4","prodi4", Criteria.LEFT_JOIN)
                                            .createAlias("prodi5","prodi5", Criteria.LEFT_JOIN)
                                            .createAlias("prodi1.jenjang","jenjang1", Criteria.LEFT_JOIN)
                                            .createAlias("prodi2.jenjang","jenjang2", Criteria.LEFT_JOIN)
                                            .createAlias("prodi3.jenjang","jenjang3", Criteria.LEFT_JOIN)
                                            .createAlias("prodi4.jenjang","jenjang4", Criteria.LEFT_JOIN)
                                            .createAlias("prodi5.jenjang","jenjang5", Criteria.LEFT_JOIN)
                                            .add(Restrictions.or(Restrictions.or(Restrictions.in("jenjang1.nama", finalJenjang), Restrictions.in("jenjang2.nama", finalJenjang)),
                                                 Restrictions.or(Restrictions.in("jenjang3.nama", finalJenjang), Restrictions.or(Restrictions.in("jenjang4.nama", finalJenjang), Restrictions.in("jenjang5.nama", finalJenjang)))))
                                            .add(colY == 0 ? Restrictions.in("tahun", tahunAngkatan) : Restrictions.eq("tahun", colY));
                                        return new Object[]{c, CetakRegistrasiAction.contents};
                                    } else if (x == 2) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .createAlias("prodiLulus","prodiLulus").createAlias("prodiLulus.jenjang","jenjang")
                                            .add(Restrictions.in("jenjang.nama", finalJenjang)).add(Restrictions.eq("tahunAkademik", newTa));
                                        return new Object[]{c, CetakRegistrasiAction.contents};
                                    } else if (x == 3) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .createAlias("jurusan","jurusan").createAlias("jurusan.jenjang","jenjang")
                                            .add(Restrictions.in("jenjang.nama", finalJenjang))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.eq("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 4) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .createAlias("jurusan","jurusan").createAlias("jurusan.jenjang","jenjang")
                                            .add(Restrictions.in("jenjang.nama", finalJenjang))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.eq("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 5) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .createAlias("jurusan","jurusan").createAlias("jurusan.jenjang","jenjang")
                                            .add(Restrictions.in("jenjang.nama", finalJenjang))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.le("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 6) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .createAlias("jurusan","jurusan").createAlias("jurusan.jenjang","jenjang")
                                            .add(Restrictions.in("jenjang.nama", finalJenjang))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.le("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswa_A_3_1_5.java:203"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 9, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

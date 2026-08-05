package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
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
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;
import ais.ui.util.DataCriteriaWithColumn;

public class LaporanProfileMahasiswaDanLulusan_A_3_1_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.1.1";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    public LaporanProfileMahasiswaDanLulusan_A_3_1_1() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanProfileMahasiswaDanLulusan_A_3_1_1(String title, String border, boolean closable) throws Exception {
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
                        for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
                            datas.add(SaptoGenerator.generateProfileMahasiswaDanLulusan(
                                session, label, rowIndex, selectedJurusan, true, "Reguler"));
                        }
                        HibernateUtil.closeSession();
                        for (int i = 0; i < 5; i++) datas.add(new ArrayList());
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
            final Integer[] yy = {tahun - 4, tahun - 3, tahun - 2, tahun - 1, tahun, 0};

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
                                    Integer colY = yy[y];
                                    String newTa = colY + "/" + (colY + 1);
                                    Integer[] tahunAngkatan = {tahun-4, tahun-3, tahun-2, tahun-1, tahun};

                                    if (x == 0) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(KapasitasMahasiswaBaru.class)
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahun", tahunAngkatan) : Restrictions.eq("tahun", colY));
                                        return new Object[]{c, new String[]{"jurusan.nama","tahunAkademik","ganjilGenap","jumlahTargetMahasiswaBaru"}};
                                    } else if (x == 1) {
                                        Criterion cr = Restrictions.or(Restrictions.eq("prodi1", selectedJurusan), Restrictions.eq("prodi2", selectedJurusan));
                                        cr = Restrictions.or(cr, Restrictions.eq("prodi3", selectedJurusan));
                                        cr = Restrictions.or(cr, Restrictions.eq("prodi4", selectedJurusan));
                                        cr = Restrictions.or(cr, Restrictions.eq("prodi5", selectedJurusan));
                                        Criteria c = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler")).add(cr)
                                            .add(colY == 0 ? Restrictions.in("tahun", tahunAngkatan) : Restrictions.eq("tahun", colY));
                                        return new Object[]{c, CetakRegistrasiAction.contents};
                                    } else if (x == 2) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler"))
                                            .add(Restrictions.eq("prodiLulus", selectedJurusan))
                                            .add(Restrictions.eq("tahunAkademik", newTa));
                                        return new Object[]{c, CetakRegistrasiAction.contents};
                                    } else if (x == 3) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.eq("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 4) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.eq("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 5) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.le("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 6) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.le("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 7) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler"))
                                            .createAlias("statusKeluar","statusKeluar")
                                            .add(Restrictions.ilike("statusKeluar.nama", "Lulus", MatchMode.EXACT))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunLulus", tahunAngkatan) : Restrictions.eq("tahunLulus", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 8) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunLulus", tahunAngkatan) : Restrictions.eq("tahunLulus", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x >= 9 && x <= 11) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("program", "Reguler"))
                                            .add(colY == 0 ? Restrictions.in("tahunLulus", tahunAngkatan) : Restrictions.eq("tahunLulus", colY))
                                            .createAlias("statusKeluar","statusKeluar").add(Restrictions.ilike("statusKeluar.nama", "Lulus", MatchMode.EXACT))
                                            .add(Restrictions.eq("jurusan", selectedJurusan));
                                        return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                    } else if (x == 12) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.lt("ipk", 2.75))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunLulus", tahunAngkatan) : Restrictions.eq("tahunLulus", colY));
                                        return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                    } else if (x == 13) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.between("ipk", 2.75, 3.5))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunLulus", tahunAngkatan) : Restrictions.eq("tahunLulus", colY));
                                        return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                    } else if (x == 14) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.gt("ipk", 3.5))
                                            .add(Restrictions.eq("program", "Reguler")).add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunLulus", tahunAngkatan) : Restrictions.eq("tahunLulus", colY));
                                        return new Object[]{c, new String[]{"nim","nama","tahunangkatan","tahunLulus","jurusan","ipk"}};
                                    }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswaDanLulusan_A_3_1_1.java:231"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 17, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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

/**
 * Jendela laporan borang akreditasi BAN-PT SAPTO butir <b>A-3.1.2</b>: profil mahasiswa dan
 * lulusan program studi terpilih untuk jalur <b>Non Reguler</b>, per tahun angkatan (5 tahun
 * terakhir, TS-4 s.d. TS): target/daya tampung, jumlah pendaftar, jumlah lulus seleksi, jumlah
 * mahasiswa baru reguler dan transfer/pindahan, serta jumlah mahasiswa aktif kumulatif. Baris
 * data per tahun dihitung lewat {@link SaptoGenerator#generateProfileMahasiswaDanLulusan}. Sel
 * tabel dapat diklik untuk membuka rincian data mentah (daya tampung/pendaftar/mahasiswa baru/
 * pindahan/aktif kumulatif) sesuai kolom yang diklik, lewat {@link Common#cetakDataCustomButton}.
 * Laporan hanya aktif bila satu jurusan/program studi dipilih pada filter (lihat
 * {@link #buildFilters}) — tanpa pemilihan jurusan, konten dikosongkan.
 */
public class LaporanProfileMahasiswaDanLulusan_A_3_1_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-3.1.2";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    private static final String[] EMPTY_COLS = new String[180];
    static { Arrays.fill(EMPTY_COLS, ""); }

    /** Konstruktor default: menyiapkan filter fakultas/jurusan dan tahun akademik (default tahun akademik berjalan), lalu membangun kerangka jendela laporan. */
    public LaporanProfileMahasiswaDanLulusan_A_3_1_2() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Seperti konstruktor default, dengan judul/border/closable jendela yang dapat disesuaikan. */
    public LaporanProfileMahasiswaDanLulusan_A_3_1_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    /** Kode butir borang yang dipetakan ke jendela ini: {@value #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menyusun kontrol filter pada baris toolbar: filter fakultas/jurusan bawaan {@link SaptoBaseWindow} (wajib dipilih agar laporan tampil), ditambah pemilih tahun akademik yang memicu {@link #onCetak} saat diubah. */
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

    /**
     * Menyusun data laporan A-3.1.2: bila jurusan dipilih, menghitung baris profil mahasiswa/
     * lulusan jalur Non Reguler untuk 5 tahun angkatan terakhir lewat
     * {@link SaptoGenerator#generateProfileMahasiswaDanLulusan}, dijalankan di thread terpisah
     * agar UI tidak terblokir; bila tidak ada jurusan terpilih, konten dikosongkan. Menyiapkan
     * juga listener klik sel yang membuka rincian data mentah sesuai kolom (daya tampung/
     * pendaftar/mahasiswa baru/pindahan/aktif kumulatif) dan baris tahun yang diklik.
     */
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
                                session, label, rowIndex, selectedJurusan, false, "Non Reguler"));
                        }
                        HibernateUtil.closeSession();
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
                                            .add(Restrictions.ne("program", "Reguler")).add(cr)
                                            .add(colY == 0 ? Restrictions.in("tahun", tahunAngkatan) : Restrictions.eq("tahun", colY));
                                        return new Object[]{c, CetakRegistrasiAction.contents};
                                    } else if (x == 2) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.ne("program", "Reguler"))
                                            .add(Restrictions.eq("prodiLulus", selectedJurusan))
                                            .add(Restrictions.eq("tahunAkademik", newTa));
                                        return new Object[]{c, CetakRegistrasiAction.contents};
                                    } else if (x == 3) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.ne("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.eq("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 4) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.ne("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.eq("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 5) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.ne("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.or(Restrictions.isNull("merupakanPindahan"), Restrictions.eq("merupakanPindahan", false)))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.le("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    } else if (x == 6) {
                                        Criteria c = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.ne("program", "Reguler")).add(Restrictions.isNull("statusKeluar"))
                                            .add(Restrictions.eq("merupakanPindahan", true))
                                            .add(Restrictions.eq("jurusan", selectedJurusan))
                                            .add(colY == 0 ? Restrictions.in("tahunangkatan", tahunAngkatan) : Restrictions.le("tahunangkatan", colY));
                                        return new Object[]{c, MahasiswaAction.contents};
                                    }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            EMPTY_COLS).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileMahasiswaDanLulusan_A_3_1_2.java:185"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 9, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

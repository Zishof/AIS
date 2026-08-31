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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.ui.util.DataCriteriaWithColumn;

/**
 * Jendela laporan borang akreditasi BAN-PT SAPTO butir <b>A-7.2.2_PT</b>: tabel silang jumlah
 * kegiatan pengabdian masyarakat dosen tetap yang disetujui, dipecah baris per sumber dana
 * ({@link #SUMBER_LIST}: swadana peneliti, PT/yayasan, Kemdiknas/kementerian lain, institusi
 * dalam negeri lain, institusi luar negeri) dan kolom per tahun (TS-2, TS-1, TS). Sel tabel dapat
 * diklik untuk menampilkan/mengunduh rincian data pengajuan yang membentuk angka tersebut
 * (difilter sesuai baris/kolom yang diklik) lewat {@link Common#cetakDataCustomButton}.
 */
public class LaporanPengabdianDosen_A_7_2_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-7.2.2_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Daftar kategori sumber dana pengabdian yang menjadi baris tabel; elemen terakhir kosong dipakai sebagai baris total/semua sumber. */
    private static final String[] SUMBER_LIST = {
        "Pembiayaan sendiri oleh peneliti",
        "PT/yayasan yang bersangkutan",
        "Kemdiknas/Kementerian lain terkait",
        "Institusi dalam negeri di luar Kemdiknas/Kementerian lain terkait",
        "Institusi luar negeri", ""
    };

    /** Konstruktor default: menyiapkan pemilih tahun akademik (default tahun akademik berjalan), lalu membangun kerangka jendela laporan. */
    public LaporanPengabdianDosen_A_7_2_2() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Seperti konstruktor default, dengan judul/border/closable jendela yang dapat disesuaikan. */
    public LaporanPengabdianDosen_A_7_2_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** Kode butir borang yang dipetakan ke jendela ini: {@value #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menyusun kontrol filter pada baris toolbar: pemilih tahun akademik yang memicu {@link #onCetak} saat diubah. */
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

    /**
     * Menyusun tabel silang laporan A-7.2.2_PT: untuk tiap sumber dana ({@link #SUMBER_LIST}),
     * menghitung jumlah pengajuan pengabdian dosen tetap yang disetujui per tahun (TS-2..TS),
     * dijalankan di thread terpisah agar UI tidak terblokir. Menyiapkan juga listener klik sel
     * yang membuka rincian data pengajuan sesuai baris/kolom yang diklik.
     */
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

                    for (String sumber : SUMBER_LIST) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add("");
                        for (int yr = tahun - 2; yr <= tahun; yr++) {
                            int jumlah = ((Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
                                .createAlias("sumberDanaPenelitianDanPengabdianes", "sumberDana")
                                .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                                .add(Restrictions.eq("dosen.tetap", 1))
                                .createAlias("penelitianDanPengabdian", "penelitianDanPengabdian")
                                .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENGABDIAN))
                                .add(Restrictions.eq("sumberDana.nama", sumber))
                                .add(Restrictions.eq("penelitianDanPengabdian.tahun", yr))
                                .add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
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
                            PengajuanPenelitianDanPengabdian.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    try {
                                        int x = ev.getColumn() - 2;
                                        int y = ev.getRow() - 7;
                                        String colY = SUMBER_LIST[y];
                                        Integer[] yy = {tahun - 2, tahun - 1, tahun, 0};
                                        Integer colX = yy[x];

                                        Criteria criteria = HibernateUtil.currentSession()
                                            .createCriteria(PengajuanPenelitianDanPengabdian.class)
                                            .createAlias("sumberDanaPenelitianDanPengabdianes", "sumberDana")
                                            .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                                            .add(Restrictions.eq("dosen.tetap", 1))
                                            .createAlias("penelitianDanPengabdian", "penelitianDanPengabdian")
                                            .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENGABDIAN))
                                            .add(colY.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                : Restrictions.eq("sumberDana.nama", colY))
                                            .add(colX == 0 ? Restrictions.sqlRestriction("true")
                                                : Restrictions.eq("penelitianDanPengabdian.tahun", colX))
                                            .add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI));

                                        return new Object[]{criteria, new String[]{"tbmuser.dosen.nidn","tbmuser.dosen.mycode",
                                            "tbmuser.dosen.nama","judul","keyword","tujuan","sumberDanaPenelitianDanPengabdianes",
                                            "jumlahDana","anggota","penelitianDanPengabdian.tahun","penelitianDanPengabdian.judul",
                                            "penelitianDanPengabdian.tipePenelitianDanPengabdian.isi",
                                            "penelitianDanPengabdian.tahunAkademik","penelitianDanPengabdian.semester",
                                            "penelitianDanPengabdian.sks"}};
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(arg0);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanPengabdianDosen_A_7_2_2.java:153"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

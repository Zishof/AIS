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
 * Jendela laporan borang akreditasi BAN-PT (SAPTO) butir A-7.1.2_PT — Penelitian Dosen Tetap: merekap
 * jumlah judul penelitian (tipe {@link ConstantValues#PENELITIAN}, status
 * {@link PengajuanPenelitianDanPengabdian#DISETUJUI}, hanya dosen tetap) yang disetujui per sumber
 * dana baku ({@link #SUMBER_LIST}) untuk tiga tahun berjalan (TS-2, TS-1, TS) relatif terhadap tahun
 * akademik yang dipilih. Setiap sel angka dapat diklik untuk mengunduh daftar pengajuan penelitian
 * yang menyusun angka tersebut, ditentukan dari posisi baris (sumber dana) dan kolom (tahun) sel
 * yang diklik. Memperluas {@link SaptoBaseWindow} untuk kerangka layar cetak/ekspor borang SAPTO baku.
 */
public class LaporanPenelitianDosen_A_7_1_2 extends SaptoBaseWindow {

    /** Kode sheet/butir borang SAPTO yang diwakili laporan ini. */
    public static final String sheetCode = "A-7.1.2_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Daftar kategori sumber dana penelitian baku yang direkap, sesuai urutan baris pada worksheet; elemen kosong terakhir mewakili baris total/lainnya. */
    private static final String[] SUMBER_LIST = {
        "Pembiayaan sendiri oleh peneliti",
        "PT/yayasan yang bersangkutan",
        "Kemdiknas/Kementerian lain terkait",
        "Institusi dalam negeri di luar Kemdiknas/Kementerian lain terkait",
        "Institusi luar negeri", ""
    };

    /** Konstruktor default; menyiapkan pilihan tahun akademik berjalan lalu membangun kerangka dasar layar. */
    public LaporanPenelitianDosen_A_7_1_2() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit, diteruskan ke {@link SaptoBaseWindow}. */
    public LaporanPenelitianDosen_A_7_1_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter berisi combobox tahun akademik; perubahan pilihan langsung memicu {@link #onCetak}. */
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
     * Menangani aksi cetak/tampilkan lembar kerja: di thread terpisah, untuk setiap kategori sumber
     * dana di {@link #SUMBER_LIST} dan setiap tahun (TS-2, TS-1, TS) menghitung jumlah pengajuan
     * penelitian dosen tetap yang disetujui, menyusun baris worksheet, lalu menampilkannya lewat
     * {@link SaptoUtil#displayWorksheet} dengan handler klik-sel yang membuka unduhan daftar
     * pengajuan penelitian sesuai sel (sumber dana x tahun) yang diklik.
     *
     * @param event event ZK pemicu aksi cetak (juga dipanggil manual dengan {@code null} saat filter berubah)
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
                                .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENELITIAN))
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
                                            .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENELITIAN))
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
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanPenelitianDosen_A_7_1_2.java:153"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

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

/**
 * Laporan borang akreditasi BAN-PT SAPTO butir A-7.1.4 (jumlah artikel jurnal dosen tetap yang
 * telah terindeks sitasi, disetujui, dan diterbitkan dalam 3 tahun akademik terakhir — TS-2, TS-1,
 * TS digabung). Menghitung total {@link Artikel} yang memenuhi kriteria tersebut untuk tahun
 * akademik terpilih; menyediakan tombol unduh data tambahan (detail per artikel, mis. NIDN dosen,
 * judul, ISSN, dsb) lewat {@link Common#cetakDataCustomButton} saat sel hasil diklik.
 */
public class LaporanPenelitianDosen_A_7_1_4 extends SaptoBaseWindow {

    public static final String sheetCode = "A-7.1.4_PT";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Membangun jendela laporan dengan tahun akademik berjalan terpilih pada combobox filter, lalu langsung membangun kerangka laporan. */
    public LaporanPenelitianDosen_A_7_1_4() {
        super();
        try {
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(true);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor varian dengan judul/border/closable eksplisit, dipakai saat jendela dibuat sebagai komponen tersemat. */
    public LaporanPenelitianDosen_A_7_1_4(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(true);
    }

    /** Mengembalikan kode sheet borang BAN-PT yang ditangani jendela ini: {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter berisi combobox pilihan tahun akademik (readonly); memilih ulang tahun akademik memicu {@link #onCetak} otomatis. */
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
     * Menyusun dan menampilkan worksheet A-7.1.4: menghitung jumlah {@link Artikel} dosen tetap
     * yang terindeks sitasi, berstatus disetujui, dan tahun terbitnya termasuk TS-2/TS-1/TS
     * relatif terhadap tahun akademik terpilih, dijalankan di thread terpisah. Sel hasil dapat
     * diklik untuk mengunduh data tambahan detail artikel lewat
     * {@link Common#cetakDataCustomButton}.
     *
     * @param event event pemicu (perubahan combobox tahun akademik), boleh {@code null}
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
                    for (int i = 0; i < 4; i++) datas.add(new ArrayList());

                    List sub = new ArrayList();
                    sub.add(""); sub.add(""); sub.add("");
                    int jumlah = ((Number) session.createCriteria(Artikel.class)
                        .createAlias("tingkatArtikeles", "tingkatArtikeles")
                        .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                        .add(Restrictions.eq("dosen.tetap", 1))
                        .add(Restrictions.eq("telahTerindeksSitasi", true))
                        .add(Restrictions.in("tahun", new Integer[]{tahun - 2, tahun - 1, tahun}))
                        .add(Restrictions.eq("status", Artikel.DISETUJUI))
                        .setProjection(Projections.rowCount()).uniqueResult()).intValue();
                    sub.add(jumlah);
                    datas.add(sub);

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
                                        Criteria criteria = HibernateUtil.currentSession()
                                            .createCriteria(Artikel.class)
                                            .createAlias("tingkatArtikeles", "tingkatArtikeles")
                                            .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                                            .add(Restrictions.eq("dosen.tetap", 1))
                                            .add(Restrictions.eq("telahTerindeksSitasi", true))
                                            .add(Restrictions.in("tahun", new Integer[]{tahun - 2, tahun - 1, tahun}))
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
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanPenelitianDosen_A_7_1_4.java:126"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

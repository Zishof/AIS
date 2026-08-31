package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sapto.PrasaranaPerguruanTinggiSapto;

/**
 * Jendela laporan borang akreditasi BAN-PT (SAPTO) butir A-6.2.3A — Prasarana Institusi: menyusun
 * lembar kerja berisi daftar prasarana milik/tersedia bagi perguruan tinggi (tahun perolehan, nama,
 * jumlah, luas, status kepemilikan sendiri/bukan sendiri, dan kondisi terawat/tidak terawat) yang
 * ditandai "utama". Tidak memiliki filter (lihat {@link #buildFilters}), sehingga seluruh data
 * prasarana utama diambil sekaligus. Memperluas {@link SaptoBaseWindow} untuk mewarisi kerangka
 * layar cetak/ekspor borang SAPTO yang seragam antar-butir laporan.
 */
public class LaporanPrasaranaInstitusi_A_6_2_3A extends SaptoBaseWindow {

    /** Kode sheet/butir borang SAPTO yang diwakili laporan ini. */
    public static final String sheetCode = "A-6.2.3A";
    private static final long serialVersionUID = 3331244819198611604L;

    /** Konstruktor default; membangun kerangka dasar layar segera saat instansiasi. */
    public LaporanPrasaranaInstitusi_A_6_2_3A() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit, diteruskan ke {@link SaptoBaseWindow}. */
    public LaporanPrasaranaInstitusi_A_6_2_3A(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }
    /** Tidak ada filter untuk laporan ini — seluruh data prasarana utama selalu diambil. */
    @Override protected void buildFilters(Row row) { /* no filters */ }

    /**
     * Menangani aksi cetak/tampilkan lembar kerja: mengosongkan konten, lalu di thread terpisah
     * memuat seluruh {@link PrasaranaPerguruanTinggiSapto} bertanda utama (diurutkan tahun, nama,
     * id), menyusunnya menjadi baris tabel worksheet (dengan tanda "v" untuk kolom kepemilikan dan
     * kondisi yang sesuai), lalu menampilkannya lewat {@link SaptoUtil#displayWorksheet}.
     *
     * @param event event ZK pemicu aksi cetak
     */
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
                    for (int i = 0; i < 11; i++) datas.add(new ArrayList());

                    List<PrasaranaPerguruanTinggiSapto> list = session
                        .createCriteria(PrasaranaPerguruanTinggiSapto.class)
                        .add(Restrictions.or(Restrictions.isNull("utama"), Restrictions.eq("utama", true)))
                        .addOrder(Order.asc("tahun")).addOrder(Order.asc("nama")).addOrder(Order.asc("id")).list();

                    for (PrasaranaPerguruanTinggiSapto p : list) {
                        List sub = new ArrayList();
                        sub.add("");
                        sub.add(p.getTahun());
                        sub.add(p.getNama());
                        sub.add(p.getJumlah());
                        sub.add(p.getLuas());

                        boolean milikSendiri = p.getJenisKepemilikan() != null &&
                            p.getJenisKepemilikan().equals(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_SENDIRI);
                        sub.add(milikSendiri ? "v" : "");
                        sub.add(milikSendiri ? "" : "v");

                        boolean terawat = p.getKondisi() != null &&
                            p.getKondisi().equals(PrasaranaPerguruanTinggiSapto.KONDISI_TERAWAT);
                        sub.add(terawat ? "v" : "");
                        sub.add(terawat ? "" : "v");

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

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

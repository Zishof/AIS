package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.DosenAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.TugasBelajarDosen;

/**
 * Laporan borang akreditasi BAN-PT butir A-4.5.2 (tugas belajar dosen): mendaftar seluruh
 * {@link TugasBelajarDosen} (studi lanjut dosen), opsional difilter berdasarkan jurusan, diurutkan
 * menurut tahun. Mengikuti kerangka kerja laporan sapto ({@link SaptoBaseWindow}); subkelas ini
 * menentukan {@code sheetCode} ({@code "A-4.5.2"}), filter fakultas/jurusan, dan pengisian data di
 * {@link #onCetak}. Setiap baris pada worksheet dapat diklik ({@code onCellClick}) untuk langsung
 * mencetak Daftar Riwayat Hidup (DRH) dosen bersangkutan lewat
 * {@link DosenAction#cetakDRHDosen(Dosen)}.
 */
public class LaporanProfileDosen_A_4_5_2 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.5.2";
    private static final long serialVersionUID = 3331244819198611604L;
    /** Membuat jendela laporan, menginisialisasi filter fakultas/jurusan, dan membangun tata letak dasar. */
    public LaporanProfileDosen_A_4_5_2() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membuat jendela laporan dengan judul/border/closable kustom; setup sama seperti konstruktor default. */
    public LaporanProfileDosen_A_4_5_2(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter fakultas/jurusan bawaan {@link SaptoBaseWindow} ke baris filter. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /** Mengambil data {@link TugasBelajarDosen} (difilter jurusan bila dipilih, di thread terpisah) dan menampilkannya sebagai worksheet A-4.5.2 dengan klik baris untuk cetak DRH dosen. */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();

            Session session = HibernateUtil.currentNativeSession();
            final List<TugasBelajarDosen> items = session.createCriteria(TugasBelajarDosen.class)
                .createAlias("dosen","dosen").addOrder(Order.asc("tahun"))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen.jurusan", selectedJurusan))
                .list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 8; i++) datas.add(new ArrayList());

                    int rowIndexTotal = 1;
                    for (int rowIndex = 1; rowIndex <= items.size(); rowIndex++) {
                        TugasBelajarDosen t = items.get(rowIndex - 1);
                        List sub = new ArrayList();
                        sub.add(""); sub.add(rowIndexTotal);
                        sub.add(t.getDosen().getNama());
                        sub.add(t.getJenjang() == null ? "" : t.getJenjang().getNama());
                        sub.add(t.getBidang()); sub.add(t.getNama());
                        sub.add(t.getNegara() == null ? "" : t.getNegara().getNamaNegara());
                        sub.add(t.getTahun());
                        datas.add(sub);
                        rowIndexTotal++;
                    }
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                }
            }).start();

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 9;
                        Dosen dosen = items.get(y).getDosen();
                        DosenAction.cetakDRHDosen(dosen);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen_A_4_5_2.java:99"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

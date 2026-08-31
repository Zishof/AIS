package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;

/**
 * Laporan borang akreditasi BAN-PT butir A-6.3.7 (aksesibilitas fasilitas institusi). Berbeda dari
 * laporan sapto lain yang menarik data dari database, kelas ini adalah TEMPLATE KOSONG: hanya
 * menyiapkan 21 baris data kosong dan membuka worksheet {@link #sheetCode} ("A-6.3.7_PT") lewat
 * {@link SaptoUtil#displayWorksheet} tanpa query apa pun — isian datanya diketik manual langsung
 * pada worksheet (Excel-like) yang ditampilkan. Tidak menyediakan filter apa pun.
 */
public class LaporanAksesbilitas_A_6_2_7 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.3.7_PT";
    private static final long serialVersionUID = 3331244819198611604L;

    /** Konstruktor default: membangun kerangka jendela dan langsung membuka worksheet kosong. */
    public LaporanAksesbilitas_A_6_2_7() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit; kegagalan inisialisasi dilempar ke pemanggil. */
    public LaporanAksesbilitas_A_6_2_7(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
    }

    /** @return kode sheet template worksheet borang, {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }
    /** Tidak menambahkan filter apa pun — worksheet ini diisi manual, bukan dari query. */
    @Override protected void buildFilters(Row row) { /* no filters */ }

    /** Handler cetak: membuka worksheet {@link #sheetCode} dengan 21 baris kosong (tanpa memuat data dari database) untuk diisi manual. */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 21; i++) datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                }
            }).start();

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}

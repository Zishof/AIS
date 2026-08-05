package ais.action.master.akreditasi;

import ais.common.Common;
import ais.ui.util.MyWindow;

/**
 * Dasbor utama Akreditasi LKPS S1 — menampilkan semua tabel LKPS
 * (Laporan Kinerja Program Studi) dalam enam tab yang di-load secara lazy.
 */
public class LaporanAkreditasiLKPS_S1 extends MyWindow {

    private static final long serialVersionUID = 1L;

    public LaporanAkreditasiLKPS_S1() {
        super();
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanAkreditasiLKPS_S1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        init();
    }

    private void init() {
        ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(
                Common.tampilanScrollTabbox(this), "100%", new int[] { 0 });

        // Tab 0: Sumber Dana - load immediately
        {
            org.zkoss.zul.Div panel = btnTab.tambahTab(0, "1.A.2 Sumber Dana", "/img/svg/money-bills.svg");
            try {
                panel.appendChild(new LaporanLKPS_1A2_SumberPendanaan());
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        btnTab.tambahTabLazy(1, "1.A.3 Penggunaan Dana", "/img/svg/chart-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                panel.appendChild(new LaporanLKPS_1A3_PenggunaanDana());
            }
        });
        btnTab.tambahTabLazy(2, "2.A.1 Data Mahasiswa", "/img/svg/graduate-cap.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                panel.appendChild(new LaporanLKPS_2A1_DataMahasiswa());
            }
        });
        btnTab.tambahTabLazy(3, "3.A.2 Penelitian", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                panel.appendChild(new LaporanLKPS_3A2_PenelitianDosen());
            }
        });
        btnTab.tambahTabLazy(4, "3.C.2 Publikasi", "/img/svg/journal-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                panel.appendChild(new LaporanLKPS_3C2_PublikasiDosen());
            }
        });
        btnTab.tambahTabLazy(5, "4.A.2 PkM", "/img/svg/user-follow-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception {
                panel.appendChild(new LaporanLKPS_4A2_PkMDosen());
            }
        });
    }
}

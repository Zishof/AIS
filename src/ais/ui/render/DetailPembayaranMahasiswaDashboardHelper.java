package ais.ui.render;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.model.CicilanPembayaran;

/**
 * Helper pemasangan panel dashboard/analisis pembayaran.
 *
 * Dashboard dipisahkan dari action agar DaftarUlangMahasiswaLamaAction dan
 * DaftarUlangMahasiswaBaruAction tidak lagi bergantung pada method tambahan di
 * DetailPembayaranMahasiswaRenderer. Renderer dapat dikembalikan ke versi lama
 * yang stabil untuk perhitungan tagihan, sementara panel dashboard tetap tampil.
 */
public final class DetailPembayaranMahasiswaDashboardHelper {

    private DetailPembayaranMahasiswaDashboardHelper() {
    }

    public static void pasangPanelAnalisisPembayaran(final DetailPembayaranMahasiswaRenderer renderer,
            final List<CicilanPembayaran> cicilans, final Row rowUtama) {
        if (rowUtama == null || rowUtama.getParent() == null) {
            return;
        }

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Row rowDashboard = new Row();
                rowUtama.getParent().appendChild(rowDashboard);

                if (renderer == null) {
                    return;
                }

                try {
                    renderer.hitungUlang();
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }

                try {
                    renderer.tampilDasboard(cicilans, rowDashboard);
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
            }
        });
    }
}

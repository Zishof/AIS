package ais.ui.util;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;

/**
 * Helper terpusat untuk dua kontrol standar pada header setiap Panel portal:
 *
 *   1. Tombol LAYAR PENUH (maximize) bawaan ZK -> panel.setMaximizable(true).
 *      Saat diklik, panel membesar memenuhi area sehingga detail lebih jelas.
 *   2. Tombol BANTUAN (tanda tanya) -> membuka popup modal berisi panduan
 *      khusus panel tersebut.
 *
 * Tujuannya agar SEMUA panel pada MyPortallayout punya perilaku seragam:
 * pengguna bisa memperbesar panel ke layar penuh DAN membuka panduan kapan
 * saja. Cukup panggil {@link #pasangBantuanDanLayarPenuh(Panel, String, String)}
 * sekali per panel.
 *
 * Pola popup mengikuti tombol Bantuan pada GenericCrudAction agar konsisten.
 *
 * Kompatibel Java 1.7 / gaya 1.6 dan ZK 5/9/10 CE
 * (tanpa lambda, try-with-resources, diamond, atau Stream API).
 */
public final class PanelToolHelper {

    private PanelToolHelper() {
    }

    /**
     * Pasang tombol Layar Penuh + Bantuan pada header sebuah Panel.
     *
     * PENTING: panggil method ini SEBELUM menambahkan Panelchildren ke panel,
     * karena Caption (header) wajib menjadi anak pertama Panel.
     *
     * @param panel    panel target (boleh null -> diabaikan)
     * @param judul    judul yang tampil di header (akan diterjemahkan)
     * @param helpHtml konten HTML panduan untuk panel ini (disarankan >= 500 kata)
     */
    public static void pasangBantuanDanLayarPenuh(final Panel panel, final String judul,
            final String helpHtml) {
        if (panel == null) {
            return;
        }

        // 1. Tombol Layar Penuh bawaan ZK (ikon maximize di kanan atas header).
        panel.setMaximizable(true);

        // 2. Header (Caption) berisi judul + tombol Bantuan di sisi kanan,
        //    bersisian dengan ikon Layar Penuh.
        Caption caption = new Caption();
        caption.setLabel(Common.getBahasaConfig(judul == null ? "" : judul));
        caption.setParent(panel);

        // Pembungkus float kanan: gaya diterapkan pada Div (bukan Toolbarbutton)
        // agar tidak terduplikasi ke .z-toolbarbutton-cnt pada ZK 5.
        Div toolWrap = new Div();
        toolWrap.setStyle("float:right;display:inline-flex;gap:6px;align-items:center;");
        toolWrap.setParent(caption);

        final MyToolbarbuttonConfig bantuan = new MyToolbarbuttonConfig(
                "Bantuan", "/img/svg/question-circle.svg");
        bantuan.setTooltiptext("Panduan penggunaan panel ini");
        bantuan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                tampilkanBantuan(judul, helpHtml);
            }
        });
        bantuan.setParent(toolWrap);
    }

    /**
     * Tampilkan popup modal berisi panduan panel.
     */
    public static void tampilkanBantuan(String judul, String helpHtml) throws Exception {
        final String judulF = judul;
        final String helpF = helpHtml;
        final MyWindow helpWindow = new MyWindow("Bantuan — "
                + Common.getBahasaConfig(judul == null ? "" : judul), "none", true);
        helpWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        helpWindow.setWidth("700px");
        helpWindow.setHeight("580px");

        Borderlayout bl = new MyBorderlayout();
        bl.setParent(helpWindow);

        Center center = new Center();
        center.setStyle("overflow:auto;padding:14px 18px;");
        ZkCompat.setFlex(center, true);
        center.setParent(bl);

        Html html = new Html();
        html.setContent(helpHtml == null ? "" : helpHtml);
        html.setParent(center);

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(bl);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);

        // Tombol Share: terbitkan panduan panel ini agar dapat dibuka siapa saja tanpa login.
        final MyToolbarbuttonConfig share = new MyToolbarbuttonConfig("Share", "/img/svg/send.svg");
        share.setTooltiptext("Bagikan panduan ini — dapat dibuka semua orang tanpa login");
        share.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                ais.action.master.helper.BantuanHelper.bagikanKonten(judulF, helpF);
            }
        });
        share.setParent(toolbar);

        final MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
        tutup.setTooltiptext("Tutup");
        tutup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                helpWindow.setVisible(false);
                helpWindow.detach();
            }
        });
        tutup.setParent(toolbar);

        helpWindow.setVisible(true);
        helpWindow.onModal();
    }
}

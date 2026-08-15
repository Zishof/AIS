package ais.action.master.koperasi;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyInclude;

/**
 * Pengaturan / Pusat Master Data Kantin — konversi dari {@code modul/kantin/pengaturan.jsp}.
 *
 * <p>Bukan layar data tersendiri, melainkan "hub" yang mengumpulkan seluruh halaman master kantin
 * (Member, Produk, Harga/Kulakan, Diskon, Pencairan, Metode Bayar, Meja, Pedagang, Toko) ke dalam
 * satu tampilan ber-tab. Tiap tab <b>memakai ulang</b> halaman ZK master yang sudah ada lewat
 * {@link MyInclude}, dimuat saat tab pertama kali dibuka (lazy) agar ringan.</p>
 */
public class PengaturanKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;

    /** Daftar tab: {label, path zul}. Semua path menunjuk halaman ZK master yang sudah ada. */
    private static final String[][] TABS = {
            { "Member", "/pages/master/koperasi/anggota_koperasi.zul" },
            { "Produk", "/pages/master/inventory/produk.zul" },
            { "Jenis Produk", "/pages/master/inventory/jenis_produk.zul" },
            { "Harga / Kulakan", "/pages/master/inventory/pengajuan_perubahan_harga_produk.zul" },
            { "Diskon", "/pages/master/koperasi/aturan_diskon.zul" },
            { "Diskon Grup", "/pages/master/koperasi/grup_aturan_diskon.zul" },
            { "Pencairan Diskon", "/pages/master/koperasi/pencairan_diskon.zul" },
            { "Metode Bayar", "/pages/master/koperasi/cara_pembayaran_koperasi.zul" },
            { "Meja", "/pages/master/koperasi/meja_kantin.zul" },
            { "Pedagang", "/pages/master/inventory/pedagang_kantin.zul" },
            { "Toko", "/pages/master/inventory/toko.zul" },
    };

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        DashboardUiKit.attachIntro(comp, "Pengaturan Kantin",
                "Satu tempat untuk mengelola seluruh data kantin: member, produk, harga/kulakan, diskon, "
                        + "metode pembayaran, meja, pedagang, dan toko. Klik tab yang diinginkan.");

        if (host == null) {
            return;
        }
        host.getChildren().clear();

        Tabbox tb = new Tabbox();
        if (tb != null) { tb.setWidth("100%"); }
        if (tb != null) { tb.setStyle("border:0;"); }
        Tabs tabs = new Tabs();
        if (tabs != null) { tabs.setParent(tb); }
        Tabpanels tps = new Tabpanels();
        if (tps != null) { tps.setParent(tb); }

        for (int i = 0; i < TABS.length; i++) {
            Tab t = new Tab(TABS[i][0]);
            t.setParent(tabs);
            Tabpanel tp = new ais.ui.util.MyTabpanel();
            tp.setStyle("padding:6px 2px;");
            tp.setParent(tps);
            if (i == 0) {
                t.setSelected(true);
                muat(tp, TABS[i][1]); // tab pertama dimuat langsung
            } else {
                lazy(t, tp, TABS[i][1]);
            }
        }
        if (tb != null) { tb.setParent(host); }
    }

    private void lazy(Tab tab, final Tabpanel panel, final String src) {
        final EventListener loader = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (panel.getChildren().isEmpty()) {
                    muat(panel, src);
                }
            }
        };
        tab.addEventListener("onClick", loader);
        tab.addEventListener("onSelect", loader);
    }

    private void muat(Tabpanel panel, String src) {
        try {
            MyInclude inc = new MyInclude(src);
            inc.setWidth("100%");
            inc.setHeight("100%");
            inc.setParent(panel);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            panel.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:#dc2626;padding:14px;'>Halaman tidak dapat dimuat.</div>"));
        }
    }
}

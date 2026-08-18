package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

/**
 * Helper UI reusable — tata letak komponen tombol aksi per baris.
 */
public class UIHelper {

    /**
     * Bungkus semua tombol aksi ke dalam kebab popup (⋯).
     * Satu tombol titik-tiga muncul di baris grid; klik → daftar aksi vertikal.
     *
     * <p>Aturan otomatis:</p>
     * <ul>
     *   <li>Tombol tanpa label → label diisi dari tooltiptext.</li>
     *   <li>Separator ditambahkan otomatis sebelum grup "danger" (hapus) dan "kunci".</li>
     *   <li>orient=vertical dan font-size:9px dari pola lama dibersihkan.</li>
     * </ul>
     *
     * @param parent      komponen induk (Row grid) — langsung di-parent
     * @param maxPerBaris diabaikan (dipertahankan agar signature tidak berubah)
     * @param tombol      daftar tombol yang akan masuk ke popup
     * @return Vbox pembungkus Hbox kebab (untuk backward-compat return type)
     */
    public static Vbox buatBarisAksi(Component parent, int maxPerBaris,
            List<Component> tombol) {

        // Hbox tipis yang hanya menampung satu tombol pemicu "⋯"
        final Hbox toolbar = new Hbox();
        toolbar.setSpacing("0px");
        toolbar.setAlign("center");
        toolbar.setSclass("ais-row-actions");
        toolbar.setStyle("display:flex;flex-wrap:nowrap;width:auto;align-items:center;"
                + "justify-content:flex-end;gap:0;");

        final Popup popup = new Popup();
        popup.setSclass("ais-row-popup");
        popup.setParent(toolbar);

        final Div popupContent = new Div();
        popupContent.setSclass("ais-row-popup-content");
        popupContent.setParent(popup);

        toolbar.setAttribute("ais_row_actions_popup", popupContent);

        List<Component> copy = new ArrayList<Component>(tombol);
        String prevGroup = ""; // deteksi pergantian grup untuk separator

        for (int i = 0; i < copy.size(); i++) {
            Component comp = copy.get(i);

            if (comp instanceof Toolbarbutton) {
                Toolbarbutton tb = (Toolbarbutton) comp;
                String sc = tb.getSclass() == null ? "" : tb.getSclass();
                boolean isDanger = sc.contains("danger") || sc.contains("delete");
                boolean isKunci = sc.contains("kunci");
                String curGroup = isDanger ? "danger" : (isKunci ? "kunci" : "normal");

                // Separator sebelum pergantian ke grup "danger" atau "kunci"
                if ((isDanger || isKunci) && !curGroup.equals(prevGroup) && i > 0) {
                    Div sep = new Div();
                    sep.setSclass("ais-row-popup-divider");
                    sep.setParent(popupContent);
                }
                prevGroup = curGroup;

                // Bersihkan sisa gaya pola lama (orient vertical, font-size kecil)
                try { tb.setOrient("horizontal"); } catch (Exception ignore) { }
                String ts = tb.getStyle() == null ? "" : tb.getStyle();
                if (ts.contains("font-size:9px") || ts.contains("font-size: 9px")) {
                    tb.setStyle("");
                }

                // Susun sclass: popup-item [+ danger] [+ sclass asli tanpa override]
                String base = "ais-row-popup-item";
                if (isDanger) base += " ais-row-popup-item-danger";
                if (!sc.isEmpty() && !sc.contains("ais-row-popup-item")) {
                    base = base + " " + sc;
                }
                tb.setSclass(base);

                // Isi label dari tooltiptext jika tombol tidak punya label
                String lbl = tb.getLabel();
                if ((lbl == null || lbl.trim().isEmpty()) && tb.getTooltiptext() != null
                        && !tb.getTooltiptext().trim().isEmpty()) {
                    tb.setLabel(tb.getTooltiptext());
                }
            }

            comp.setParent(popupContent);
        }

        // Tombol pemicu "⋯"
        final MyToolbarbuttonConfig triggerBtn = new MyToolbarbuttonConfig("", "/img/svg/three-dots.svg");
        triggerBtn.setSclass("ais-row-action-btn ais-row-action-kebab");
        triggerBtn.setTooltiptext("Aksi");
        final Popup finalPopup = popup;
        triggerBtn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                finalPopup.open(triggerBtn, "after_end");
            }
        });
        triggerBtn.setParent(toolbar);

        Vbox vbox = new Vbox();
        vbox.setAlign("center");
        vbox.setStyle("width:100%;");
        toolbar.setParent(vbox);

        if (parent != null) {
            vbox.setParent(parent);
        }
        return vbox;
    }

    /** Overload dengan maxPerBaris default = 3 (nilai diabaikan di implementasi baru). */
    public static Vbox buatBarisAksi(Component parent, List<Component> tombol) {
        return buatBarisAksi(parent, 3, tombol);
    }
}

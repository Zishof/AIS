package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

public class UIHelper {

    /**
     * Bungkus daftar tombol aksi ke dalam kebab popup (⋯).
     * Tombol tanpa label → label diisi dari tooltiptext.
     * Separator otomatis sebelum pergantian grup danger/kunci.
     *
     * @param parent      komponen induk (Row grid), boleh null
     * @param maxPerBaris diabaikan (backward-compat)
     * @param tombol      daftar tombol yang masuk popup
     * @return Vbox pembungkus Hbox kebab
     */
    public static Vbox buatBarisAksi(Component parent, int maxPerBaris,
            List<Component> tombol) {

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

        String prevGroup = "";
        List<Component> copy = new ArrayList<Component>(tombol);
        for (int i = 0; i < copy.size(); i++) {
            Component comp = copy.get(i);
            if (comp instanceof Toolbarbutton) {
                Toolbarbutton tb = (Toolbarbutton) comp;
                String sc = tb.getSclass() == null ? "" : tb.getSclass();
                boolean isDanger = sc.contains("danger") || sc.contains("delete");
                boolean isKunci  = sc.contains("kunci");
                String curGroup  = isDanger ? "danger" : (isKunci ? "kunci" : "normal");
                if (!curGroup.equals(prevGroup) && i > 0
                        && (isDanger || isKunci
                                || "danger".equals(prevGroup)
                                || "kunci".equals(prevGroup))) {
                    Div sep = new Div();
                    sep.setSclass("ais-row-popup-divider");
                    sep.setParent(popupContent);
                }
                prevGroup = curGroup;
                polaNilaiTombol(tb, sc, isDanger);
            }
            comp.setParent(popupContent);
        }

        tambahTombolPemicu(popup, toolbar);

        Vbox vbox = new Vbox();
        vbox.setAlign("center");
        vbox.setStyle("width:100%;");
        toolbar.setParent(vbox);
        if (parent != null) vbox.setParent(parent);
        return vbox;
    }

    /** Overload — maxPerBaris default 3 (nilai diabaikan). */
    public static Vbox buatBarisAksi(Component parent, List<Component> tombol) {
        return buatBarisAksi(parent, 3, tombol);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // wrapKebab — konversi Hbox berisi Toolbarbutton ke kebab in-place
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Konversi Hbox berisi Toolbarbutton menjadi kebab popup secara in-place.
     * Semua Toolbarbutton yang sudah ada di hbox dipindahkan ke dalam popup.
     * Tombol pemicu "⋯" ditambahkan sebagai anak terakhir hbox.
     * Atribut {@code ais_row_actions_popup} diset ke Div konten popup.
     */
    public static void wrapKebab(final Hbox hbox) {
        List<Component> existing = new ArrayList<Component>(hbox.getChildren());

        hbox.setSclass("ais-row-actions");
        hbox.setSpacing("0px");
        hbox.setAlign("center");
        hbox.setStyle("display:flex;flex-wrap:nowrap;width:auto;align-items:center;"
                + "justify-content:flex-end;gap:0;");

        final Popup popup = new Popup();
        popup.setSclass("ais-row-popup");
        popup.setParent(hbox);

        final Div popupContent = new Div();
        popupContent.setSclass("ais-row-popup-content");
        popupContent.setParent(popup);

        hbox.setAttribute("ais_row_actions_popup", popupContent);

        String prevGroup = "";
        for (int i = 0; i < existing.size(); i++) {
            Component comp = existing.get(i);
            if (comp instanceof Toolbarbutton) {
                Toolbarbutton tb = (Toolbarbutton) comp;
                String sc = tb.getSclass() == null ? "" : tb.getSclass();
                boolean isDanger = sc.contains("danger") || sc.contains("delete");
                boolean isKunci  = sc.contains("kunci");
                String curGroup  = isDanger ? "danger" : (isKunci ? "kunci" : "normal");
                if (!curGroup.equals(prevGroup) && i > 0
                        && (isDanger || isKunci
                                || "danger".equals(prevGroup)
                                || "kunci".equals(prevGroup))) {
                    Div sep = new Div();
                    sep.setSclass("ais-row-popup-divider");
                    sep.setParent(popupContent);
                }
                prevGroup = curGroup;
                polaNilaiTombol(tb, sc, isDanger);
            }
            comp.setParent(popupContent);
        }

        tambahTombolPemicu(popup, hbox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addToKebab — tambahkan satu tombol ke popup yang sudah ada
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tambahkan satu Toolbarbutton ke dalam popup kebab yang sudah ada.
     * Separator otomatis jika grup berbeda dari item terakhir.
     */
    public static void addToKebab(Div popupContent, Toolbarbutton tb) {
        String prevGroup = "normal";
        List<Component> existing = new ArrayList<Component>(popupContent.getChildren());
        for (int i = existing.size() - 1; i >= 0; i--) {
            Component c = existing.get(i);
            if (c instanceof Toolbarbutton) {
                String sc = ((Toolbarbutton) c).getSclass() == null ? "" : ((Toolbarbutton) c).getSclass();
                if (sc.contains("danger") || sc.contains("delete")) prevGroup = "danger";
                else if (sc.contains("kunci")) prevGroup = "kunci";
                else prevGroup = "normal";
                break;
            }
        }

        String sc = tb.getSclass() == null ? "" : tb.getSclass();
        boolean isDanger = sc.contains("danger") || sc.contains("delete");
        boolean isKunci  = sc.contains("kunci");
        String curGroup  = isDanger ? "danger" : (isKunci ? "kunci" : "normal");

        if (!curGroup.equals(prevGroup) && !existing.isEmpty()
                && (isDanger || isKunci
                        || "danger".equals(prevGroup)
                        || "kunci".equals(prevGroup))) {
            Div sep = new Div();
            sep.setSclass("ais-row-popup-divider");
            sep.setParent(popupContent);
        }

        polaNilaiTombol(tb, sc, isDanger);
        tb.setParent(popupContent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // absorptionKebab — dipanggil dari MyRowRenderer setelah render()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Setelah render selesai, pindahkan SEMUA tombol yang "nyasar" ke dalam
     * satu kebab popup per baris.
     *
     * <p>Kasus yang ditangani:</p>
     * <ol>
     *   <li>Toolbarbutton anak langsung dari Hbox ais-row-actions (bukan trigger)
     *       — contoh: {@code button.setParent(hbx)} setelah {@code copyEditDeleteButtons}</li>
     *   <li>Hbox non-kebab berisi Toolbarbutton — konversi ke kebab atau gabungkan</li>
     *   <li>Toolbarbutton anak langsung dari Row (standalone) — gabungkan ke kebab</li>
     * </ol>
     */
    public static void absorptionKebab(Row row) {
        // ── Fase 1: temukan kebab Hbox dan popup content yang sudah ada ──────
        Hbox kebabHbox = null;
        Div popupContent = null;

        List<Object> rowKids = new ArrayList<Object>(row.getChildren());

        for (Object child : rowKids) {
            if (child instanceof Hbox) {
                Hbox hbox = (Hbox) child;
                String sc = hbox.getSclass() == null ? "" : hbox.getSclass();
                if (sc.contains("ais-row-actions")) {
                    Object attr = hbox.getAttribute("ais_row_actions_popup");
                    if (attr instanceof Div) {
                        kebabHbox = hbox;
                        popupContent = (Div) attr;
                        break;
                    }
                }
            } else if (child instanceof Vbox) {
                // buatBarisAksi wraps Hbox in a Vbox — cek satu level dalam
                for (Object vChild : ((Vbox) child).getChildren()) {
                    if (vChild instanceof Hbox) {
                        Hbox hbox = (Hbox) vChild;
                        String sc = hbox.getSclass() == null ? "" : hbox.getSclass();
                        if (sc.contains("ais-row-actions")) {
                            Object attr = hbox.getAttribute("ais_row_actions_popup");
                            if (attr instanceof Div) {
                                kebabHbox = hbox;
                                popupContent = (Div) attr;
                                break;
                            }
                        }
                    }
                }
                if (kebabHbox != null) break;
            }
        }

        // ── Fase 2: tombol nyasar di dalam Hbox kebab (bukan trigger) ────────
        if (kebabHbox != null && popupContent != null) {
            List<Object> hboxKids = new ArrayList<Object>(kebabHbox.getChildren());
            for (Object child : hboxKids) {
                if (!(child instanceof Toolbarbutton)) continue;
                Toolbarbutton tb = (Toolbarbutton) child;
                String sc = tb.getSclass() == null ? "" : tb.getSclass();
                if (sc.contains("ais-row-action-kebab")) continue; // skip trigger
                // Tombol nyasar → pindahkan ke dalam popup
                addToKebab(popupContent, tb);
            }
        }

        // ── Fase 3: kumpulkan Hbox non-kebab dan standalone Toolbarbutton ────
        List<Hbox> orphanHboxes = new ArrayList<Hbox>();
        List<Toolbarbutton> orphanBtns = new ArrayList<Toolbarbutton>();

        rowKids = new ArrayList<Object>(row.getChildren());
        for (Object child : rowKids) {
            if (child == kebabHbox) continue;
            if (child instanceof Toolbarbutton) {
                orphanBtns.add((Toolbarbutton) child);
            } else if (child instanceof Hbox) {
                Hbox hbox = (Hbox) child;
                String sc = hbox.getSclass() == null ? "" : hbox.getSclass();
                if (sc.contains("ais-row-actions")) continue;
                for (Object k : hbox.getChildren()) {
                    if (k instanceof Toolbarbutton) {
                        orphanHboxes.add(hbox);
                        break;
                    }
                }
            }
        }

        if (kebabHbox == null && orphanHboxes.isEmpty() && orphanBtns.isEmpty()) return;

        // ── Fase 4: jika belum ada kebab, konversi Hbox pertama yang
        //           semua anaknya Toolbarbutton ──────────────────────────────
        if (kebabHbox == null && !orphanHboxes.isEmpty()) {
            for (int i = 0; i < orphanHboxes.size(); i++) {
                Hbox hbox = orphanHboxes.get(i);
                List<Object> kids = new ArrayList<Object>(hbox.getChildren());
                boolean allBtns = !kids.isEmpty();
                for (Object k : kids) {
                    if (!(k instanceof Toolbarbutton)) { allBtns = false; break; }
                }
                if (allBtns) {
                    wrapKebab(hbox);
                    Object attr = hbox.getAttribute("ais_row_actions_popup");
                    if (attr instanceof Div) {
                        kebabHbox = hbox;
                        popupContent = (Div) attr;
                        orphanHboxes.remove(i);
                        break;
                    }
                }
            }
        }

        // ── Fase 5: masih belum ada kebab → buat dari orphan standalone ──────
        if (kebabHbox == null && !orphanBtns.isEmpty()) {
            List<Component> btns = new ArrayList<Component>(orphanBtns);
            for (Toolbarbutton tb : orphanBtns) tb.detach();
            orphanBtns.clear();
            Vbox vbox = buatBarisAksi(row, btns);
            if (!vbox.getChildren().isEmpty()) {
                Object first = vbox.getChildren().get(0);
                if (first instanceof Hbox) {
                    kebabHbox = (Hbox) first;
                    Object attr = kebabHbox.getAttribute("ais_row_actions_popup");
                    if (attr instanceof Div) popupContent = (Div) attr;
                }
            }
        }

        if (popupContent == null) return;

        // ── Fase 6: gabungkan sisa Hbox orphan ke dalam popup ────────────────
        for (Hbox hbox : orphanHboxes) {
            List<Object> kids = new ArrayList<Object>(hbox.getChildren());
            for (Object k : kids) {
                if (k instanceof Toolbarbutton) {
                    addToKebab(popupContent, (Toolbarbutton) k);
                }
            }
            hbox.detach();
        }

        // ── Fase 7: gabungkan orphan standalone buttons ke dalam popup ────────
        for (Toolbarbutton tb : orphanBtns) {
            addToKebab(popupContent, tb);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Terapkan sclass popup-item, bersihkan gaya lama, isi label dari tooltiptext. */
    private static void polaNilaiTombol(Toolbarbutton tb, String sc, boolean isDanger) {
        try { tb.setOrient("horizontal"); } catch (Exception ignore) { }
        String ts = tb.getStyle() == null ? "" : tb.getStyle();
        if (ts.contains("font-size:9px") || ts.contains("font-size: 9px")) {
            tb.setStyle("");
        }

        String base = "ais-row-popup-item";
        if (isDanger) base += " ais-row-popup-item-danger";
        if (!sc.isEmpty() && !sc.contains("ais-row-popup-item")) {
            base = base + " " + sc;
        }
        tb.setSclass(base);

        String lbl = tb.getLabel();
        if ((lbl == null || lbl.trim().isEmpty()) && tb.getTooltiptext() != null
                && !tb.getTooltiptext().trim().isEmpty()) {
            tb.setLabel(tb.getTooltiptext());
        }
    }

    /** Buat dan tambahkan tombol pemicu ⋯ ke dalam hbox. */
    private static void tambahTombolPemicu(final Popup popup, final Hbox hbox) {
        final MyToolbarbuttonConfig triggerBtn =
                new MyToolbarbuttonConfig("", "/img/svg/three-dots.svg");
        triggerBtn.setSclass("ais-row-action-btn ais-row-action-kebab");
        triggerBtn.setTooltiptext("Aksi");
        triggerBtn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                popup.open(triggerBtn, "after_end");
            }
        });
        triggerBtn.setParent(hbox);
    }
}

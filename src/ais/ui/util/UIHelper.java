package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
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
        if (parent != null) {
            vbox.setParent(parent);
            // ZK 5 memanggil overload renderer dua argumen, sehingga hook normalisasi
            // pasca-render tidak selalu berjalan. Kecilkan langsung saat kebab dibuat.
            if (parent instanceof Row) {
                kecilkanKolomAksi((Row) parent, toolbar);
            }
        }
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
    // ambilItemAksi — ekstrak tombol aksi dari Hbox hasil copyEditDeleteButtons
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ambil daftar TOMBOL AKSI dari sebuah Hbox aksi baris (mis. hasil
     * {@code Common.copyEditDeleteButtons(...)}) untuk digabung ke daftar aksi lain
     * sebelum dikirim ke {@link #buatBarisAksi(Component, int, List)}.
     *
     * <p><b>Kenapa perlu.</b> {@code copyEditDeleteButtons} SUDAH membangun kebab sendiri:
     * anak LANGSUNG Hbox-nya adalah {@code Popup} (berisi Div item Ubah/Salin/Hapus) dan
     * tombol pemicu "⋯" — BUKAN tombolnya. Memakai {@code hbox.getChildren()} apa adanya
     * membuat kebab BERSARANG dua tingkat (pengguna harus klik "⋯" dua kali). Method ini
     * menembus ke isi popup lewat atribut {@code ais_row_actions_popup} sehingga yang
     * terkumpul adalah tombol aksinya langsung.</p>
     *
     * <p>Bila Hbox ternyata berisi tombol biasa (bukan pola kebab), tombol tersebut
     * dikembalikan apa adanya — aman dipakai untuk kedua bentuk.</p>
     *
     * @param hbox Hbox aksi baris; boleh {@code null} (mengembalikan daftar kosong)
     * @return daftar komponen tombol aksi, tidak pernah {@code null}
     */
    public static List<Component> ambilItemAksi(Hbox hbox) {
        List<Component> hasil = new ArrayList<Component>();
        if (hbox == null) {
            return hasil;
        }
        Object attr = hbox.getAttribute("ais_row_actions_popup");
        if (attr instanceof Div) {
            // Pola kebab: item aksi ada di dalam Div konten popup.
            for (Object anak : new ArrayList<Object>(((Div) attr).getChildren())) {
                if (anak instanceof Component) {
                    hasil.add((Component) anak);
                }
            }
            return hasil;
        }
        // Pola lama: tombol adalah anak langsung Hbox (lewati Popup bila ada).
        for (Object anak : new ArrayList<Object>(hbox.getChildren())) {
            if (anak instanceof Popup) {
                continue;
            }
            if (anak instanceof Component) {
                hasil.add((Component) anak);
            }
        }
        return hasil;
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
     *   <li>Sel trailing (paling belakang baris) yang murni tombol —
     *       konversi/serap ke kebab; sel campuran atau di tengah baris
     *       TIDAK disentuh agar konten dan keselarasan kolom aman</li>
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

        // ── Fase 3: kumpulkan "trailing run" — sel paling belakang baris yang
        //    murni berisi tombol. HANYA sel trailing yang boleh diserap:
        //    • mencabut sel di tengah baris menggeser keselarasan kolom;
        //    • Hbox campuran (Label + tombol) adalah konten sel fungsional,
        //      BUKAN toolbar aksi — jangan pernah disentuh.
        //    Sel kebab (atau Vbox pembungkusnya) ikut dilewati dalam run.
        rowKids = new ArrayList<Object>(row.getChildren());
        List<Component> trailingRun = new ArrayList<Component>();
        for (int i = rowKids.size() - 1; i >= 0; i--) {
            Object child = rowKids.get(i);
            if (isKebabContainer(child, kebabHbox)) continue; // batas aman, lanjut mundur
            if (child instanceof Toolbarbutton) {
                trailingRun.add(0, (Toolbarbutton) child);
            } else if (child instanceof Hbox && isAllButtonHbox((Hbox) child)) {
                trailingRun.add(0, (Hbox) child);
            } else {
                break; // sel non-aksi → stop; sel sebelum ini tidak disentuh
            }
        }

        if (kebabHbox == null && trailingRun.isEmpty()) return;

        // ── Fase 4: belum ada kebab → jadikan elemen PERTAMA trailing run
        //    sebagai kebab, secara in-place (posisi sel tidak berubah) ─────────
        if (kebabHbox == null) {
            Component first = trailingRun.remove(0);
            if (first instanceof Hbox) {
                Hbox hbox = (Hbox) first;
                wrapKebab(hbox);
                Object attr = hbox.getAttribute("ais_row_actions_popup");
                if (attr instanceof Div) {
                    kebabHbox = hbox;
                    popupContent = (Div) attr;
                }
            } else if (first instanceof Toolbarbutton) {
                // Tombol standalone → sisipkan Hbox kebab tepat di posisinya
                Hbox hbox = new Hbox();
                row.insertBefore(hbox, first);
                first.setParent(hbox);
                wrapKebab(hbox);
                Object attr = hbox.getAttribute("ais_row_actions_popup");
                if (attr instanceof Div) {
                    kebabHbox = hbox;
                    popupContent = (Div) attr;
                }
            }
        }

        if (popupContent == null) return;

        // ── Fase 5: serap sisa trailing run ke dalam popup ───────────────────
        for (Component comp : trailingRun) {
            if (comp instanceof Toolbarbutton) {
                addToKebab(popupContent, (Toolbarbutton) comp);
            } else if (comp instanceof Hbox) {
                Hbox hbox = (Hbox) comp;
                List<Object> kids = new ArrayList<Object>(hbox.getChildren());
                for (Object k : kids) {
                    if (k instanceof Toolbarbutton) {
                        addToKebab(popupContent, (Toolbarbutton) k);
                    }
                }
                hbox.detach();
            }
        }

        // ── Fase 6: kecilkan kolom Aksi — set lebar Column grid ke 56px ──────
        kecilkanKolomAksi(row, kebabHbox);

        // ── Fase 7: buang PITA KOSONG di kanan tabel ────────────────────────
        rapikanKolomSisaKanan(row);
    }

    /**
     * Hilangkan pita kosong di sisi KANAN grid.
     *
     * <p><b>Akar masalah.</b> Fase 5 memanggil {@code hbox.detach()} untuk sel tombol yang
     * sudah diserap ke dalam kebab. Akibatnya jumlah SEL pada baris menjadi lebih sedikit
     * daripada jumlah {@code <column>} yang dideklarasikan di ZUL, dan ZK merender kolom
     * sisa itu sebagai ruang kosong lebar di ujung kanan tabel. Sebagian layar juga memang
     * sejak awal mendeklarasikan kolom kosong tambahan di ZUL.</p>
     *
     * <p><b>Perbaikan.</b> Ciutkan kolom sisa (lebar 0 + disembunyikan) mulai dari paling
     * kanan. Pembersihan BERHENTI begitu menemukan kolom yang punya label — kolom berlabel
     * adalah kolom nyata milik layar dan tidak boleh disentuh, sekalipun barisnya sedang
     * kosong. Dengan begitu perbaikan ini hanya membuang kolom yang benar-benar tak berisi
     * apa pun, dan berlaku otomatis untuk SELURUH grid tanpa perlu mengubah ratusan ZUL.</p>
     */
    private static void rapikanKolomSisaKanan(Row row) {
        try {
            Component rows = row.getParent();
            if (rows == null || !(rows.getParent() instanceof Grid)) {
                return;
            }
            Grid grid = (Grid) rows.getParent();
            if (grid.getColumns() == null) {
                return;
            }
            List<Object> cols = new ArrayList<Object>(grid.getColumns().getChildren());
            int jumlahSel = row.getChildren().size();
            /* PENJAGA (20-08-2026): jangan menciutkan kolom bila baris justru punya sel LEBIH
             * BANYAK daripada kolom (colspan/detail row) -- pemetaan sel-ke-kolom tak dapat
             * dipercaya. Sisakan pula minimal satu kolom agar tabel tidak kehilangan semuanya. */
            if (jumlahSel > cols.size() || jumlahSel < 1) return;
            for (int i = cols.size() - 1; i >= Math.max(jumlahSel, 1); i--) {
                Object col = cols.get(i);
                if (!(col instanceof Column)) {
                    continue;
                }
                Column column = (Column) col;
                String label = column.getLabel() == null ? "" : column.getLabel().trim();
                if (label.length() > 0) {
                    // Kolom berlabel = kolom nyata milik layar -> berhenti, jangan disentuh.
                    break;
                }
                if (column.isVisible()) {
                    column.setWidth("0px");
                    column.setVisible(false);
                }
            }
        } catch (Exception ignore) {
        }
    }

    /** True bila child adalah sel kebab itu sendiri atau Vbox pembungkusnya. */
    private static boolean isKebabContainer(Object child, Hbox kebabHbox) {
        if (kebabHbox == null) return false;
        if (child == kebabHbox) return true;
        if (child instanceof Vbox) {
            for (Object v : ((Vbox) child).getChildren()) {
                if (v == kebabHbox) return true;
            }
        }
        return false;
    }

    /** True bila hbox punya >=1 anak dan SEMUA anaknya Toolbarbutton. */
    private static boolean isAllButtonHbox(Hbox hbox) {
        List<Object> kids = new ArrayList<Object>(hbox.getChildren());
        if (kids.isEmpty()) return false;
        String sc = hbox.getSclass() == null ? "" : hbox.getSclass();
        if (sc.contains("ais-row-actions")) return false; // kebab lain, jangan dobel
        for (Object k : kids) {
            if (!(k instanceof Toolbarbutton)) return false;
        }
        return true;
    }

    /**
     * Set lebar Column grid yang menampung kebab menjadi 56px (seragam di
     * semua CRUD). Aman di-skip bila struktur tidak sesuai (bukan Grid, index
     * kolom tidak cocok, dsb).
     */
    private static void kecilkanKolomAksi(Row row, Hbox kebabHbox) {
        if (kebabHbox == null) return;
        try {
            // Naik ke anak langsung Row (kebab bisa terbungkus Vbox)
            Component cell = kebabHbox;
            while (cell.getParent() != null && cell.getParent() != row) {
                cell = cell.getParent();
            }
            if (cell.getParent() != row) return;

            int idx = row.getChildren().indexOf(cell);
            if (idx < 0) return;

            Component rows = row.getParent();
            if (rows == null || !(rows.getParent() instanceof Grid)) return;
            Grid grid = (Grid) rows.getParent();
            if (grid.getColumns() == null) return;

            List<Object> cols = new ArrayList<Object>(grid.getColumns().getChildren());
            /* PENJAGA (20-08-2026): ubah lebar kolom HANYA bila jumlah SEL baris persis sama
             * dengan jumlah KOLOM. Bila tidak sejajar (baris memakai colspan/detail row, atau
             * sel sudah diserap kebab), indeks sel TIDAK menunjuk kolom yang sama sehingga
             * kolom ISI bisa ikut dipersempit ke lebar kolom aksi -- konten jadi terhimpit dan
             * layar tampak rusak. Lebih aman tidak mengubah apa pun. */
            if (row.getChildren().size() != cols.size()) return;
            if (idx >= cols.size()) return;
            Object col = cols.get(idx);
            if (col instanceof Column) {
                Column column = (Column) col;
                if (!GridKolomHelper.LEBAR_KOLOM_AKSI.equals(column.getWidth())) {
                    column.setWidth(GridKolomHelper.LEBAR_KOLOM_AKSI);
                    column.setAlign("center");
                }
            }
        } catch (Exception ignore) { }
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

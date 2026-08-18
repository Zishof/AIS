package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.ZkCompat;

/**
 * Popup "Pencarian Lebih Lanjut" — helper terpusat untuk SEMUA halaman filter.
 *
 * <h3>Cara kerja (tanpa JavaScript, murni ZK server-side)</h3>
 * <pre>
 *   // Panggil SEKALI di akhir doAfterCompose, setelah semua inisialisasi selesai:
 *   FilterLanjutHelper.setup(comp);
 * </pre>
 *
 * <h3>Yang dilakukan setup() secara otomatis</h3>
 * <ol>
 *   <li>Cari {@code .ais-crud-filter-card} di pohon komponen.</li>
 *   <li>Ambil semua {@code <row>} dari filter grid.</li>
 *   <li>Baris pertama = <em>primary</em> — tetap tampil di kartu utama.</li>
 *   <li>Baris ke-2 dan seterusnya = <em>advanced</em> — dipindah ke popup overlay.</li>
 *   <li>Tombol "Pencarian Lanjutan" ditambah ke {@code .ais-crud-filter-actions}.</li>
 *   <li>Popup: overlay + panel + header + body (grid) + footer (Terapkan + Tutup).</li>
 * </ol>
 *
 * <h3>Keamanan ID-space ZK</h3>
 * Overlay diletakkan di dalam filter card yang merupakan turunan Window yang sama.
 * Semua komponen tetap berada di ID-space yang sama → autowiring ZK tidak terpengaruh
 * → {@code self.getFellow("searchfakultas")} tetap berfungsi setelah komponen dipindah.
 *
 * <h3>Keamanan model / kriteria Hibernate</h3>
 * Helper ini HANYA mengatur tata letak UI. Logika {@code initCriteria()} di setiap
 * Action kelas tidak disentuh sama sekali. Filter hanya berdampak pada kueri jika
 * Action secara eksplisit mendaftarkan kriteria untuk field tersebut — sehingga
 * field yang tidak ada di model tidak akan ikut kueri.
 *
 * <h3>Mode A — backward-compatible (ZUL sudah punya overlay)</h3>
 * Metode {@link #buka}, {@link #tutup}, {@link #tambahBtn} tetap tersedia
 * untuk Action yang sudah punya field {@code filterLanjutOverlay}.
 *
 * Kompatibel Java 1.7, ZK CE 10. Tanpa lambda, tanpa Stream, tanpa diamond.
 */
public class FilterLanjutHelper {

    private static final String ATTR_OVERLAY   = "_flh_overlay";
    private static final String ATTR_BTN       = "_flh_btn";
    private static final String ATTR_EXTRA_BTN = "_flh_extraBtns";
    private static final String ATTR_DONE      = "_flh_setup_done";

    private FilterLanjutHelper() {
    }

    // =========================================================================
    // Mode A — backward-compatible
    // =========================================================================

    /** Tampilkan overlay popup. */
    public static void buka(Div overlay) {
        if (overlay != null) {
            overlay.setVisible(true);
        }
    }

    /** Sembunyikan overlay popup. */
    public static void tutup(Div overlay) {
        if (overlay != null) {
            overlay.setVisible(false);
        }
    }

    /** Cek apakah popup sedang terbuka. */
    public static boolean isTerbuka(Div overlay) {
        return overlay != null && overlay.isVisible();
    }

    /**
     * Tambah tombol ke container pencarian lanjutan (containerBtnLanjut).
     * Jika container null (ZUL belum dimigrasi), fallback ke parent addBtn.
     */
    public static void tambahBtn(Div containerBtnLanjut, Component addBtn, Component btn) {
        if (btn == null) {
            return;
        }
        if (containerBtnLanjut != null) {
            containerBtnLanjut.appendChild(btn);
        } else if (addBtn != null && addBtn.getParent() != null) {
            addBtn.getParent().appendChild(btn);
        }
    }

    /** Overload tanpa addBtn — hanya masuk container jika tersedia. */
    public static void tambahBtn(Div containerBtnLanjut, Component btn) {
        tambahBtn(containerBtnLanjut, null, btn);
    }

    // =========================================================================
    // Mode B — auto-setup (tidak perlu ubah ZUL)
    // =========================================================================

    /**
     * Setup otomatis popup pencarian lanjutan — mode default.
     * Baris pertama tetap tampil di primary, baris ke-2+ masuk popup.
     *
     * @param root komponen root dari ZUL (parameter comp di doAfterCompose)
     */
    public static void setup(final Component root) {
        setupCore(root, 1);
    }

    /**
     * Setup otomatis popup pencarian lanjutan dengan kontrol penuh.
     *
     * @param root               komponen root dari ZUL
     * @param semuaBarisKePopup  {@code true} = SEMUA baris filter masuk popup
     *                           (filter area utama tersembunyi, hanya tombol tampil);
     *                           {@code false} = baris pertama tetap di primary (default)
     */
    public static void setup(final Component root, final boolean semuaBarisKePopup) {
        setupCore(root, semuaBarisKePopup ? 0 : 1);
    }

    /**
     * Setup otomatis popup pencarian lanjutan dengan jumlah baris primary yang dapat dikonfigurasi.
     *
     * @param root          komponen root dari ZUL
     * @param barisPrimary  jumlah baris filter yang tetap tampil di area utama (bukan di popup);
     *                      0 = semua baris masuk popup; 1 = default (hanya baris pertama); 2+ = N baris pertama di primary
     */
    public static void setup(final Component root, final int barisPrimary) {
        setupCore(root, barisPrimary);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setupCore(final Component root, final int barisPrimary) {
        if (root == null) {
            return;
        }

        final Component filterCard = cariByClass(root, "ais-crud-filter-card");
        if (filterCard == null) {
            return;
        }

        // Idempoten: jangan setup ulang jika sudah dikerjakan
        if (Boolean.TRUE.equals(filterCard.getAttribute(ATTR_DONE))) {
            return;
        }
        filterCard.setAttribute(ATTR_DONE, Boolean.TRUE);

        // Cari filter grid
        final Grid filterGrid = (Grid) cariByClass(filterCard, "ais-crud-filter-grid");
        if (filterGrid == null || filterGrid.getRows() == null) {
            return;
        }

        List<Row> semuaBaris = ambilRow(filterGrid.getRows());
        int mulai = barisPrimary;

        // Butuh baris yang cukup: mode semua-ke-popup >=1, mode primary >=barisPrimary+1
        if (semuaBaris.size() <= mulai) {
            return;
        }

        List<Row> barisAdvanced = new ArrayList(semuaBaris.subList(mulai, semuaBaris.size()));

        // ── 1. Buat OVERLAY modal (Div, bukan ZK Popup) ──────────────────────
        // ZK 5.5 Popup gagal RE-OPEN setelah ditutup (state widget client stale →
        // klik ke-2 tidak muncul). Div overlay yang di-toggle setVisible 100% andal.
        final Div popup = new Div();
        popup.setSclass("ais-filter-lanjut-overlay");
        popup.setVisible(false);
        popup.setParent(filterCard);

        // Backdrop: klik area gelap → tutup (panel berada di atas backdrop via z-index).
        Div backdrop = new Div();
        backdrop.setSclass("ais-filter-lanjut-backdrop");
        backdrop.setParent(popup);
        backdrop.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event e) throws Exception {
                popup.setVisible(false);
                MyToolbarbuttonConfig b = (MyToolbarbuttonConfig) filterCard.getAttribute(ATTR_BTN);
                if (b != null) b.setSclass("ais-btn-pencarian-lanjut");
            }
        });

        Div panel = new Div();
        panel.setSclass("ais-filter-lanjut-panel");
        panel.setParent(popup);

        // ── 2. Header popup ───────────────────────────────────────────────
        Div head = new Div();
        head.setSclass("ais-filter-lanjut-head");
        head.setParent(panel);

        Html titleEl = new Html();
        titleEl.setContent(
            "<span class='ais-filter-lanjut-title'>"
            + "&#128269;&nbsp;Filter Pencarian</span>");
        titleEl.setParent(head);

        // Container untuk tombol-tombol ekstra (diisi Action via tambahBtn)
        final Div extraBtns = new Div();
        extraBtns.setSclass("ais-filter-lanjut-extra-btns");
        extraBtns.setParent(head);

        // Tombol Tutup di kanan header
        final MyToolbarbuttonConfig btnTutupHead = new MyToolbarbuttonConfig(
                "✕ Tutup", null);
        btnTutupHead.setSclass("ais-filter-lanjut-close-btn");
        btnTutupHead.setTooltiptext("Tutup pencarian lanjutan");
        btnTutupHead.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event e) throws Exception {
                popup.setVisible(false);
                MyToolbarbuttonConfig b = (MyToolbarbuttonConfig) filterCard.getAttribute(ATTR_BTN);
                if (b != null) b.setSclass("ais-btn-pencarian-lanjut");
            }
        });
        btnTutupHead.setParent(head);

        // ── 3. Body: grid dengan baris-baris advanced ─────────────────────
        Div body = new Div();
        body.setSclass("ais-filter-lanjut-body");
        body.setParent(panel);

        // Grid di dalam popup — tanpa fixedLayout agar kolom auto-size mengisi lebar popup
        Grid advGrid = new MyGrid();
        advGrid.setSclass("ais-crud-filter-grid ais-filter-popup-grid");
        advGrid.setParent(body);

        // Pindah baris advanced ke grid popup (tetap di ID-space Window yang sama)
        Rows advRows = new Rows();
        advRows.setParent(advGrid);
        for (Row baris : barisAdvanced) {
            baris.detach();
            baris.setParent(advRows);
        }

        // Mode semua-ke-popup: sembunyikan area filter utama
        if (barisPrimary == 0) {
            Component filterBody = cariByClass(filterCard, "ais-crud-filter-body");
            if (filterBody instanceof HtmlBasedComponent) {
                ((HtmlBasedComponent) filterBody).setVisible(false);
            }
        }

        // ── 4. Footer: Terapkan + Tutup ───────────────────────────────────
        Div foot = new Div();
        foot.setSclass("ais-filter-lanjut-foot");
        foot.setParent(panel);

        MyToolbarbuttonConfig btnTerapkan = new MyToolbarbuttonConfig(
                "✓ Terapkan Filter", null);
        btnTerapkan.setSclass("ais-btn-terapkan-filter");
        btnTerapkan.setTooltiptext("Terapkan filter dan lihat hasil pencarian");
        btnTerapkan.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event e) throws Exception {
                popup.setVisible(false);
                MyToolbarbuttonConfig b = (MyToolbarbuttonConfig) filterCard.getAttribute(ATTR_BTN);
                if (b != null) b.setSclass("ais-btn-pencarian-lanjut");
                Events.sendEvent(new Event("onSearchDefault", root, null));
            }
        });
        btnTerapkan.setParent(foot);

        MyToolbarbuttonConfig btnTutupFoot = new MyToolbarbuttonConfig(
                "Tutup", null);
        btnTutupFoot.setSclass("ais-filter-lanjut-close-btn");
        btnTutupFoot.setTooltiptext("Tutup tanpa menerapkan filter");
        btnTutupFoot.addEventListener(Events.ON_CLICK, new EventListener() {
            public void onEvent(Event e) throws Exception {
                popup.setVisible(false);
                MyToolbarbuttonConfig b = (MyToolbarbuttonConfig) filterCard.getAttribute(ATTR_BTN);
                if (b != null) b.setSclass("ais-btn-pencarian-lanjut");
            }
        });
        btnTutupFoot.setParent(foot);

        // ── 5. Tombol "Filter Lanjutan" di filter actions ─────────────────
        Component actionsDiv = cariByClass(filterCard, "ais-crud-filter-actions");
        if (actionsDiv != null) {
            String labelBtn = barisPrimary == 0 ? "⋮ Filter" : "+ Lanjutan";
            String tooltipBtn = barisPrimary == 0
                    ? "Buka panel filter pencarian"
                    : "Tampilkan filter pencarian lanjutan";
            final MyToolbarbuttonConfig btnLanjut = new MyToolbarbuttonConfig(
                    labelBtn, "/img/svg/filter.svg");
            btnLanjut.setSclass("ais-btn-pencarian-lanjut");
            btnLanjut.setTooltiptext(tooltipBtn);
            btnLanjut.addEventListener(Events.ON_CLICK, new EventListener() {
                public void onEvent(Event e) throws Exception {
                    // TOGGLE overlay: setVisible selalu andal untuk buka/tutup berulang.
                    if (popup.isVisible()) {
                        popup.setVisible(false);
                        btnLanjut.setSclass("ais-btn-pencarian-lanjut");
                    } else {
                        popup.setVisible(true);
                        btnLanjut.setSclass("ais-btn-pencarian-lanjut ais-btn-pencarian-lanjut--active");
                    }
                }
            });
            // Insert sebelum elemen pertama
            Component firstChild = actionsDiv.getFirstChild();
            if (firstChild != null) {
                actionsDiv.insertBefore(btnLanjut, firstChild);
            } else {
                btnLanjut.setParent(actionsDiv);
            }
            filterCard.setAttribute(ATTR_BTN, btnLanjut);
        }

        // Simpan referensi untuk akses dari luar
        filterCard.setAttribute(ATTR_OVERLAY, popup);
        filterCard.setAttribute(ATTR_EXTRA_BTN, extraBtns);
    }

    // ── Akses dari Action ─────────────────────────────────────────────────────

    /**
     * Dapatkan overlay (Div) yang dibuat oleh {@link #setup} (Mode B).
     * Berguna jika Action ingin membuka/menutup popup secara manual
     * ({@code getPopup(card).setVisible(true/false)}).
     */
    public static Div getPopup(Component filterCard) {
        if (filterCard == null) {
            return null;
        }
        Object o = filterCard.getAttribute(ATTR_OVERLAY);
        return (o instanceof Div) ? (Div) o : null;
    }

    /** @deprecated Mode B kini pakai Popup — gunakan {@link #getPopup}. */
    public static Div getOverlay(Component filterCard) {
        return null;
    }

    /**
     * Dapatkan container tombol ekstra di header popup.
     * Tambah tombol aksi kustom di sini: {@code extraBtns.appendChild(myBtn)}.
     */
    public static Div getExtraBtnsContainer(Component filterCard) {
        if (filterCard == null) {
            return null;
        }
        Object o = filterCard.getAttribute(ATTR_EXTRA_BTN);
        return (o instanceof Div) ? (Div) o : null;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Cari komponen pertama (DFS) yang memiliki sclass {@code target}.
     * Cukup efisien untuk pohon komponen ZK yang tipikal.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Component cariByClass(Component root, String target) {
        if (root == null || target == null) {
            return null;
        }
        if (memilikiClass(root, target)) {
            return root;
        }
        List<Component> kids = new ArrayList<Component>(root.getChildren());
        for (int i = 0; i < kids.size(); i++) {
            Component found = cariByClass(kids.get(i), target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean memilikiClass(Component c, String target) {
        if (!(c instanceof HtmlBasedComponent)) {
            return false;
        }
        String sc = ((HtmlBasedComponent) c).getSclass();
        if (sc == null || sc.trim().length() == 0) {
            return false;
        }
        // Cocokkan sebagai kata utuh (dibatasi spasi atau awal/akhir string)
        return (" " + sc.trim() + " ").contains(" " + target + " ");
    }

    private static List<Row> ambilRow(Rows rows) {
        List<Row> result = new ArrayList<Row>();
        if (rows == null) {
            return result;
        }
        List<Component> kids = new ArrayList<Component>(rows.getChildren());
        for (int i = 0; i < kids.size(); i++) {
            Component c = kids.get(i);
            if (c instanceof Row) {
                result.add((Row) c);
            }
        }
        return result;
    }
}

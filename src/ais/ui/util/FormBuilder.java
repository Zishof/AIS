package ais.ui.util;

import org.zkoss.zul.Cell;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

/**
 * Reusable form-row builder: alternating odd/even cell backgrounds, hint text,
 * section headers, and full-width rows — all from a single, centrally-styled class.
 *
 * Cara pakai minimal:
 *
 *   org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
 *   formGrid.setStyle("border:none;width:100%;");
 *   formGrid.setParent(cardWrap);
 *   Rows rows = new Rows();
 *   rows.setParent(formGrid);
 *
 *   FormBuilder fb = new FormBuilder(rows);
 *   fb.addRow("Nama *", namaTextbox);
 *   fb.addRow("Kode",   kodeTextbox, "Singkatan 3 huruf");
 *   fb.addSectionHeader("Informasi Tambahan");
 *   fb.addRow("Keterangan", keteranganTextbox);
 *
 * Design tokens: ubah satu konstanta di sini, semua form ikut berubah.
 *
 * Kompatibel Java 1.6 gaya (tidak ada lambda / diamond / try-with-resources).
 */
public class FormBuilder {

    // =========================================================
    //  Design Tokens — ubah di sini, berlaku ke SEMUA form
    // =========================================================

    /** Warna utama (gradient header & aksen section). */
    public static final String COLOR_PRIMARY      = "#1d4ed8";

    // Permintaan: form Add/Edit JANGAN warna selang-seling antar baris — cukup
    // garis tipis pemisah (CELL_BORDER_BOTTOM). Karena itu warna ganjil & genap
    // dibuat SAMA (seragam): kolom label tetap satu tint lembut, kolom value putih.
    /** Background sel label (seragam semua baris, tidak selang-seling). */
    public static final String COLOR_LABEL_ODD    = "#f1f5f9";

    /** Background sel label (seragam semua baris). */
    public static final String COLOR_LABEL_EVEN   = "#f1f5f9";

    /** Background sel value (seragam putih, tidak selang-seling). */
    public static final String COLOR_VALUE_ODD    = "#ffffff";

    /** Background sel value (seragam putih). */
    public static final String COLOR_VALUE_EVEN   = "#ffffff";

    /** Warna border pemisah antar-baris. */
    public static final String COLOR_BORDER       = "#d1d9e6";

    /** Background header section di dalam form. */
    public static final String COLOR_SECTION_BG   = "#dbeafe";

    /** Warna teks header section. */
    public static final String COLOR_SECTION_TEXT = "#1e40af";

    /** Warna teks label kolom kiri. */
    public static final String COLOR_TEXT_LABEL   = "#374151";

    /** Warna teks hint (petunjuk kecil di bawah input). */
    public static final String COLOR_TEXT_HINT    = "#6b7280";

    // =========================================================
    //  Composite style strings — digunakan di form manapun
    // =========================================================

    /**
     * Style div card wrapper — rounded corner + drop shadow.
     * Tempel langsung sebagai setStyle() pada Div pembungkus form.
     */
    public static final String STYLE_CARD_WRAP =
            "border:1px solid " + COLOR_BORDER + ";"
            + "border-radius:8px;overflow:hidden;"
            + "box-shadow:0 2px 8px rgba(0,0,0,0.09);margin:4px;";

    /**
     * Style div gradient header di atas card form.
     * Gunakan dengan Html atau Div sebagai container header.
     *
     * Contoh:
     *   Html hdr = new Html();
     *   hdr.setContent("<div style='" + FormBuilder.STYLE_HEADER + "'>Judul</div>");
     */
    public static final String STYLE_HEADER =
            "background:linear-gradient(135deg," + COLOR_PRIMARY + " 0%,#3b82f6 100%);"
            + "color:#fff;padding:11px 16px;font-size:14px;font-weight:bold;"
            + "letter-spacing:0.3px;";

    /**
     * Style untuk area South (toolbar bawah form).
     * Tempel pada south.setStyle().
     */
    public static final String STYLE_TOOLBAR_AREA =
            "border-top:1px solid " + COLOR_BORDER + ";background:#f8fafc;";

    // =========================================================
    //  Internal style fragments
    // =========================================================

    private static final String CELL_BORDER_BOTTOM =
            "border-bottom:1px solid " + COLOR_BORDER + ";";

    private static final String CELL_PADDING = "padding:9px 12px;";

    private static final String LABEL_STYLE =
            "font-weight:600;font-size:13px;color:" + COLOR_TEXT_LABEL + ";";

    private static final String HINT_STYLE =
            "font-size:11px;color:" + COLOR_TEXT_HINT + ";display:block;margin-top:3px;";

    // =========================================================
    //  State
    // =========================================================

    private final Rows rows;
    private int rowIndex = 0;

    // =========================================================
    //  Constructor
    // =========================================================

    /**
     * @param rows Rows yang sudah menjadi child dari Grid target.
     */
    public FormBuilder(Rows rows) {
        this.rows = rows;
    }

    // =========================================================
    //  Public API
    // =========================================================

    /**
     * Tambah baris label + input tanpa teks petunjuk.
     *
     * @param labelText Teks label kolom kiri, mis. "Nama Agama *"
     * @param input     Komponen input ZK (Textbox, Longbox, Combobox, dsb.)
     * @return Row yang baru dibuat
     */
    public Row addRow(String labelText, org.zkoss.zk.ui.Component input) {
        return addRowInternal(labelText, input, null);
    }

    /**
     * Tambah baris label + input + teks petunjuk kecil di bawah input.
     *
     * @param labelText Teks label kolom kiri
     * @param input     Komponen input ZK
     * @param hint      Teks petunjuk; null atau kosong = tidak ditampilkan
     * @return Row yang baru dibuat
     */
    public Row addRow(String labelText, org.zkoss.zk.ui.Component input, String hint) {
        return addRowInternal(labelText, input, hint);
    }

    /**
     * Tambah section header full-width (colspan 2) sebagai pemisah grup field.
     * Mereset counter alternasi sehingga grup berikutnya mulai dari baris ganjil.
     *
     * @param title Judul section, mis. "Informasi Feeder Dikti"
     */
    public void addSectionHeader(String title) {
        Row row = new Row();
        Cell cell = new Cell();
        cell.setColspan(2);
        cell.setStyle(
                "background:" + COLOR_SECTION_BG + ";"
                + "color:" + COLOR_SECTION_TEXT + ";"
                + "font-weight:bold;font-size:11px;"
                + "letter-spacing:0.8px;text-transform:uppercase;"
                + CELL_PADDING
                + "border-bottom:1px solid #bfdbfe;"
        );
        Label lbl = new Label(title);
        lbl.setParent(cell);
        cell.setParent(row);
        row.setParent(rows);
        rowIndex = 0;
    }

    /**
     * Tambah baris full-width (colspan 2) untuk konten bebas.
     * Berguna untuk Checkbox, pesan informatif, atau komponen custom
     * yang tidak memerlukan label di kolom kiri.
     *
     * @param content Komponen ZK apapun
     * @return Row yang baru dibuat
     */
    public Row addFullRow(org.zkoss.zk.ui.Component content) {
        Row row = new Row();
        Cell cell = new Cell();
        cell.setColspan(2);
        cell.setStyle(
                "background:#f8fafc;"
                + CELL_PADDING
                + CELL_BORDER_BOTTOM
        );
        content.setParent(cell);
        cell.setParent(row);
        row.setParent(rows);
        return row;
    }

    /**
     * Reset counter baris ganjil/genap ke nol.
     * Berguna saat menambah baris secara kondisional agar urutan warna tidak loncat.
     */
    public void resetIndex() {
        rowIndex = 0;
    }

    /** Kembalikan index baris saat ini (0-based). */
    public int getRowIndex() {
        return rowIndex;
    }

    // =========================================================
    //  Internal helpers
    // =========================================================

    private Row addRowInternal(String labelText,
                               org.zkoss.zk.ui.Component input,
                               String hint) {
        boolean odd = (rowIndex % 2 == 0);
        rowIndex++;

        String bgLabel = odd ? COLOR_LABEL_ODD  : COLOR_LABEL_EVEN;
        String bgValue = odd ? COLOR_VALUE_ODD  : COLOR_VALUE_EVEN;

        Row row = new Row();

        // ---- Label cell (kolom kiri 35%) ----
        Cell labelCell = new Cell();
        labelCell.setStyle(
                "background:" + bgLabel + ";"
                + CELL_BORDER_BOTTOM
                + CELL_PADDING
                + "vertical-align:middle;"
                + "width:35%;"
        );
        Label lbl = new Label(labelText);
        lbl.setStyle(LABEL_STYLE);
        lbl.setParent(labelCell);
        labelCell.setParent(row);

        // ---- Value cell (kolom kanan 65%) ----
        Cell valueCell = new Cell();
        valueCell.setStyle(
                "background:" + bgValue + ";"
                + CELL_BORDER_BOTTOM
                + CELL_PADDING
                + "vertical-align:middle;"
        );

        boolean hasHint = hint != null && !hint.trim().isEmpty();
        if (hasHint) {
            // Bungkus input + hint dalam Div agar hint selalu di bawah input
            Div wrapper = new Div();
            wrapper.setStyle("width:100%;");
            input.setParent(wrapper);
            Label hintLbl = new Label(hint);
            hintLbl.setStyle(HINT_STYLE);
            hintLbl.setParent(wrapper);
            wrapper.setParent(valueCell);
        } else {
            input.setParent(valueCell);
        }

        valueCell.setParent(row);
        row.setParent(rows);
        return row;
    }
}

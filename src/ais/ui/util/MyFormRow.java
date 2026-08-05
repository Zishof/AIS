package ais.ui.util;

import org.zkoss.zul.Row;

/**
 * Row standar untuk form — otomatis menerapkan CSS class "ais-form-row".
 *
 * Style terpusat di css_utama.css (.ais-form-row > td):
 *   border: 0; padding: 4px; vertical-align: top;
 *
 * Gunakan sebagai pengganti langsung new MyFormRow() pada form:
 *   MyFormRow row = new MyFormRow();
 *   // tidak perlu row.setSclass("ais-form-row"); — sudah otomatis
 */
public class MyFormRow extends Row {

    private static final long serialVersionUID = 1L;

    public MyFormRow() {
        super();
        setValign("top");
        setSclass("ais-form-row");
    }
}

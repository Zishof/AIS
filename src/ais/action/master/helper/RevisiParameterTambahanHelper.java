package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.ParameterTambahan;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiParameterTambahanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiParameterTambahanHelper(EventListener eventListener) throws Exception {
        super(ParameterTambahan.class, "Revisi Parameter Tambahan", eventListener, new String[] { "nilaiDataInputan", "labelInputan", "nama", "keterangan" });
    }
}

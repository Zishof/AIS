package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Perkuliahan;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPerkuliahanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiPerkuliahanHelper(EventListener eventListener) throws Exception {
        super(Perkuliahan.class, "Revisi Perkuliahan", eventListener, new String[] { "kelas", "tahunAjaran", "program", "keterangan" });
    }
}

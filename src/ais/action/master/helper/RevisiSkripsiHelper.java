package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Skripsi;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSkripsiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiSkripsiHelper(EventListener eventListener) throws Exception {
        super(Skripsi.class, "Revisi Skripsi", eventListener, new String[] { "judul", "nama", "keterangan" });
    }
}

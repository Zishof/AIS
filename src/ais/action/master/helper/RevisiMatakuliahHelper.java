package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Matakuliah;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiMatakuliahHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiMatakuliahHelper(EventListener eventListener) throws Exception {
        super(Matakuliah.class, "Revisi Matakuliah", eventListener, new String[] { "kode", "nama", "keterangan" });
    }
}

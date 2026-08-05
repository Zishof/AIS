package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Notifikasi;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes" })
public class RevisiNotifikasiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiNotifikasiHelper(EventListener eventListener) throws Exception {
        super(Notifikasi.class, "Revisi Notifikasi", eventListener, new String[] { "emails", "nama", "keterangan" });
    }
}

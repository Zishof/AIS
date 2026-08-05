package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Mahasiswa;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiMahasiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiMahasiswaHelper(EventListener eventListener) throws Exception {
        super(Mahasiswa.class, "Revisi Mahasiswa", eventListener, new String[] { "nim", "nama", "email", "keterangan" });
    }
}

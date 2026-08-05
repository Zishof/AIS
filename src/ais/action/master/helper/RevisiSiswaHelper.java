package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.Siswa;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiSiswaHelper(EventListener eventListener) throws Exception {
        super(Siswa.class, "Revisi Siswa", eventListener, new String[] { "nomorIndukNasional", "nomorInduk", "namaSiswa", "nama" });
    }
}

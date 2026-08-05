package ais.action.master.sekolah.helper;

import ais.action.master.helper.GenericRevisiHelper;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.CalonSiswa;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiCalonSiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiCalonSiswaHelper(EventListener eventListener) throws Exception {
        super(CalonSiswa.class, "Revisi Calon Siswa", eventListener, new String[] { "noRegistrasi", "noUjian", "namaSiswa", "nama", "nomorIndukNasional" });
    }
}

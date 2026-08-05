package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.PembombotanNilai;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPembobotanNilaiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiPembobotanNilaiHelper(EventListener eventListener) throws Exception {
        super(PembombotanNilai.class, "Revisi Pembobotan Nilai", eventListener, new String[] { "nama", "keterangan" });
    }
}

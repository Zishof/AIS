package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.Tagihan;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiTagihanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiTagihanHelper(EventListener eventListener) throws Exception {
        super(Tagihan.class, "Revisi Tagihan", eventListener, new String[] { "nama", "keterangan", "bulan", "tahun" });
    }
}

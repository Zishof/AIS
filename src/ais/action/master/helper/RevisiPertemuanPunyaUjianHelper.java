package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanPunyaUjianHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiPertemuanPunyaUjianHelper(Pertemuan pertemuan, EventListener eventListener) throws Exception {
        super(PertemuanPunyaUjian.class, "Revisi Ujian Pertemuan", eventListener, new String[] { "nama", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("pertemuan", pertemuan));
    }
}

package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Pertemuan;
import ais.database.model.kkn.KelompokKkn;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanKknHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiPertemuanKknHelper(KelompokKkn kelompokKkn, EventListener eventListener) throws Exception {
        super(Pertemuan.class, "Revisi Pertemuan KKN", eventListener, new String[] { "topik", "absensi", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("kelompokKkn", kelompokKkn));
    }
}

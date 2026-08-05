package ais.action.master.sop.helper;

import ais.action.master.helper.GenericRevisiHelper;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sop.DisposisiSop;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDisposisiSopHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiDisposisiSopHelper(EventListener eventListener) throws Exception {
        super(DisposisiSop.class, "Revisi Disposisi SOP", eventListener, new String[] { "keterangan", "properti", "nama" });
    }
}

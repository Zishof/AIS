package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.VirtualAccountBank;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiVirtualAccountBankHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiVirtualAccountBankHelper(EventListener eventListener) throws Exception {
        super(VirtualAccountBank.class, "Revisi Virtual Account Bank", eventListener, new String[] { "noVa", "kode", "nama", "keterangan" });
    }
}

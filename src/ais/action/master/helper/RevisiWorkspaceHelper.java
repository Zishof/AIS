package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.rab.Workspace;

/**
 * Wrapper kompatibilitas untuk helper revisi lama.
 * Logika utama dipusatkan di GenericRevisiHelper agar session handling, restore,
 * pencarian, dan rendering revisi konsisten di semua modul.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiWorkspaceHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiWorkspaceHelper(EventListener eventListener) throws Exception {
        super(Workspace.class, "Revisi Workspace/RAB", eventListener, new String[] { "kode", "nama", "keterangan" });
    }
}

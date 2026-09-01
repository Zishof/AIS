package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.rab.Workspace;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link Workspace} (modul RAB —
 * Rencana Anggaran Biaya) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur
 * window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat data Workspace ditampilkan). Field
 * pencarian: {@code kode}, {@code nama}, {@code keterangan}. Judul window: "Revisi
 * Workspace/RAB".</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiWorkspaceHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi data Workspace/RAB (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiWorkspaceHelper(EventListener eventListener) throws Exception {
        super(Workspace.class, "Revisi Workspace/RAB", eventListener, new String[] { "kode", "nama", "keterangan" });
    }
}

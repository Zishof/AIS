package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Matakuliah;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Matakuliah} (data induk matakuliah) — lihat Javadoc class tersebut
 * untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code kode}, {@code nama}, {@code keterangan}. Tidak ada
 * {@link GenericRevisiHelper.QueryCustomizer} (seluruh riwayat revisi matakuliah tampil tanpa
 * penyaringan) dan tidak ada override hook {@code afterRestoreInTransaction}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiMatakuliahHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link Matakuliah}.
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiMatakuliahHelper(EventListener eventListener) throws Exception {
        super(Matakuliah.class, "Revisi Matakuliah", eventListener, new String[] { "kode", "nama", "keterangan" });
    }
}

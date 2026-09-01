package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Perkuliahan;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Perkuliahan} (data penjadwalan/kelas perkuliahan) — lihat Javadoc
 * class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code kelas}, {@code tahunAjaran}, {@code program}, {@code keterangan}.
 * Tidak ada {@link GenericRevisiHelper.QueryCustomizer} (seluruh riwayat revisi perkuliahan
 * tampil tanpa penyaringan) dan tidak ada override hook {@code afterRestoreInTransaction}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPerkuliahanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link Perkuliahan}.
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiPerkuliahanHelper(EventListener eventListener) throws Exception {
        super(Perkuliahan.class, "Revisi Perkuliahan", eventListener, new String[] { "kelas", "tahunAjaran", "program", "keterangan" });
    }
}

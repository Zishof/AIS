package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.ParameterTambahan;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.ParameterTambahan} (definisi field/parameter tambahan dinamis pada
 * suatu form/entitas lain) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur
 * window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code nilaiDataInputan}, {@code labelInputan}, {@code nama},
 * {@code keterangan}. Tidak ada {@link GenericRevisiHelper.QueryCustomizer} (seluruh riwayat
 * revisi tampil tanpa penyaringan) dan tidak ada override hook {@code afterRestoreInTransaction}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiParameterTambahanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link ParameterTambahan}.
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiParameterTambahanHelper(EventListener eventListener) throws Exception {
        super(ParameterTambahan.class, "Revisi Parameter Tambahan", eventListener, new String[] { "nilaiDataInputan", "labelInputan", "nama", "keterangan" });
    }
}

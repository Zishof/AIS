package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Notifikasi;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Notifikasi} (data notifikasi/pengingat) — lihat Javadoc class
 * tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code emails}, {@code nama}, {@code keterangan}. Tidak ada
 * {@link GenericRevisiHelper.QueryCustomizer} (seluruh riwayat revisi notifikasi tampil tanpa
 * penyaringan) dan tidak ada override hook {@code afterRestoreInTransaction}.
 */
@SuppressWarnings({ "rawtypes" })
public class RevisiNotifikasiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link Notifikasi}.
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiNotifikasiHelper(EventListener eventListener) throws Exception {
        super(Notifikasi.class, "Revisi Notifikasi", eventListener, new String[] { "emails", "nama", "keterangan" });
    }
}

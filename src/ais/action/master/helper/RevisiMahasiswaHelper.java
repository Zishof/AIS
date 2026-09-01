package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Mahasiswa;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Mahasiswa} (data induk mahasiswa) — lihat Javadoc class tersebut untuk
 * penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code nim}, {@code nama}, {@code email}, {@code keterangan} — kolom
 * identitas utama mahasiswa. Tidak ada {@link GenericRevisiHelper.QueryCustomizer} (seluruh
 * riwayat revisi mahasiswa tampil tanpa penyaringan) dan tidak ada override hook
 * {@code afterRestoreInTransaction}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiMahasiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link Mahasiswa}.
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiMahasiswaHelper(EventListener eventListener) throws Exception {
        super(Mahasiswa.class, "Revisi Mahasiswa", eventListener, new String[] { "nim", "nama", "email", "keterangan" });
    }
}

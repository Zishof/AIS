package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.Siswa;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link Siswa} (modul sekolah) —
 * lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat data siswa sekolah ditampilkan).
 * Pencarian kata kunci menyaring identitas siswa: {@code nomorIndukNasional} (NISN),
 * {@code nomorInduk} (NIS), {@code namaSiswa}, dan {@code nama}. Bandingkan dengan
 * {@link ais.action.master.sekolah.helper.RevisiCalonSiswaHelper} untuk riwayat calon siswa
 * (sebelum diterima).</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi data Siswa (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiSiswaHelper(EventListener eventListener) throws Exception {
        super(Siswa.class, "Revisi Siswa", eventListener, new String[] { "nomorIndukNasional", "nomorInduk", "namaSiswa", "nama" });
    }
}

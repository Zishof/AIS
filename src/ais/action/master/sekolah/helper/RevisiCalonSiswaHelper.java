package ais.action.master.sekolah.helper;

import ais.action.master.helper.GenericRevisiHelper;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.CalonSiswa;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link CalonSiswa} (calon siswa sebelum diterima/dikonversi menjadi {@code Siswa}) — lihat
 * Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur
 * restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat calon siswa ditampilkan). Field
 * pencarian: {@code noRegistrasi}, {@code noUjian}, {@code namaSiswa}, {@code nama},
 * {@code nomorIndukNasional} — mencerminkan tahap pendaftaran (nomor registrasi/ujian) sebelum
 * identitas resmi (NISN) lengkap. Bandingkan dengan
 * {@link ais.action.master.helper.RevisiSiswaHelper} untuk riwayat siswa yang sudah diterima.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiCalonSiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi data Calon Siswa (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
     */
    public RevisiCalonSiswaHelper(EventListener eventListener) throws Exception {
        super(CalonSiswa.class, "Revisi Calon Siswa", eventListener, new String[] { "noRegistrasi", "noUjian", "namaSiswa", "nama", "nomorIndukNasional" });
    }
}

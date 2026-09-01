package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Pertemuan;
import ais.database.model.pkl.KelompokPkl;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link Pertemuan} — lihat Javadoc
 * class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: {@code Pertemuan} adalah entity bersama yang dipakai lintas modul akademik
 * (kuliah, KKN, PKL, dst); helper ini MEMBATASI riwayat hanya pada pertemuan yang menjadi milik
 * satu {@link KelompokPkl} tertentu lewat {@link GenericRevisiHelper.FixedPropertyFilter} pada
 * property {@code kelompokPkl}. Field pencarian: {@code topik}, {@code absensi},
 * {@code keterangan}. Bandingkan dengan {@link RevisiPertemuanKknHelper} (pola sama untuk KKN).</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanPklHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi Pertemuan, dibatasi pada satu kelompok PKL.
     *
     * @param kelompokPkl   kelompok PKL yang membatasi riwayat pertemuan yang ditampilkan.
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiPertemuanPklHelper(KelompokPkl kelompokPkl, EventListener eventListener) throws Exception {
        super(Pertemuan.class, "Revisi Pertemuan PKL", eventListener, new String[] { "topik", "absensi", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("kelompokPkl", kelompokPkl));
    }
}

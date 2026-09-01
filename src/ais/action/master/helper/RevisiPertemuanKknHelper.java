package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Pertemuan;
import ais.database.model.kkn.KelompokKkn;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link Pertemuan} — lihat Javadoc
 * class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: {@code Pertemuan} adalah entity bersama yang dipakai lintas modul akademik
 * (kuliah, KKN, PKL, dst); helper ini MEMBATASI riwayat hanya pada pertemuan yang menjadi milik
 * satu {@link KelompokKkn} tertentu lewat {@link GenericRevisiHelper.FixedPropertyFilter} pada
 * property {@code kelompokKkn}. Field pencarian: {@code topik}, {@code absensi},
 * {@code keterangan}. Bandingkan dengan {@link RevisiPertemuanPklHelper} (pola sama untuk PKL).</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanKknHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi Pertemuan, dibatasi pada satu kelompok KKN.
     *
     * @param kelompokKkn   kelompok KKN yang membatasi riwayat pertemuan yang ditampilkan.
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiPertemuanKknHelper(KelompokKkn kelompokKkn, EventListener eventListener) throws Exception {
        super(Pertemuan.class, "Revisi Pertemuan KKN", eventListener, new String[] { "topik", "absensi", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("kelompokKkn", kelompokKkn));
    }
}

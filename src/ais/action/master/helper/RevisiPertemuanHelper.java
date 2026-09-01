package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;

/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Pertemuan} (pertemuan/sesi kelas dalam satu {@link Perkuliahan}) —
 * lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore. Tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Field pencarian: {@code topik}, {@code absensi}, {@code keterangan}. Berbeda dari
 * kebanyakan subclass lain, konstruktor di sini SELALU menyaring lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code perkuliahan} — tidak ada
 * jalur konstruksi tanpa filter perkuliahan (perhatikan: bila {@code perkuliahan} bernilai
 * {@code null}, filter tetap dipasang dengan nilai {@code null}, yang berarti hanya cocok dengan
 * revisi Pertemuan yang property {@code perkuliahan}-nya juga null, bukan "tanpa filter").
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPertemuanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link Pertemuan} milik satu {@link Perkuliahan}.
     *
     * @param perkuliahan perkuliahan yang membatasi riwayat yang ditampilkan (dipasang sebagai
     *                    {@link GenericRevisiHelper.FixedPropertyFilter} pada property
     *                    {@code perkuliahan}, termasuk bila bernilai {@code null})
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiPertemuanHelper(Perkuliahan perkuliahan, EventListener eventListener) throws Exception {
        super(Pertemuan.class, "Revisi Pertemuan", eventListener, new String[] { "topik", "absensi", "keterangan" }, new GenericRevisiHelper.FixedPropertyFilter("perkuliahan", perkuliahan));
    }
}

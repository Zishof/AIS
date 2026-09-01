package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.Tagihan;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link Tagihan} (modul sekolah) —
 * lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat tagihan sekolah ditampilkan). Field
 * pencarian: {@code nama}, {@code keterangan}, {@code bulan}, {@code tahun} — mencerminkan
 * struktur periode tagihan bulanan/tahunan pada entity ini.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiTagihanHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi data Tagihan (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiTagihanHelper(EventListener eventListener) throws Exception {
        super(Tagihan.class, "Revisi Tagihan", eventListener, new String[] { "nama", "keterangan", "bulan", "tahun" });
    }
}

package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.VirtualAccountBank;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link VirtualAccountBank} —
 * lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat data Virtual Account Bank
 * ditampilkan). Field pencarian: {@code noVa} (nomor virtual account), {@code kode},
 * {@code nama}, {@code keterangan}.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiVirtualAccountBankHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi data Virtual Account Bank (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiVirtualAccountBankHelper(EventListener eventListener) throws Exception {
        super(VirtualAccountBank.class, "Revisi Virtual Account Bank", eventListener, new String[] { "noVa", "kode", "nama", "keterangan" });
    }
}

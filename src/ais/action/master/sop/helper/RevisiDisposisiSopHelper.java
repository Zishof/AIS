package ais.action.master.sop.helper;

import ais.action.master.helper.GenericRevisiHelper;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sop.DisposisiSop;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link DisposisiSop} (dokumen disposisi SOP) — lihat Javadoc class tersebut untuk penjelasan
 * lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat dokumen disposisi SOP ditampilkan).
 * Field pencarian: {@code keterangan}, {@code properti}, {@code nama}. Riwayat alur per langkah
 * disposisi didokumentasikan terpisah di {@link RevisiDisposisiAlurSopHelper}, yang dibatasi ke
 * satu dokumen disposisi.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDisposisiSopHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi dokumen Disposisi SOP (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
     */
    public RevisiDisposisiSopHelper(EventListener eventListener) throws Exception {
        super(DisposisiSop.class, "Revisi Disposisi SOP", eventListener, new String[] { "keterangan", "properti", "nama" });
    }
}

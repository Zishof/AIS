package ais.action.master.sop.helper;

import ais.action.master.helper.GenericRevisiHelper;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link DisposisiAlurSop} (langkah/alur pada satu dokumen disposisi SOP) — lihat Javadoc class
 * tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: riwayat MEMBATASI hanya pada alur milik satu {@link DisposisiSop} tertentu lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code disposisiSop}. Field
 * pencarian: {@code keterangan}, {@code properti}, {@code nama}. Bandingkan dengan
 * {@link RevisiDisposisiSopHelper} untuk riwayat dokumen disposisi SOP induknya (tanpa filter).</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDisposisiAlurSopHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi Alur Disposisi SOP, dibatasi pada satu dokumen disposisi SOP.
     *
     * @param disposisiSop  dokumen disposisi SOP yang membatasi riwayat alur yang ditampilkan.
     * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
     */
    public RevisiDisposisiAlurSopHelper(DisposisiSop disposisiSop, EventListener eventListener) throws Exception {
        super(DisposisiAlurSop.class, "Revisi Alur Disposisi SOP", eventListener, new String[] { "keterangan", "properti", "nama" }, new GenericRevisiHelper.FixedPropertyFilter("disposisiSop", disposisiSop));
    }
}

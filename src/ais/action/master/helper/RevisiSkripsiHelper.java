package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.Skripsi;

/**
 * Subclass tipis dari {@link GenericRevisiHelper} untuk entity {@link Skripsi} — lihat Javadoc
 * class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: helper ini punya DUA konstruktor yang membuka mode berbeda —
 * {@link #RevisiSkripsiHelper(EventListener)} untuk riwayat SELURUH data skripsi kampus (tanpa
 * filter, judul "Revisi Skripsi"), dan {@link #RevisiSkripsiHelper(Skripsi, EventListener)} untuk
 * riwayat SATU skripsi tertentu lewat {@link GenericRevisiHelper.EntityIdFilter} (judul "Riwayat
 * Nilai Skripsi" + NIM/nama mahasiswa bila tersedia). Field pencarian sama di kedua mode:
 * {@code judul}, {@code nama}, {@code keterangan}.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSkripsiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi seluruh data Skripsi kampus (tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiSkripsiHelper(EventListener eventListener) throws Exception {
        super(Skripsi.class, "Revisi Skripsi", eventListener, new String[] { "judul", "nama", "keterangan" });
    }

    /**
     * Membuka riwayat untuk satu data skripsi saja. Filter ID membuat operator
     * dapat menelusuri perubahan nilai mahasiswa yang sedang dilihat tanpa harus
     * mencari di seluruh revisi skripsi kampus.
     *
     * @param skripsi       data skripsi yang riwayatnya ingin dilihat; boleh {@code null} (filter
     *                      ID lalu memakai {@code null}, lihat {@link GenericRevisiHelper.EntityIdFilter}).
     *                      Judul window disisipi NIM dan nama mahasiswa bila {@code skripsi} dan
     *                      {@link Skripsi#getMahasiswa()}-nya tidak {@code null}.
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}.
     */
    public RevisiSkripsiHelper(Skripsi skripsi, EventListener eventListener) throws Exception {
        super(Skripsi.class,
                "Riwayat Nilai Skripsi"
                        + (skripsi == null || skripsi.getMahasiswa() == null ? ""
                                : " - " + skripsi.getMahasiswa().getNim() + " "
                                        + skripsi.getMahasiswa().getNama()),
                eventListener,
                new String[] { "judul", "nama", "keterangan" },
                new GenericRevisiHelper.EntityIdFilter(skripsi == null ? null : skripsi.getId()));
    }
}

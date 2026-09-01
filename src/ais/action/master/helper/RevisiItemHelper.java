package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.library.Item;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link Item} (koleksi/item perpustakaan) — lihat Javadoc class tersebut untuk penjelasan
 * lengkap arsitektur window, alur Envers, dan fitur restore. Hanya mengonfigurasi
 * {@link GenericRevisiHelper} generik dengan kolom-kolom yang dipantau perubahannya:
 * {@code isbn}, {@code isbn10}, {@code issn}, {@code kode}, {@code nama}, dan
 * {@code pengarangs}. Tidak ada {@link GenericRevisiHelper.QueryCustomizer} dan tidak ada
 * override hook {@code afterRestoreInTransaction}. Seluruh logika pengambilan/penampilan riwayat
 * revisi ada pada kelas induk; kelas ini murni deklaratif.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiItemHelper extends GenericRevisiHelper<Item> {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuat helper revisi untuk {@link Item} dengan judul "Revisi Item Perpustakaan".
     *
     * @param eventListener listener yang diteruskan ke {@link GenericRevisiHelper} untuk menangani
     *                       event pada komponen ZK terkait, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiItemHelper(EventListener eventListener) throws Exception {
        super(Item.class, "Revisi Item Perpustakaan", eventListener, new String[] { "isbn", "isbn10", "issn", "kode", "nama", "pengarangs" });
    }
}

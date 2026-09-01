package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.PembombotanNilai;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.PembombotanNilai} (catatan: nama class entity memang mengandung typo
 * historis "Pembombotan", bukan "Pembobotan" — dipertahankan apa adanya karena ini nama class
 * Hibernate yang sudah dipakai di skema database; JANGAN diubah tanpa migrasi terpisah) — berisi
 * aturan pembobotan komponen nilai. Lihat Javadoc {@link GenericRevisiHelper} untuk penjelasan
 * lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code nama}, {@code keterangan}. Tidak ada
 * {@link GenericRevisiHelper.QueryCustomizer} (seluruh riwayat revisi tampil tanpa penyaringan)
 * dan tidak ada override hook {@code afterRestoreInTransaction}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPembobotanNilaiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka jendela riwayat revisi {@link PembombotanNilai}.
     *
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiPembobotanNilaiHelper(EventListener eventListener) throws Exception {
        super(PembombotanNilai.class, "Revisi Pembobotan Nilai", eventListener, new String[] { "nama", "keterangan" });
    }
}

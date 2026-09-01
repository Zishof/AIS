package ais.action.master.sekolah.helper;

import ais.action.master.helper.GenericRevisiHelper;
import org.zkoss.zk.ui.event.EventListener;
import ais.database.model.sekolah.PembayaranSiswa;

/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link PembayaranSiswa} (dokumen master transaksi pembayaran siswa) — lihat Javadoc class
 * tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: tidak ada filter tambahan (seluruh riwayat transaksi pembayaran siswa
 * ditampilkan). Field pencarian: {@code nama}, {@code keterangan}, {@code noKwitansi},
 * {@code kode}. Riwayat baris detail per item pembayaran didokumentasikan terpisah di
 * {@link RevisiPembayaranSiswaDetailHelper}, yang bisa dibatasi ke satu siswa/calon siswa.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPembayaranSiswaHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi dokumen Pembayaran Siswa (seluruh data, tanpa filter).
     *
     * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
     */
    public RevisiPembayaranSiswaHelper(EventListener eventListener) throws Exception {
        super(PembayaranSiswa.class, "Revisi Pembayaran Siswa", eventListener, new String[] { "nama", "keterangan", "noKwitansi", "kode" });
    }
}

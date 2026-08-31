package ais.action.master.akunting.helper;

import org.zkoss.zk.ui.event.Event;

import ais.database.model.akunting.Akun;

/**
 * Tipe khusus untuk ambil data akun kredit banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AmbilDataAkunBanbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code onSearchDefault}(). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see AmbilDataAkunBanbox
 */
public class AmbilDataAkunKreditBanbox extends AmbilDataAkunBanbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6288697863973153458L;

	public void onSearchDefault(Event event) {
		debetCredit = Akun.CREDIT;
		super.onSearchDefault(event);
	}

}

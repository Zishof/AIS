package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk pembayaran calon siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * PembayaranSiswaAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doAfterCompose}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see PembayaranSiswaAction
 */
public class PembayaranCalonSiswaAction extends PembayaranSiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5794731159936759364L;

	public void doAfterCompose(Component comp) throws Exception {
		super.pembayaranCalonSiswa = true;
		super.doAfterCompose(comp);

	        FilterLanjutHelper.setup(comp);
}

}

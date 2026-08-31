package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk alumni siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * SiswaAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doAfterCompose}(); konfigurasi constructor:
 * {@code alumni}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see SiswaAction
 */
public class AlumniSiswaAction extends SiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8793276768316272112L;

	public AlumniSiswaAction() {
		super.alumni = true;
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub

		super.doAfterCompose(comp);
		Common.initLaguage();

	        FilterLanjutHelper.setup(comp);
}

}

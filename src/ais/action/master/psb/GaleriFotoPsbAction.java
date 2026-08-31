package ais.action.master.psb;

import ais.action.master.pmb.GaleriFotoAction;
import ais.database.model.GaleriFoto;

/**
 * Controller/action ZK untuk galeri foto psb. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GaleriFotoAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah konfigurasi constructor: {@code jenis}. Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GaleriFotoAction
 */
public class GaleriFotoPsbAction extends GaleriFotoAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -728549040082532177L;

	public GaleriFotoPsbAction() {
		jenis = GaleriFoto.PSB;
	}

}

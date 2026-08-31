package ais.action.master;

import org.zkoss.zk.ui.Component;

/**
 * Controller/action ZK untuk perkuliahan jadwal. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * PerkuliahanAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doAfterCompose}(); konfigurasi constructor:
 * {@code today}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see PerkuliahanAction
 */
public class PerkuliahanJadwalAction extends PerkuliahanAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2589792589519814884L;

	public PerkuliahanJadwalAction() {
		super();
		
		today = true;
	}

	
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp); 
		if (add != null) { add.setVisible(false); }
		if (addJadwalKurikulum != null) { addJadwalKurikulum.setVisible(false); }
		delete = false;
		edit = false;
	}
}

package ais.action.master;

import ais.database.model.Perkuliahan;

/**
 * Controller/action ZK untuk monitor krs mahasiswa ekstrakurikuler. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MonitorKRSMahasiswaAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah konfigurasi constructor: {@code ekstrakurikuler}. Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MonitorKRSMahasiswaAction
 */
public class MonitorKRSMahasiswaEkstrakurikulerAction extends
		MonitorKRSMahasiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -248128929411608561L;

	public MonitorKRSMahasiswaEkstrakurikulerAction() {
		ekstrakurikuler = Perkuliahan.EKSTRA;
	}
}

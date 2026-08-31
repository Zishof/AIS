package ais.action.master.payroll;

/**
 * Controller/action ZK untuk persetujuan izin. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * CutiDanIzinAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code istilah}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see CutiDanIzinAction
 */
public class PersetujuanIzinAction extends CutiDanIzinAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	public PersetujuanIzinAction() {
		super(true, "Izin");
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Persetujuan Izin Pegawai";
	}
}

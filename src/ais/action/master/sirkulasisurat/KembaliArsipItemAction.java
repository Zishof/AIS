package ais.action.master.sirkulasisurat;

/**
 * Controller/action ZK untuk kembali arsip item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * KembaliSuratItemAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code tipe}; operasi lokal: {@code istilah}().
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see KembaliSuratItemAction
 */
public class KembaliArsipItemAction extends KembaliSuratItemAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1557820179448751233L;

	private static String tipe = "arsip";

	public KembaliArsipItemAction() {
		super(tipe);
	}

	@Override
	public String istilah() throws Exception {
		return "Pengembalian Arsip";
	}
}

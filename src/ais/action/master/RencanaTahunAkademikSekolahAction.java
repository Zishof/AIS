package ais.action.master;

/**
 * Controller/action ZK untuk rencana tahun akademik sekolah. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * RencanaTahunAkademikAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see RencanaTahunAkademikAction
 */
public class RencanaTahunAkademikSekolahAction extends RencanaTahunAkademikAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1260756740639261244L;

	public RencanaTahunAkademikSekolahAction() {
		super(true);
	}

}

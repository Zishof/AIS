package ais.action.master.akunting.helper;

/**
 * Tipe khusus untuk ambil data pegawai not default banbox. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AmbilDataPegawaiBanbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see AmbilDataPegawaiBanbox
 */
public class AmbilDataPegawaiNotDefaultBanbox extends AmbilDataPegawaiBanbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;

	public AmbilDataPegawaiNotDefaultBanbox() {
		super(false);
	}

}

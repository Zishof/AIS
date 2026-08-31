package ais.action.report.helper.pdf;

import ais.action.report.format1.employ.LaporanDaftarPegawaiNamaAlamat;

/**
 * Tipe khusus untuk laporan data pegawai nama alamat window. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * LaporanDaftarPegawaiNamaAlamat}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk
 * variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak
 * bercabang atau tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see LaporanDaftarPegawaiNamaAlamat
 */
public class LaporanDataPegawaiNamaAlamatWindow extends
		LaporanDaftarPegawaiNamaAlamat {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7420198988646285668L;

	public LaporanDataPegawaiNamaAlamatWindow() {
		super();

	}

	public LaporanDataPegawaiNamaAlamatWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);

	}

}
